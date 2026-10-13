# x509-ra API 계약

`v2x-pki/x509/x509-ra` 가 제공하는 API(뷰 제외)를 이 프로젝트로 옮긴 기록이다. x509-ra 의 v2 API 만 옮겼고, 경로에서 `/v2` 를 뺐다.
그 밖의 HTTP 메서드, 미디어 타입, 요청·응답 필드와 검증 규칙은 원본과 같다. 계약은 `ApiContractTest` 가, 구현한 엔드포인트의 동작은 `*ApiIT` 가
지킨다. 아직 구현하지 않은 엔드포인트는 올바른 요청에 `501 COMMON_NOT_IMPLEMENTED` 로 응답한다([구현 현황](#구현-현황)).
호출 순서 예시는 [API_SCENARIOS.md](API_SCENARIOS.md).

- 원본 위치: `x509-ra/src/main/java/io/autocrypt/vehicles/pki/x509ra/{cert,profile,stats}/presentation/{api,internal}`
- 옮긴 위치: `core/core-api/src/main/java/io/autocrypt/vehicles/pki/x509/api` (컨트롤러), `api.request`, `api.response`, enum 은 core-enum 의 `x509.enums`
- 뷰(`presentation/ui`, `/view/**`, `/login`)와 화면 전용 어드바이스는 옮기지 않았다.

## 원본과 다른 점

- **v1 API 는 옮기지 않았다.** 원본의 v1 컨트롤러(`*ControllerV1`)와 v1 전용 DTO(`*V1`)는 없다. v1 경로(`/api/certificates`,
  `/api/certificates/p10cr`, `/api/certificates/sub`, `/api/certificates/{serialNumber}/revocation`, `/api/certificates/{skiHex}`,
  `/api/certpolicies/**`, `/api/ca/{skiHex}.crt`, `/api/crl/bulk.zip`)를 부르던 클라이언트는 아래 경로로 옮겨야 한다.
  예외로 공개 CRL 배포 경로 `GET /api/crl/{caSkiHex}.crl` 은 원본 그대로 남겼다([CRL](#crl)).
- **v2 경로에서 `/v2` 를 뺐다.** 원본 `/api/v2/certificates/root` 는 여기서 `/api/certificates/root` 다. 원본 v1 과 v2 가
  같은 경로를 쓰던 곳(`POST /api/certificates/root`, `GET /api/certificates/ca`, `GET /api/certificates/{akiHex}/download`,
  `/api/extended-key-usages`)은 이제 v2 계약으로 응답한다. 버전이 붙지 않았던 경로(`/cmp`, 키 합의·동기화, 통계, `/internal/**`)는 그대로다.

## 구현 현황

| 기능 | 엔드포인트 | 상태 |
|---|---|---|
| 인증서 프로파일 | `POST /api/profiles`, `GET /api/profiles/detail`, `GET /api/profiles` | 구현 |
| EKU | `POST·GET /api/extended-key-usages` | 구현. 기동할 때 표준 EKU 6개 등록 |
| CA 체인 | `POST /api/certificates/root`, `POST /api/certificates/sub-ca` | 구현 |
| CA 조회·다운로드 | `GET /api/certificates/ca`, `GET /api/certificates/ca/{skiHex}`, `GET /api/certificates/{akiHex}/download` | 구현 |
| Leaf 발급 | `POST /api/certificates/leaf/spki`, `POST /api/certificates/leaf/p10cr` | 구현 |
| Sub CA 갱신 | `POST /api/certificates/sub-ca/renewal` | 501. x509-ra 에도 구현이 없다("Sub CA renewal API is not implemented yet.") |
| 그 밖의 엔드포인트 | Leaf 갱신·폐기, CRL, CMP, 키 합의·동기화, 배포용 TLS 번들, 통계 | 501 |

### 구현 구조

| 역할 | 코드 |
|---|---|
| 규칙 | 프로파일 `CertProfile`(종류별 규칙, 유효기간, 확장), 인증서 `Cert`(발급 자격, 상태), 이름·OID 중복 `CertProfileNamePolicy`·`EkuRegistrationPolicy`, 값 객체(`ValidityDuration`, `SubjectAltNameTemplate`, `CertificationRequest` ...) |
| 흐름 | `CertProfileUseCase`(EKU 확인 + 프로파일 저장), `CertIssuanceUseCase`(프로파일·발급자 확인 → 키 생성·서명 → 저장) |
| 키 보관과 서명 | `clients/client-crypto` 의 `CryptoClient`. HSM/KMS 자리라 외부 시스템으로 다루며 트랜잭션 밖에서 부른다. 개인키는 DB 에 두지 않고 키 id 만 저장한다. `client.crypto.key-dir` 이 있으면 암호화 PKCS#8 파일, 없으면(local) 메모리 |
| 저장 | `cert_profile`(+ 키 용도, EKU, 정책 OID, OCSP 컬렉션 테이블), `extended_key_usage`, `cert`(DER + 조회 컬럼, `(aki, serial_number)` unique) |

### 구현에서 원본과 다른 점

- **사용자 정의 확장(`profileCustomExtensions`).** 원본은 화면으로 custom OID 를 등록해 두고 그 OID 만 쓰게 한다. 등록 기능이 아직 없어
  비어 있지 않은 요청은 원본에서 레지스트리가 비었을 때와 같이 400 `PROFILE_INVALID_INPUT` "Custom OID not found or inactive." 다.
- **발급 가능한 프로파일 목록.** 원본은 LEAF 전부와 HSM 이 지원하는 키 알고리즘의 CA 프로파일만 돌려준다. 여기는 키 보관소가 모든 키 알고리즘을
  지원하므로 전부를 만든 순서(id)로 돌려준다(원본은 정렬이 없다).
- **RSA 서명 알고리즘 식별자.** 원본은 OID 만 넣는다. 여기는 RFC 4055 대로 NULL 파라미터를 붙인다. ECDSA, ML-DSA, Ed448 은 원본과 같다.
- **EKU OID 형식 오류.** 원본은 프로파일 생성에서 형식이 틀린 EKU OID 를 errorCode 없는 400 으로 거절한다. 여기는 등록된 EKU 에서 찾지 못한
  것으로 보아 `PROFILE_INVALID_INPUT` "Unknown EKU OID." 다.
- **하지 않는 일.** 발급 요청 로그(`cert_issue_request`, 통계용), 승인 요청 기록(`cert_approval_request`)과 승인·반려·만료 처리, CA
  발급 직후의 첫 CRL 발행, 웹훅 이벤트, 인증서 설명(`certDescription` 은 항상 빈 값), 숨김 관리, 키 레지스트리 테이블. 승인이 필요한 Leaf 는
  서명까지 하고 `PENDING` 으로 저장만 한다.
- **에러 코드.** 원본에서 값 객체가 던지던 `CERT_INVALID_INPUT`(DomainErrorCode)도 같은 코드로 옮겼다. 원본과 같이 `detail` 에 구체적인
  메시지가 들어간다.

## 공통 규약

| 항목 | 내용 |
|---|---|
| JSON 날짜 | `Instant` 는 초 단위로 자른 ISO-8601 문자열(`2026-01-01T00:00:00Z`), `LocalDate` 는 `2026-01-01` |
| 바이너리 필드 | `byte[]` 는 Base64 문자열 (`subjectPublicKeyInfoDer`, `x509CertificateDer` 등) |
| 빠진 primitive 필드 | `boolean`, `int` 필드가 요청에 없으면 `false`, `0` 으로 받는다 (x509-ra 의 Jackson 2 동작. Jackson 3 기본값을 끄고 맞췄다) |
| `validityPeriod` | `^P[1-9]\d*[YMD]$` (`P1Y`, `P6M`, `P30D`). 대부분의 요청은 빈 문자열도 허용(`^$|...`)하고, 프로파일 생성과 Sub CA 갱신은 빈 값을 허용하지 않는다 |
| 오류 응답 | RFC 9457 ProblemDetail. 아래 [오류 응답](#오류-응답) |

### 오류 응답

x509-ra 의 `GlobalExceptionHandler` 와 같은 모양이다.

```json
{
  "type": "about:blank",
  "title": "PKI-ERROR",
  "status": 400,
  "detail": "validityPeriod only allows PnY, PnM, or PnD format.",
  "instance": "/api/certificates/root",
  "errorCode": "COMMON_INVALID_INPUT",
  "timestamp": "2026-10-12T12:58:01.469217Z",
  "errors": [ { "field": "validityPeriod", "message": "validityPeriod only allows PnY, PnM, or PnD format." } ]
}
```

| 원본 동작 | 이 프로젝트 |
|---|---|
| `title` 은 항상 `PKI-ERROR` | 같음 |
| 검증 실패의 `detail` 은 첫 필드 오류 메시지, `errors[{field, message}]` | 같음 |
| `X509Exception` 은 `errorCode`(`<접두어>_<이름>`, 예: `CERT_INVALID_INPUT`) | 같은 규약. `COMMON_*`, `CERT_*`, `PROFILE_*` |
| 검증·파싱 실패에는 `errorCode` 가 없다 | `COMMON_INVALID_INPUT` 을 붙인다 (필드 추가라 하위 호환) |
| `ErrorCategory` → HTTP 상태: BUSINESS_RULE_VIOLATION 400, NOT_FOUND 404, UNAUTHORIZED 403, INTERNAL_ERROR 500 | 같은 상태로 정의했다 |

## 엔드포인트

권한 열은 x509-ra `RaApiPolicy` 에서 원본 v2 경로에 붙은 권한이다. `openapi` 기능이 켜지고 TLS 가 켜졌을 때 mTLS 클라이언트 인증서 주체의 권한으로 검사한다.
`-` 는 정책에 없다는 뜻으로, 그 조건에서는 `denyAll` 에 걸려 호출할 수 없다. [보안](#보안-아직-옮기지-않음) 참고.

### 인증서 발급

| 메서드 | 경로 | 요청 | 응답 (200) | 권한 |
|---|---|---|---|---|
| POST | `/api/certificates/leaf/spki` | `LeafIssuanceRequest` | `LeafIssuanceResponse` | ISSUE_LEAF |
| POST | `/api/certificates/leaf/p10cr` | `P10crIssuanceRequest` | `LeafP10CrIssuanceResponse` | ISSUE_LEAF |
| POST | `/api/certificates/root` | `RootIssuanceRequest` | PEM 문자열 (`application/x-pem-file`) | ISSUE_CA |
| POST | `/api/certificates/sub-ca` | `SubCaIssuanceRequest` | `SubCaIssuanceResponse` | ISSUE_CA |
| POST | `/api/certificates/sub-ca/renewal` | `SubCaRenewalRequest` | `SubCaRenewalResponse` | - |

leaf 발급은 프로파일의 승인 정책을 따른다(`ApprovalStrategy.FOLLOW_PROFILE`). Root·Sub CA 발급·갱신 후에는 발급 이벤트를 발행한다.

### 인증서 관리·조회

| 메서드 | 경로 | 요청 | 응답 (200) | 권한 |
|---|---|---|---|---|
| POST | `/api/certificates/leaf/{akiHex}/renewal` | `RenewalRequest` | `RenewalResponse` | RENEW |
| POST | `/api/certificates/leaf/{akiHex}/revocation` | `RevocationRequest` | 빈 본문 | REVOKE |
| GET | `/api/certificates/{akiHex}/download?serialNumberHex=&format=` | 쿼리 `DownloadRequest` | 인증서 파일 | READ |
| GET | `/api/certificates/ca/{skiHex}` | - | `CertResponse` | READ |
| GET | `/api/certificates/ca` | - | `CaCertDetailResponse[]` (체인별 최신 CA, 숨김 제외) | READ |

다운로드: `format` 이 없거나 `pem` 이면 `application/x-pem-file`, 파일명 `{serialNumberHex}.pem`. `der` 이면 `application/pkix-cert`,
`{serialNumberHex}.der`. 그 외는 400 `CERT_INVALID_INPUT` "Unsupported certificate download format.".

### CRL

| 메서드 | 경로 | 응답 (200) | 권한 |
|---|---|---|---|
| GET | `/api/crls/bulk.zip` | ZIP. 항목 `{AKI}_{crlNumber}.crl`, 파일명 `bulk-crl-yyyyMMdd-HHmmss.zip`(UTC). CRL 이 없으면 404 `CERT_RESOURCE_NOT_FOUND` | READ |
| GET | `/api/crls/{caSkiHex}.crl` | 최신 CRL DER (`application/pkix-crl`), 파일명 `{caSkiHex}.crl` | - |
| POST | `/api/crls/{caSkiHex}/issue` | Full CRL 을 즉시 발행해 DER 로 돌려준다 | - |
| GET | `/api/crl/{caSkiHex}.crl` | 최신 CRL DER (`application/pkix-crl`). 공개 CRL 배포 경로(CRL DP) | 항상 허용 |

`/api/crl/{caSkiHex}.crl` 은 원본 v1 경로를 그대로 쓴다(`CrlDistributionController`). 원본은 발급하는 인증서의 CRL Distribution
Points 확장에 `{CRL DP base URL}{pki-services.dp.crl-dp-path}/{issuerSki}.crl`(기본 `crl-dp-path: /api/crl`)을 넣고, 이 경로를 인증
없이 연다. 이미 발급된 인증서와 검증자가 이 URL 로 CRL 을 받으므로 경로를 바꾸지 않는다.

### CMP, 키 합의, 키 동기화, 내부 API

| 메서드 | 경로 | 요청 / 응답 | 접근 |
|---|---|---|---|
| POST | `/cmp` | `application/pkixcmp` DER PKIMessage ↔ DER PKIMessage | 항상 허용 |
| POST | `/api/key-agreement/request-public-key` | - / 공개키 바이트 | `key-agreement.role=server` 일 때만 등록. 허용 |
| POST | `/api/key-agreement/operate` | `KeyAgreementRequest` / 빈 본문 | 위와 같음 |
| POST | `/api/key-sync` | - / 204 | `pki.features.key-sync.enabled=true` 일 때만 등록. 허용 |
| POST | `/internal/deployment/tls-bundle` | `DeploymentTlsBundleRequest` / ZIP(`tls-leaf.pfx`, `subca.pem`, `rootca.pem`), `deployment-tls.zip`, `Cache-Control: no-store` | 별도 필터(`deploymentInternalAccessFilter`) |

- CMP 는 처리 실패도 HTTP 200 + error PKIBody(`PKIStatus.rejection`, `PKIFailureInfo`, `[<CmpErrorCode>] <메시지>`)로 돌려준다. 응답 헤더는
  요청 헤더에서 만든다(`CmpExceptionHandler`, `PkiHeaderExtractor`, 요청 본문 캐싱 필요). 이 동작은 구현 단계에서 옮긴다.
- 키 합의는 원본 주석 기준 200 성공, 208 이미 완료, 500 실패.

### 프로파일

| 메서드 | 경로 | 요청 | 응답 (200) | 권한 |
|---|---|---|---|---|
| POST | `/api/profiles` | `CertProfileCreationRequest` | `CertProfileResponse` | MANAGE_PROFILE |
| GET | `/api/profiles/detail?profileName=` | - | `CertProfileDetailResponse` | READ |
| GET | `/api/profiles?page=0&size=20` | - | `CertProfilePageResponse` (발급 가능한 프로파일만) | READ |
| POST | `/api/extended-key-usages` | `EkuCreateRequest` | 빈 본문 | MANAGE_PROFILE |
| GET | `/api/extended-key-usages` | - | `EkuResponse[]` | READ |

- 프로파일 생성 요청의 EKU 필드는 JSON 이름이 `extendKeyUsage` 다(자바 필드는 `extendedKeyUsages`). 오타처럼 보이지만 계약이다.
- `profileCustomExtensions` 는 빠지면 빈 목록이다.

### 통계

| 메서드 | 경로 | 요청 | 응답 (200) | 권한 |
|---|---|---|---|---|
| GET | `/api/stats/issue-cert-request?start=&end=&statType=` | 쿼리 | `CertIssueRequestStatResponse[]` | READ_STATS |
| GET | `/api/stats/issued-cert?start=&end=&statType=` | 쿼리 | `IssuedCertStatResponse[]` | READ_STATS |
| GET | `/api/stats/revoked-cert?start=&end=&statType=` | 쿼리 | `RevokedCertStatResponse[]` | READ_STATS |
| POST | `/api/cert-stat/recalculation` | `CertStatRefreshRequest` | 빈 본문 | RECALCULATE_STATS |

`start`, `end` 는 ISO 날짜(`2026-01-01`), `statType` 은 `HOURLY`, `DAILY`, `WEEKLY`, `MONTHLY`, `YEARLY`.

## 요청 필드

`*` 는 필수. 검증 메시지가 따로 없으면 Bean Validation 기본 메시지다.

| 요청 | 필드 |
|---|---|
| `LeafIssuanceRequest` | `subjectDn`*, `akiHex`*, `subjectPublicKeyInfoDer`*(Base64), `certProfileId`*, `crlDps`, `includeDefaultCrlDp`(bool), `aiaCaIssuers`, `ocspUris`, `validityPeriod`, `notBefore` |
| `P10crIssuanceRequest` | `p10crPem`*, `certProfileId`*, `akiHex`*, `crlDps`, `includeDefaultCrlDp`, `aiaCaIssuers`, `ocspUris`, `validityPeriod`, `notBefore` |
| `RootIssuanceRequest` | `certProfileId`*, `subjectDn`*, `validityPeriod`, `crlDps`, `ocspUris`, `notBefore`(비면 현재 시각) |
| `SubCaIssuanceRequest` | `certProfileId`*, `subjectDn`*, `akiHex`*, `crlDps`, `includeDefaultCrlDp`, `aiaCaIssuers`, `ocspUris`, `validityPeriod`, `notBefore` |
| `SubCaRenewalRequest` | `serialNumberHex`*, `akiHex`*, `activationStartDaysAgo`(≥0), `validityPeriod`(빈 값 불가), `useExistingPublicKey`*, `revokeOriginalCertificate`*, `useLatestIssuer`* |
| `RenewalRequest` | `serialNumberHex`*, `activationStartDaysAgo`(≥0), `revokeOriginalCertificate`(bool), `subjectPublicKeyInfoDer`(Base64), `validityPeriod` |
| `RevocationRequest` | `serialNumberHex`*, `reason`*(RFC 5280 CRLReason 정수) |
| `DownloadRequest` (쿼리) | `serialNumberHex`*, `format`(`pem`/`der`) |
| `KeyAgreementRequest` | `wrappingAlgorithm`*(`ECDH_AES_128_GCM`/`192`/`256`), `initialValue`*, `ephemeralPublicKeySpki`*, `wrappedKey`* (모두 Base64) |
| `DeploymentTlsBundleRequest` | `subjectAlternativeNames`*(최대 2000자), `pfxPassword`* |
| `CertProfileCreationRequest` | `policyName`*, `certType`*(`ROOT`/`SUB`/`LEAF`), `validityPeriod`*, `pathLenConstraint`, `subjectAltName`, `keyUsages`*(KeyUsage 집합), `extendKeyUsage`(OID 목록), `policyOids`, `useDefaultOcspUri`, `ocspUris`, `keyUsageCritical`, `requireApproval`, `useDefaultAiaCaIssuer`, `keyAlgorithmSpec`*, `profileCustomExtensions[{oid*, critical, oidValue}]` |
| `EkuCreateRequest` | `oid`*, `purpose`* |
| `CertStatRefreshRequest` | `start`*, `end`* |

## 응답 필드

| 응답 | 필드 |
|---|---|
| `CertResponse` | `id`, `serialNumberHex`, `issuerDn`, `subjectDn`, `keyAlgorithm`, `notBefore`, `notAfter`, `signatureAlgorithmName`, `signatureAlgorithmOid`, `certType`, `certStatus`, `akiHex`, `skiHex`, `keyUsages`, `extendedKeyUsages`, `customOidExtensions[{oid, critical, oidValue, valueType}]` |
| `CaCertDetailResponse` | `CertResponse` + `trackingId`, `certPem`, `certDescription{values[{key, value}]}` |
| `LeafIssuanceResponse`, `SubCaIssuanceResponse`, `SubCaRenewalResponse` | `x509CertificateDer`(Base64) |
| `LeafP10CrIssuanceResponse` | `x509CertificateDer`, `issuerDn`, `subjectDn`, `notBefore`, `notAfter`, `serialNumberHex`, `akiHex`, `skiHex`, `signatureAlgorithmName`, `signatureAlgorithmOid`, `keyUsages`, `extendedKeyUsages` |
| `RenewalResponse` | `serialNumberHex`, `subjectDn`, `certificateDerBase64` |
| `CertProfileResponse` | `id`, `name` |
| `CertProfileDetailResponse` | `id`, `name`, `certType`, `validityPeriod`, `isCa`, `pathLenConstraint`, `keyUsageCritical`, `requireApproval`, `keyUsages`, `extendedKeyUsages[{oid, purpose}]`, `certificateOidsCsv`, `certificatePoliciesCritical`, `sanCsv`, `keyAlgorithm`, `useDefaultAiaCaIssuer`, `ocspUris`, `profileCustomExtensions[{oid, critical, oidValue, valueType}]`, `createdAt`, `updatedAt` |
| `CertProfilePageResponse` | `certProfileDetailResponses`, `page`, `size`, `totalElements`, `totalPages`, `blockStart`, `blockEnd` |
| `EkuResponse` | `id`, `oid`, `purpose` |
| 통계 응답 | `groupBy` + `certIssueRequestCount` / `issuedCertCount` / `revokedCertCount` |

enum 값: `CertStatus` `VALID`, `REVOKED`, `PENDING`, `EXPIRED`. `KeyUsage` `DIGITAL_SIGNATURE` … `DECIPHER_ONLY`(9개).
`CustomOidValueType` `UTF8_STRING`, `BIT_STRING`, `PRINTABLE_STRING`, `IA5_STRING`, `INTEGER`, `BOOLEAN`, `OBJECT_IDENTIFIER`, `OCTET_STRING_HEX`, `NULL`, `UNKNOWN`.

## 보안 (아직 옮기지 않음)

x509-ra 는 SecurityFilterChain 을 경로별로 나눈다. 계약만 옮긴 지금은 Spring Security 가 없어 모든 경로가 열려 있다.

| 체인 | 대상 | 동작 |
|---|---|---|
| 0 | `/internal/deployment/**` | CSRF 끔, `deploymentInternalAccessFilter` 로 접근 제한 후 허용 |
| 1 | `/actuator/**` | health, prometheus 만 허용 |
| 2 | `/api/**`, `/cmp` | 무상태. `/cmp` 와 배포 경로 `/api/ca/*.crt`, `/api/crl/*.crl` 은 항상 허용. `pki.features.openapi=false` 면 키 합의·동기화만 허용하고 나머지 거절. openapi + TLS 면 mTLS 클라이언트 인증서(`X509PrincipalExtractor`)의 권한으로 `RaApiPolicy` 검사, 정책에 없는 경로는 거절. openapi + 비TLS 면 모두 허용 |

## 구현 단계에서 옮길 것

- 남은 엔드포인트(Leaf 갱신·폐기, CRL, CMP, 키 합의·동기화, TLS 번들, 통계)의 동작과 에러 코드(`STATS_*` …)
- CRL·ZIP 응답 헤더(`Content-Disposition`, 파일명 규칙)
- 위 [하지 않는 일](#구현에서-원본과-다른-점)과 custom OID 레지스트리
- CMP 의 PKIMessage 오류 응답과 요청 본문 캐싱
- 보안 체인과 `RaApiPolicy`(원본 v2 경로 기준이라 `/v2` 를 뺀 경로로 다시 매핑한다. `/cmp` 와 `/api/crl/*.crl` 은 인증 없이
  연다. 원본에서 함께 열려 있던 CA 인증서 배포 경로 `/api/ca/*.crt` 를 어떻게 할지도 정해야 한다), 기능 플래그(`pki.features.openapi`, `pki.features.key-sync.enabled`, `key-agreement.role`)
