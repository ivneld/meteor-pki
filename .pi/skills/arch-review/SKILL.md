---
name: arch-review
description: 이 저장소의 아키텍처 의미 규칙(S-01~S-08)으로 현재 변경을 리뷰한다. "아키텍처 리뷰", "규칙 검사", "코드리뷰 요청 전 확인" 요청에 사용한다.
---

# 아키텍처 리뷰

이 저장소에는 `/arch-review` 명령이 있다(`.pi/extensions/arch-review.ts`). 사용자가 리뷰를 요청하면 직접 흉내 내지 말고
그 명령을 쓰라고 안내한다. 옵션은 `--base <ref>`, `--diff <patch>`, `--no-archunit`.

명령을 쓸 수 없는 상황에서만 같은 절차를 수동으로 따른다.

1. `review/prompts/reviewer.md` 의 역할과 출력 형식을 그대로 따른다.
2. `docs/ARCHITECTURE.md` 의 S 규칙과 `docs/REVIEW_AGENT.md` 의 판단 가이드를 읽는다.
3. 변경은 `git diff $(git merge-base origin/main HEAD)` 로 얻는다.
4. 관련 애그리거트, *Policy, *Reader / *Writer, *UseCase 파일을 읽어 판단한다. 파일을 수정하지 않는다.
