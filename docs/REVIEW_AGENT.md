# 리뷰 에이전트 하네스

코드리뷰를 요청하기 전에 팀원이 직접 돌리는 아키텍처 리뷰. [Pi](https://github.com/badlogic/pi-mono) 를 읽기 전용 에이전트로 실행해
[ARCHITECTURE.md](ARCHITECTURE.md) 의 의미 규칙(S-xx)을 검사한다. 구조 규칙(R-xx)은 ArchUnit 이 맡는다.
판단이 애매하면 ARCHITECTURE.md 의 [설계 원칙](ARCHITECTURE.md#설계-원칙)으로 돌아간다.

## 설치와 설정

팀원 기기에 Pi 가 설치되어 있으면 저장소 안에서 `pi` 명령만으로 리뷰한다. 별도 스크립트는 없다.

1. **Pi 설치** (1.x)

   ```bash
   npm i -g @earendil-works/pi-coding-agent
   pi --version   # 1.0.x
   ```

2. **모델과 토큰을 `.env` 에 적는다.** 저장소 루트의 `.env.example` 을 복사한다. `.env` 는 gitignore 되어 있다.

   ```bash
   cp .env.example .env
   ```

   ```dotenv
   ARCH_REVIEW_MODEL=anthropic/claude-sonnet-4-5   # 리뷰 턴에만 쓸 모델. provider/model 형식. 비우면 현재 모델
   ANTHROPIC_API_KEY=sk-ant-...                     # 쓰는 제공자의 토큰. pi 가 읽는 환경변수 이름 그대로
   ```

   다른 제공자를 쓰면 그 제공자의 변수 이름으로 바꾼다 (`OPENAI_API_KEY`, `GEMINI_API_KEY`, `OPENROUTER_API_KEY` 등. `pi --help` 의
   Environment Variables 절). `/arch-review` 확장이 시작할 때 `.env` 를 읽어 **아직 없는 환경변수만** 채우므로, 셸에 이미 설정된
   값이나 `pi` 의 `/login` 으로 저장한 인증이 있으면 그쪽이 우선한다. 모델은 리뷰 턴에만 바뀌고 끝나면 원래 모델로 돌아온다.

3. **프로젝트를 한 번 신뢰한다.** `/arch-review` 는 저장소의 `.pi/extensions` 에 있어서 Pi 의 프로젝트 신뢰(trust)가 필요하다.

   ```bash
   pi          # 저장소 루트에서 실행하면 신뢰 여부를 묻는다. 허용하면 ~/.pi/agent/trust.json 에 저장된다
   /trust      # 이미 열려 있다면 이 명령으로 저장
   ```

   비대화형(`-p`)은 물어볼 수 없으므로 저장된 결정이 없으면 확장을 **조용히 건너뛴다**. 훅이나 스크립트에서는 항상 `--approve` 를 붙인다.

## 리뷰 받기

```bash
# 대화형: pi 를 띄우고 프롬프트에 입력
/arch-review                       # origin/main 과의 차이(커밋 + 작업 트리)를 검토
/arch-review --base main           # 비교 기준 변경
/arch-review --no-archunit         # ArchUnit 생략
/arch-review --diff x.patch        # 주어진 패치를 검토 (골든 세트 평가용)

# 비대화형: 결과를 출력하고 종료 코드로 판정을 알린다
pi --approve -p --no-session "/arch-review" < /dev/null
```

| 종료 코드 (`-p`) | 의미 | 처리 |
|---|---|---|
| 0 | PASS. findings 없음 | 리뷰를 요청한다 |
| 1 | BLOCK. S-05, S-07 위반 | 고치거나 ADR 로 예외를 기록한 뒤 다시 돌린다 |
| 2 | WARN. 그 외 위반 또는 suspect | 판단해서 고치거나 리뷰 요청에 사유를 적는다 |

대화형에서는 판정이 알림(notify)으로 뜬다. 결과 본문은 `findings:` 목록과 `result:` 한 줄이다. 각 finding 에는 규칙 ID, 파일과 줄,
근거 코드, 규칙의 이유(`why`), 수정 방향(`fix`)이 있다.

비대화형으로 쓸 때 두 가지를 지킨다.

- `< /dev/null` 을 붙인다. Pi 는 stdin 이 터미널이 아니면 그 내용을 프롬프트 앞에 붙이려고 EOF 까지 기다린다. 훅과 CI 에서는 stdin 이
  열려 있거나(멈춤) 다른 내용(pre-push 의 ref 목록)이 들어온다.
- `--approve` 를 붙인다. 위 3번 참고.

푸시 때마다 자동으로 돌리려면 `git config core.hooksPath .githooks` 로 훅을 켠다. BLOCK 만 푸시를 막고, `SKIP_ARCH_REVIEW=1 git push`
로 건너뛸 수 있다. pi 가 없는 기기에서는 훅이 리뷰를 건너뛴다.

## 하네스 구성

`/arch-review` 는 저장소에 포함된 Pi 확장이다. Pi 는 신뢰한 프로젝트의 `.pi/` 를 자동으로 읽으므로 설치·`.env`·신뢰 외에 설정할 것은 없다.

```
.env.example                    모델(ARCH_REVIEW_MODEL)과 토큰 변수의 본보기. 복사해 .env 로
.pi/
├── extensions/arch-review.ts   /arch-review 명령. .env 로딩 → 입력 수집 → 프롬프트 치환 → 그 턴만 읽기 전용 도구 + 리뷰어 프롬프트 + 리뷰 모델 → 판정
└── skills/arch-review/SKILL.md 대화 중 "아키텍처 리뷰해줘" 류 요청을 /arch-review 로 안내
review/
├── prompts/reviewer.md         시스템 프롬프트. 역할, 검사 범위(S-xx 만), 작업 방식, 출력 형식
├── prompts/task.md             요청 프롬프트. 입력 절의 읽는 순서
├── eval.sh                     골든 세트로 재현율·정밀도 측정 (pi -p "/arch-review --diff ..." 를 반복 호출)
└── golden/cases/<이름>/        diff.patch + expected.txt
.githooks/pre-push              선택적 훅
```

확장이 하는 일과 그 이유.

| 단계 | 동작 | 이유 |
|---|---|---|
| `input` | `/arch-review …` 를 가로채 git diff(merge-base 기준, 작업 트리 포함), 변경 파일 목록, ArchUnit 결과, `docs/ARCHITECTURE.md`, `docs/REVIEW_AGENT.md` 를 한 프롬프트로 합쳐 치환한다 | 입력이 매번 같아야 결과가 재현된다. 에이전트에게 "알아서 모으라"고 하지 않는다 |
| `before_agent_start` | 그 턴에만 도구를 `read, grep, find, ls` 로 제한하고, `review/prompts/reviewer.md` 를 시스템 프롬프트에 붙이고, `ARCH_REVIEW_MODEL` 이 있으면 그 모델로 바꾼다 | 리뷰어는 코드를 고치지 않는다. 다른 턴의 설정은 건드리지 않는다 |
| `agent_end` | findings 를 파싱해 PASS/WARN/BLOCK 을 내고 도구와 모델을 되돌린다. 비대화형이면 종료 코드로 전달한다 | 훅과 CI 가 판정을 쓸 수 있어야 한다 |

에이전트는 diff 만으로 판단이 안 되면 read/grep 으로 관련 애그리거트, 도메인 서비스, usecase 파일을 직접 읽는다. 그래서 저장소 안에서 실행해야 한다.
`ARCH_REVIEW_DEBUG=1` 을 주면 실제로 보낸 프롬프트가 `build/arch-review-prompt.md` 에 남는다.

## 역할 분담

| 수단 | 맡는 규칙 | 성격 |
|---|---|---|
| ArchUnit (`./gradlew test`) | R-01 ~ R-13 | 결정적. 전체 코드 그래프를 본다. 위반이면 머지 불가 |
| 리뷰 에이전트 | S-01 ~ S-08 | 의미 판단. PR diff 와 주변 코드를 본다. 규칙별로 차단/권고를 정한다 |
| 사람 | 설계 판단 | usecase 가 필요한지, 트랜잭션을 나누는 게 맞는지, 규칙 자체를 바꿔야 하는지 |

## 에이전트 입력

1. 검토 대상 diff 전체 (`git merge-base origin/main HEAD` 기준, 작업 트리 포함)
2. 변경 파일 목록
3. `docs/ARCHITECTURE.md` 와 이 문서의 판단 가이드
4. ArchUnit 실행 결과 (실패한 규칙 ID 목록). 실패가 있으면 그 맥락에서 의미 위반을 찾는다
5. 관련 파일(애그리거트, 도메인 서비스, usecase)은 에이전트가 read/grep 으로 직접 읽는다. 확장이 미리 모아 주지 않는다

## 프롬프트

시스템 프롬프트는 `review/prompts/reviewer.md`, 요청 프롬프트는 `review/prompts/task.md` 가 단일 출처다. 여기에 복사해 두지 않는다.
출력은 `findings:` 목록(rule, file, line, verdict, evidence, why, fix)과 마지막 줄 `result: PASS|WARN|BLOCK` 으로 고정되어 있고,
확장(`.pi/extensions/arch-review.ts`)과 `review/eval.sh` 가 그 형식을 파싱한다. 형식을 바꾸면 두 파서도 함께 바꾼다.

`why` 에는 규칙의 이유를 ARCHITECTURE.md 의 표현으로 쓰게 했다. 코멘트가 쌓이면 규칙이 팀의 언어가 된다.

## 규칙별 판단 가이드

에이전트가 각 규칙을 어떻게 확인하는지. 골든 세트의 기대 결과도 이 기준으로 만든다.

| 규칙 | 확인 방법 | 오탐 주의 |
|---|---|---|
| S-01 | `*UseCase`, `*Reader`, `*Writer` 본문에서 도메인 객체의 getter 결과(상태 enum 등)로 분기하는 `if`/`switch` | 입력 검증(null, 빈 값)은 위반 아님 |
| S-02 | domain 의 애그리거트에 public setter, 상태 변경 메서드 부재, 도메인 서비스가 `restore(...)` 로 새 상태를 조립 | record VO 는 대상 아님. 상태 전이·계산·정책이 없는 설정·코드 관리 기능의 단순 모델은 위반 아님 |
| S-03 | `*Controller` 에서 `of`/`from`/getter 외의 VO 메서드 호출 | Request → Command·도메인 서비스 인자 변환은 허용 |
| S-04 | `*Writer` 나 `*UseCase` 가 저장소 조회 결과(건수, 목록, 합계)를 직접 비교해 거절·분기 | 사실을 꺼내 `*Policy` 나 애그리거트에 넘기기만 하는 것은 정상 |
| S-05 | `*Writer` 가 애그리거트의 상태 변경 메서드를 호출한 뒤 `repository.save()` 없이 반환 | `*Reader` 는 대상 아님 |
| S-06 | `@Transactional` usecase 메서드 안의 client(외부 I/O) 호출. `TransactionTemplate` 을 쓰는 메서드에서 람다 밖에서 Writer 를 둘 이상 엮음. 트랜잭션을 나눴는데 앞 구간이 남긴 상태로 실패를 정리·대사할 수 없음 | 트랜잭션 밖에서 외부 호출 실패를 잡아 단일 Writer 로 실패 상태를 남기는 것은 정상 (`PaymentUseCase.pay`) |
| S-07 | `@Transactional` usecase 메서드나 `TransactionTemplate` 람다 안에서 도메인 서비스 호출을 try-catch 로 감싸고 예외를 삼킨 채 계속 진행 | 트랜잭션 밖의 catch, 잡아서 다른 예외로 다시 던지는 것은 정상 |
| S-08 | `*Repository`/client 클래스 안의 도메인 상태 분기, 계산 | null 처리와 타입 변환, 없는 행에 대한 NOT_FOUND 예외는 허용 |

## 차단 범위

- 차단(머지 불가): S-05, S-07. 저장 누락은 데이터가 조용히 사라지고, 트랜잭션 안에서 삼킨 예외는 커밋 시점에 엉뚱한 실패를 낸다. 둘 다 오탐이 거의 없다.
- 권고(코멘트만): 나머지. S-06 은 트랜잭션을 어디서 나눌지라는 설계 판단이 들어가므로 권고로 둔다. 골든 세트에서 정밀도가 90% 를 넘으면 차단으로 올린다.

오탐이 차단하기 시작하면 팀은 에이전트를 무시하는 법을 배운다. 차단 범위는 측정 결과로만 넓힌다.

## 골든 세트

프롬프트를 바꿀 때마다 재현율과 정밀도를 재기 위한 고정 입력. `review/golden/cases/<이름>/` 에 `diff.patch` 와 `expected.txt` 를 두고
`review/eval.sh` 로 돌린다. 모델과 토큰은 `.env` 에서 읽힌다. 만드는 법은 [review/golden/README.md](../review/golden/README.md).

현재 케이스는 없다. 템플릿의 케이스는 예시 도메인(주문·결제) 코드에 대한 diff 라 이 저장소로 가져오지 않았다. x509 도메인 코드가 생기면
S 규칙마다 위반 1개, 위반처럼 보이지만 아닌 경우 1개 이상을 만든다.
실제 PR 에서 에이전트가 놓치거나 오탐한 사례를 케이스로 추가한다. 측정은 규칙별 재현율·정밀도이며, 차단 규칙은 정밀도 우선, 권고 규칙은 재현율 우선.

## 운영

- 규칙 변경은 `ARCHITECTURE.md` PR 하나로 하고, 같은 PR 에서 ArchUnit 테스트와 이 문서를 고친다.
- 에이전트 코멘트에 반박이 있으면 사람이 판단하고, 결과를 골든 세트에 넣는다. 에이전트와 논쟁하지 않는다.
- 분기마다 ARCHITECTURE.md 의 측정 항목과 함께 골든 세트 점수를 본다.
