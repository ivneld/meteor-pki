아래 절을 순서대로 읽어라.

1. docs/ARCHITECTURE.md — 규칙의 단일 출처
2. docs/REVIEW_AGENT.md — 규칙별 확인 방법과 오탐 주의
3. archunit.txt — 구조 규칙(R-xx) 검사 결과. 실패한 규칙이 있으면 그 맥락에서 의미 위반을 찾되, R 규칙 자체를 다시 지적하지 않는다
4. changed-files.txt — 이번 변경이 건드린 파일 목록
5. diff.patch — 검토 대상 변경

변경 범위에 대해서만 S-01 ~ S-08 을 검사하고, 시스템 프롬프트에 정한 출력 형식으로만 답하라.
diff 만으로 판단이 안 되면 read / grep / find / ls 로 관련 애그리거트, 도메인 서비스, usecase 파일을 읽어라. 파일을 수정하지 않는다.
