/**
 * /arch-review — 아키텍처 의미 규칙(S-01~S-08) 리뷰.
 *
 *   pi                                       대화형. 프롬프트에 /arch-review [--base <ref>] [--diff <patch>] [--no-archunit]
 *   pi --approve -p "/arch-review" </dev/null 비대화형. 종료 코드 0 PASS, 1 BLOCK(S-05/S-07 위반), 2 WARN
 *
 * 설정은 저장소 루트의 .env 에서 읽는다 (.env.example 참고).
 *   ARCH_REVIEW_MODEL=anthropic/claude-sonnet-4-5   리뷰 턴에만 쓸 모델. 비우면 현재 모델
 *   ANTHROPIC_API_KEY=...                           모델 제공자 토큰. pi 가 읽는 환경변수 이름 그대로
 *
 * 동작
 *   1. 로드 시 .env 를 읽어 아직 없는 환경변수만 채운다.
 *   2. input 이벤트에서 "/arch-review ..." 를 가로채 diff, 변경 파일, ArchUnit 결과, 규칙 문서를 모아 리뷰 프롬프트로 치환한다.
 *   3. before_agent_start 에서 그 턴에만 읽기 전용 도구, 리뷰어 시스템 프롬프트(review/prompts/reviewer.md), 리뷰 모델을 적용한다.
 *   4. agent_end 에서 findings 를 파싱해 판정을 내고 도구·모델을 되돌린다. 비대화형이면 종료 코드로 알린다.
 *
 * 프롬프트의 단일 출처는 review/prompts/*.md 와 docs/ARCHITECTURE.md, docs/REVIEW_AGENT.md 다. 여기에 규칙을 적지 않는다.
 */
import type { ExtensionAPI } from "@earendil-works/pi-coding-agent";
import { existsSync, readFileSync, readdirSync, writeFileSync } from "node:fs";
import { dirname, join, resolve } from "node:path";

const COMMAND = "/arch-review";
const MARKER = "<!-- arch-review -->";
const READ_ONLY_TOOLS = ["read", "grep", "find", "ls"];
const BLOCKING_RULES = new Set(["S-05", "S-07"]);

interface Options {
	base: string;
	diff?: string;
	archunit: boolean;
}

export default function (pi: ExtensionAPI) {
	const root = findRepoRoot(process.cwd());
	loadDotEnv(join(root, ".env"));

	let active = false;
	let savedTools: string[] | null = null;
	let savedModel: any = null;
	let exitCode = 0;

	pi.on("input", async (event, ctx) => {
		const text = event.text.trim();
		if (text !== COMMAND && !text.startsWith(COMMAND + " ")) return { action: "continue" };

		const opts = parseOptions(text.slice(COMMAND.length));
		const diff = await collectDiff(pi, root, ctx.cwd, opts);
		if (!diff.trim()) {
			report(ctx, `검토할 변경이 없습니다. (base: ${opts.base})`, "info");
			return { action: "handled" };
		}
		const changed = [...diff.matchAll(/^\+\+\+ b\/(.+)$/gm)].map((m) => m[1]);
		const archunit = opts.archunit ? await runArchUnit(pi, root) : "ArchUnit: skipped";

		const prompt = [
			MARKER,
			read(root, "review/prompts/task.md"),
			section("docs/ARCHITECTURE.md", read(root, "docs/ARCHITECTURE.md")),
			section("docs/REVIEW_AGENT.md", read(root, "docs/REVIEW_AGENT.md")),
			section("archunit.txt", archunit),
			section("changed-files.txt", changed.join("\n") || "(none)"),
			section("diff.patch", "```diff\n" + diff + "\n```"),
		].join("\n\n");

		if (process.env.ARCH_REVIEW_DEBUG) writeFileSync(join(root, "build", "arch-review-prompt.md"), prompt);
		active = true;
		exitCode = 0;
		return { action: "transform", text: prompt };
	});

	pi.on("before_agent_start", async (event, ctx) => {
		if (!active || !event.prompt.startsWith(MARKER)) return;
		savedTools = pi.getActiveTools();
		pi.setActiveTools(READ_ONLY_TOOLS);
		savedModel = await switchModel(pi, ctx, process.env.ARCH_REVIEW_MODEL);
		if (process.env.ARCH_REVIEW_DEBUG) {
			const m = ctx.model ? `${ctx.model.provider}/${ctx.model.id}` : "none";
			writeFileSync(join(root, "build", "arch-review-debug.txt"), `tools=${pi.getActiveTools().join(",")} model=${m}\n`);
		}
		return { systemPrompt: event.systemPrompt + "\n\n" + read(root, "review/prompts/reviewer.md") };
	});

	pi.on("agent_end", async (event, ctx) => {
		if (!active) return;
		active = false;
		if (savedTools) {
			pi.setActiveTools(savedTools);
			savedTools = null;
		}
		if (savedModel) {
			await pi.setModel(savedModel);
			savedModel = null;
		}
		const last: any = [...event.messages].reverse().find((m) => m.role === "assistant");
		if (!last || last.stopReason === "error" || last.stopReason === "aborted") {
			report(ctx, "arch-review: 리뷰가 완료되지 않았습니다 (모델 호출 실패 또는 중단)", "warning");
			return;
		}
		const verdict = judge(textOf(last));
		exitCode = verdict === "BLOCK" ? 1 : verdict === "WARN" ? 2 : 0;
		report(ctx, `arch-review: ${verdict}`, verdict === "PASS" ? "info" : "warning");
	});

	// 비대화형(print) 모드에서 판정을 종료 코드로 전달한다. pi 는 성공 시 0 으로 종료하므로 exit 직전에 올려 쓴다.
	pi.on("session_shutdown", async (_event, ctx) => {
		if (ctx.hasUI || exitCode === 0) return;
		const code = exitCode;
		process.on("exit", () => {
			if (process.exitCode === undefined || process.exitCode === 0) process.exitCode = code;
		});
	});
}

function findRepoRoot(start: string): string {
	let dir = start;
	while (true) {
		if (existsSync(join(dir, ".git"))) return dir;
		const parent = dirname(dir);
		if (parent === dir) return start;
		dir = parent;
	}
}

/** KEY=VALUE 줄만 읽는다. 따옴표는 벗기고, 이미 있는 환경변수는 덮어쓰지 않는다. */
function loadDotEnv(path: string) {
	if (!existsSync(path)) return;
	for (const raw of readFileSync(path, "utf8").split("\n")) {
		const line = raw.trim();
		if (!line || line.startsWith("#")) continue;
		const eq = line.indexOf("=");
		if (eq < 1) continue;
		const key = line.slice(0, eq).trim().replace(/^export\s+/, "");
		let value = line.slice(eq + 1).trim();
		if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) {
			value = value.slice(1, -1);
		}
		if (process.env[key] === undefined) process.env[key] = value;
	}
}

async function switchModel(pi: ExtensionAPI, ctx: any, spec: string | undefined): Promise<any> {
	if (!spec) return null;
	const slash = spec.indexOf("/");
	if (slash < 1) {
		report(ctx, `ARCH_REVIEW_MODEL 은 provider/model 형식이어야 합니다: ${spec}`, "warning");
		return null;
	}
	const model = ctx.modelRegistry.find(spec.slice(0, slash), spec.slice(slash + 1));
	if (!model) {
		report(ctx, `ARCH_REVIEW_MODEL 을 찾을 수 없어 현재 모델로 진행합니다: ${spec}`, "warning");
		return null;
	}
	const current = ctx.model;
	if (current && current.provider === model.provider && current.id === model.id) return null;
	const ok = await pi.setModel(model);
	return ok ? current : null;
}

function parseOptions(rest: string): Options {
	const tokens = rest.trim().split(/\s+/).filter(Boolean);
	const opts: Options = { base: "origin/main", archunit: true };
	for (let i = 0; i < tokens.length; i++) {
		const t = tokens[i];
		if (t === "--base" && tokens[i + 1]) opts.base = tokens[++i];
		else if (t === "--diff" && tokens[i + 1]) opts.diff = tokens[++i];
		else if (t === "--no-archunit") opts.archunit = false;
	}
	return opts;
}

async function collectDiff(pi: ExtensionAPI, root: string, cwd: string, opts: Options): Promise<string> {
	if (opts.diff) return readFileSync(resolve(cwd, opts.diff), "utf8");
	const mergeBase = (await pi.exec("git", ["merge-base", opts.base, "HEAD"], { cwd: root })).stdout.trim();
	if (!mergeBase) throw new Error(`merge-base 를 찾을 수 없습니다: ${opts.base}`);
	const args = ["diff", mergeBase, "--", ".", ":(exclude)**/build/**", ":(exclude)gradle/wrapper/**"];
	return (await pi.exec("git", args, { cwd: root })).stdout;
}

async function runArchUnit(pi: ExtensionAPI, root: string): Promise<string> {
	await pi.exec("./gradlew", ["-q", ":core:core-api:test", "--tests", "*ArchitectureRules*"], {
		cwd: root,
		timeout: 300_000,
	});
	const dir = join(root, "core/core-api/build/test-results/test");
	if (!existsSync(dir)) return "ArchUnit: no results (gradle failed?)";
	const failed: string[] = [];
	for (const file of readdirSync(dir).filter((f) => f.includes("ArchitectureRules") && f.endsWith(".xml"))) {
		const xml = readFileSync(join(dir, file), "utf8");
		for (const m of xml.matchAll(/<testcase name="([^"]+)"[^>]*>([\s\S]*?)<\/testcase>/g)) {
			if (m[2].includes("<failure")) failed.push(m[1]);
		}
	}
	return failed.length ? "ArchUnit FAILED rules:\n" + failed.map((n) => ` - ${n}`).join("\n") : "ArchUnit: all R rules passed";
}

function judge(text: string): "PASS" | "WARN" | "BLOCK" {
	let block = false;
	let warn = false;
	let rule = "";
	for (const line of text.split("\n")) {
		const r = line.match(/^\s*-\s*rule:\s*(S-\d\d)/);
		if (r) {
			rule = r[1];
			continue;
		}
		const v = line.match(/^\s*verdict:\s*(violation|suspect)/);
		if (v && rule) {
			warn = true;
			if (v[1] === "violation" && BLOCKING_RULES.has(rule)) block = true;
			rule = "";
		}
	}
	return block ? "BLOCK" : warn ? "WARN" : "PASS";
}

function textOf(message: any): string {
	if (!message) return "";
	if (typeof message.content === "string") return message.content;
	return (message.content as any[]).filter((c) => c.type === "text").map((c) => c.text).join("\n");
}

function read(root: string, rel: string): string {
	return readFileSync(join(root, rel), "utf8");
}

function section(title: string, body: string): string {
	return `## ${title}\n\n${body}`;
}

function report(ctx: any, message: string, level: "info" | "warning") {
	if (ctx.hasUI) ctx.ui.notify(message, level);
	else console.error(message);
}
