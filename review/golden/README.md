# 골든 세트

리뷰 에이전트 프롬프트를 바꿀 때마다 재현율·정밀도를 재기 위한 고정 입력. `review/eval.sh` 가 돌린다.

```
cases/<이름>/
├── diff.patch      검토 대상 패치 (현재 main 기준, git apply 가능하지 않아도 됨. 에이전트는 읽기만 한다)
└── expected.txt    기대 findings. 한 줄에 규칙 ID 하나. 위반이 없는 케이스는 "# none" 한 줄
```

규칙

- 규칙마다 위반 케이스 1개 이상, 위반처럼 보이지만 아닌 케이스 1개 이상을 둔다.
- 실제 PR 에서 에이전트가 놓치거나 오탐한 사례는 케이스로 추가한다. 골든 세트는 자란다.
- 차단 규칙(S-05, S-07)은 정밀도 우선, 권고 규칙은 재현율 우선으로 본다.
- `diff.patch` 는 실제 변경을 흉내 내야 한다. 아래처럼 만들면 형식이 맞는다.

```bash
mkdir -p /tmp/g/a /tmp/g/b
cp --parents core/.../OrderWriter.java /tmp/g/a/      # 원본
cp --parents core/.../OrderWriter.java /tmp/g/b/ && vi /tmp/g/b/core/.../OrderWriter.java   # 수정본
(cd /tmp/g && git diff --no-index a b) > review/golden/cases/<이름>/diff.patch
```
