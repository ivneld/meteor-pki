# x509-ra API 계약

`v2x-pki/x509/x509-ra` 가 제공하는 API(뷰 제외)를 이 프로젝트로 옮긴 기록이다. 경로, HTTP 메서드, 미디어 타입, 요청·응답 필드와 검증
규칙은 원본과 같다. 구현은 아직 없으므로 올바른 요청은 모두 `501 COMMON_NOT_IMPLEMENTED` 로 응답한다. 계약은 `ApiContractTest` 가 지킨다.

- 원본 위치: `x509-ra/src/main/java/io/autocrypt/vehicles/pki/x509ra/{cert,profile,stats}/presentation/{api,internal}`
- 옮긴 위치: `core/core-api/src/main/java/io/autocrypt/vehicles/pki/x509/api` (컨트롤러), `api.request`, `api.response`, enum 은 core-enum 의 `x509.enums`
- 뷰(`presentation/ui`, `/view/**`, `/login`)와 화면 전용 어드바이스는 옮기지 않았다.

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
  "instance": "/api/v2/certificates/root",
  "errorCode": "COMMON_INVALID_INPUT",
  "timestamp": "2026-10-12T12:58:01.469217Z",
  "errors": [ { "field": "validityPeriod", "message": "validityPeriod only allows PnY, PnM, or PnD format." } ]
}
```

| 원본 동작 | 이 프로젝트 |
|---|---|
| `title` 은 항상 `PKI-ERROR` | 같음 |
| 검증 실패의 `detail` 은 첫 필드 오류 메시지, `errors[{field, message}]` | 같음 |
| `X509Exception` 은 `errorCode`(`<접두어>_<이름>`, 예: `CERT_INVALID_INPUT`) | 같은 규약. 지금은 `COMMON_*` 만 있다 |
| 검증·파싱 실패에는 `errorCode` 가 없다 | `COMMON_INVALID_INPUT` 을 붙인다 (필드 추가라 하위 호환) |
| `ErrorCategory` → HTTP 상태: BUSINESS_RULE_VIOLATION 400, NOT_FOUND 404, UNAUTHORIZED 403, INTERNAL_ERROR 500 | 도메인 에러 코드를 옮길 때 같은 상태로 정의한다 |

## 엔드포인트

권한 열은 x509-ra `RaApiPolicy` 의 권한이다. `openapi` 기능이 켜지고 TLS 가 켜졌을 때 mTLS 클라이언트 인증서 주체의 권한으로 검사한다.
`-` 는 정책에 없다는 뜻으로, 그 조건에서는 `denyAll` 에 걸려 호출할 수 없다. [보안](#보안-아직-옮기지-않음) 참고.

### 인증서 발급 (v2)

| 메서드 | 경로 | 요청 | 응답 (200) | 권한 |
|---|---|---|---|---|
| POST | `/api/v2/certificates/leaf/spki` | `LeafIssuanceRequest` | `LeafIssuanceResponse` | ISSUE_LEAF |
| POST | `/api/v2/certificates/leaf/p10cr` | `P10crIssuanceRequest` | `LeafP10CrIssuanceResponse` | ISSUE_LEAF |
| POST | `/api/v2/certificates/root` | `RootIssuanceRequest` | PEM 문자열 (`application/x-pem-file`) | ISSUE_CA |
| POST | `/api/v2/certificates/sub-ca` | `SubCaIssuanceRequest` | `SubCaIssuanceResponse` | ISSUE_CA |
| POST | `/api/v2/certificates/sub-ca/renewal` | `SubCaRenewalRequest` | `SubCaRenewalResponse` | - |

leaf 발급은 프로파일의 승인 정책을 따른다(`ApprovalStrategy.FOLLOW_PROFILE`). Root·Sub CA 발급·갱신 후에는 발급 이벤트를 발행한다.

### 인증서 관리·조회 (v2)

| 메서드 | 경로 | 요청 | 응답 (200) | 권한 |
|---|---|---|---|---|
| POST | `/api/v2/certificates/leaf/{akiHex}/renewal` | `RenewalRequest` | `RenewalResponse` | RENEW |
| POST | `/api/v2/certificates/leaf/{akiHex}/revocation` | `RevocationRequest` | 빈 본문 | REVOKE |
| GET | `/api/v2/certificates/{akiHex}/download?serialNumberHex=&format=` | 쿼리 `DownloadRequest` | 인증서 파일 | READ |
| GET | `/api/v2/certificates/ca/{skiHex}` | - | `CertResponse` | READ |
| GET | `/api/v2/certificates/ca` | - | `CaCertDetailResponse[]` (체인별 최신 CA, 숨김 제외) | READ |

다운로드: `format` 이 없거나 `pem` 이면 `application/x-pem-file`, 파일명 `{serialNumberHex}.pem`. `der` 이면 `application/pkix-cert`,
`{serialNumberHex}.der`. 그 외는 400 `CERT_INVALID_INPUT` "Unsupported certificate download format.".

### CRL (v2)

| 메서드 | 경로 | 응답 (200) | 권한 |
|---|---|---|---|
| GET | `/api/v2/crls/bulk.zip` | ZIP. 항목 `{AKI}_{crlNumber}.crl`, 파일명 `bulk-crl-yyyyMMdd-HHmmss.zip`(UTC). CRL 이 없으면 404 `CERT_RESOURCE_NOT_FOUND` | READ |
| GET | `/api/v2/crls/{caSkiHex}.crl` | 최신 CRL DER (`application/pkix-crl`), 파일명 `{caSkiHex}.crl` | - |
| POST | `/api/v2/crls/{caSkiHex}/issue` | Full CRL 을 즉시 발행해 DER 로 돌려준다 | - |

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

### 인증서 (v1)

| 메서드 | 경로 | 요청 | 응답 (200) | 권한 |
|---|---|---|---|---|
| POST | `/api/certificates` | `LeafIssuanceRequestV1` | `LeafIssuanceResponseV1` | ISSUE_LEAF |
| POST | `/api/certificates/p10cr` | `P10crIssuanceRequestV1` (`issuerKeyIdHex`) | `LeafP10CrIssuanceResponseV1` | ISSUE_LEAF |
| POST | `/api/certificates/root` | `RootIssuanceRequestV1` | PEM 문자열 | ISSUE_CA |
| POST | `/api/certificates/sub` | `SubCaIssuanceRequestV1` | `SubCaIssuanceResponseV1` | ISSUE_CA |
| POST | `/api/certificates/{akiHex}/renewal` | `RenewalRequestV1` | `RenewalResponseV1` | RENEW |
| POST | `/api/certificates/{serialNumber}/revocation` | `RevocationRequestV1` (`akiHex`) | `RevocationResponseV1` | REVOKE |
| GET | `/api/certificates/{akiHex}/download?serialNumberHex=&format=` | 쿼리 `DownloadRequestV1` | 인증서 파일 | READ |
| GET | `/api/certificates/{skiHex}` | - | `CertResponseV1` | READ |
| GET | `/api/certificates/ca` | - | `CaCertDetailResponseV1[]` | READ |
| GET | `/api/ca/{skiHex}.crt` | - | CA 인증서 DER (`application/pkix-cert`) | 항상 허용 |
| GET | `/api/crl/{caSkiHex}.crl` | - | 최신 CRL DER | 항상 허용 |
| GET | `/api/crl/bulk.zip` | - | ZIP | READ |

- v1 과 v2 는 경로 변수의 의미가 다르다. v1 폐기는 경로가 serialNumber, 본문이 akiHex 이고 v2 는 그 반대다.
- v1 폐기는 실패도 ProblemDetail 이 아니라 `RevocationResponseV1{revoked: false, failedReason}` 로 알린다. 업무 오류 400, 그 외 500.
- `GET /api/certificates/{skiHex}` 와 `GET /api/certificates/ca` 는 경로가 겹친다. Spring 은 리터럴 경로(`ca`)를 우선한다.

### 프로파일

| 메서드 | 경로 | 요청 | 응답 (200) | 권한 |
|---|---|---|---|---|
| POST | `/api/v2/profiles` | `CertProfileCreationRequest` | `CertProfileResponse` | MANAGE_PROFILE |
| GET | `/api/v2/profiles/detail?profileName=` | - | `CertProfileDetailResponse` | READ |
| GET | `/api/v2/profiles?page=0&size=20` | - | `CertProfilePageResponse` (발급 가능한 프로파일만) | READ |
| POST | `/api/v2/extended-key-usages` | `EkuCreateRequest` | 빈 본문 | MANAGE_PROFILE |
| GET | `/api/v2/extended-key-usages` | - | `EkuResponse[]` | READ |
| POST | `/api/certpolicies` | `CertProfileCreationRequestV1` | `CertProfileResponseV1` | MANAGE_PROFILE |
| GET | `/api/certpolicies/detail?policyName=` | - | `CertProfileDetailResponseV1` | READ |
| GET | `/api/certpolicies?page=0&size=20` | - | `CertProfilePageResponseV1` (모든 프로파일) | READ |
| POST | `/api/extended-key-usages` | `EkuCreateRequestV1` | 빈 본문 | MANAGE_PROFILE |
| GET | `/api/extended-key-usages` | - | `EkuResponseV1[]` | READ |

- 프로파일 생성 요청의 EKU 필드는 JSON 이름이 `extendKeyUsage` 다(자바 필드는 `extendedKeyUsages`). 오타처럼 보이지만 계약이다.
- v2 만 `profileCustomExtensions` 를 받고 돌려준다. 빠지면 빈 목록이다.
- v1 상세 조회의 쿼리 이름은 `policyName`, v2 는 `profileName` 이다.

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
| `LeafIssuanceRequestV1` | 위에서 `includeDefaultCrlDp` 제외 |
| `P10crIssuanceRequest` | `p10crPem`*, `certProfileId`*, `akiHex`*, `crlDps`, `includeDefaultCrlDp`, `aiaCaIssuers`, `ocspUris`, `validityPeriod`, `notBefore` |
| `P10crIssuanceRequestV1` | `p10crPem`*, `certProfileId`*, `issuerKeyIdHex`*, `validityPeriod`, `notBefore`, `crlDps`, `aiaCaIssuers`, `ocspUris` |
| `RootIssuanceRequest` | `certProfileId`*, `subjectDn`*, `validityPeriod`, `crlDps`, `ocspUris`, `notBefore`(비면 현재 시각) |
| `RootIssuanceRequestV1` | `certProfileId`*, `subjectDn`*, `validityPeriod`, `notBefore` |
| `SubCaIssuanceRequest` | `certProfileId`*, `subjectDn`*, `akiHex`*, `crlDps`, `includeDefaultCrlDp`, `aiaCaIssuers`, `ocspUris`, `validityPeriod`, `notBefore` |
| `SubCaIssuanceRequestV1` | 위에서 `includeDefaultCrlDp` 제외 |
| `SubCaRenewalRequest` | `serialNumberHex`*, `akiHex`*, `activationStartDaysAgo`(≥0), `validityPeriod`(빈 값 불가), `useExistingPublicKey`*, `revokeOriginalCertificate`*, `useLatestIssuer`* |
| `RenewalRequest`, `RenewalRequestV1` | `serialNumberHex`*, `activationStartDaysAgo`(≥0), `revokeOriginalCertificate`(bool), `subjectPublicKeyInfoDer`(Base64), `validityPeriod` |
| `RevocationRequest` | `serialNumberHex`*, `reason`*(RFC 5280 CRLReason 정수) |
| `RevocationRequestV1` | `akiHex`*, `reason`* |
| `DownloadRequest`, `DownloadRequestV1` (쿼리) | `serialNumberHex`*, `format`(`pem`/`der`) |
| `KeyAgreementRequest` | `wrappingAlgorithm`*(`ECDH_AES_128_GCM`/`192`/`256`), `initialValue`*, `ephemeralPublicKeySpki`*, `wrappedKey`* (모두 Base64) |
| `DeploymentTlsBundleRequest` | `subjectAlternativeNames`*(최대 2000자), `pfxPassword`* |
| `CertProfileCreationRequest(V1)` | `policyName`*, `certType`*(`ROOT`/`SUB`/`LEAF`), `validityPeriod`*, `pathLenConstraint`, `subjectAltName`, `keyUsages`*(KeyUsage 집합), `extendKeyUsage`(OID 목록), `policyOids`, `useDefaultOcspUri`, `ocspUris`, `keyUsageCritical`, `requireApproval`, `useDefaultAiaCaIssuer`, `keyAlgorithmSpec`*, (v2) `profileCustomExtensions[{oid*, critical, oidValue}]` |
| `EkuCreateRequest(V1)` | `oid`*, `purpose`* |
| `CertStatRefreshRequest` | `start`*, `end`* |

## 응답 필드

| 응답 | 필드 |
|---|---|
| `CertResponse` | `id`, `serialNumberHex`, `issuerDn`, `subjectDn`, `keyAlgorithm`, `notBefore`, `notAfter`, `signatureAlgorithmName`, `signatureAlgorithmOid`, `certType`, `certStatus`, `akiHex`, `skiHex`, `keyUsages`, `extendedKeyUsages`, `customOidExtensions[{oid, critical, oidValue, valueType}]` |
| `CertResponseV1` | 위에서 `keyAlgorithm`, `customOidExtensions` 제외 |
| `CaCertDetailResponse` | `CertResponse` + `trackingId`, `certPem`, `certDescription{values[{key, value}]}` |
| `CaCertDetailResponseV1` | `CertResponseV1` + `certPem`, `certDescription` |
| `LeafIssuanceResponse(V1)`, `SubCaIssuanceResponse(V1)`, `SubCaRenewalResponse` | `x509CertificateDer`(Base64) |
| `LeafP10CrIssuanceResponse(V1)` | `x509CertificateDer`, `issuerDn`, `subjectDn`, `notBefore`, `notAfter`, `serialNumberHex`, `akiHex`, `skiHex`, `signatureAlgorithmName`, `signatureAlgorithmOid`, `keyUsages`, `extendedKeyUsages` |
| `RenewalResponse(V1)` | `serialNumberHex`, `subjectDn`, `certificateDerBase64` |
| `RevocationResponseV1` | `revoked`, `failedReason` |
| `CertProfileResponse(V1)` | `id`, `name` |
| `CertProfileDetailResponse` | `id`, `name`, `certType`, `validityPeriod`, `isCa`, `pathLenConstraint`, `keyUsageCritical`, `requireApproval`, `keyUsages`, `extendedKeyUsages[{oid, purpose}]`, `certificateOidsCsv`, `certificatePoliciesCritical`, `sanCsv`, `keyAlgorithm`, `useDefaultAiaCaIssuer`, `ocspUris`, `profileCustomExtensions[{oid, critical, oidValue, valueType}]`, `createdAt`, `updatedAt` |
| `CertProfileDetailResponseV1` | 위에서 `profileCustomExtensions` 제외 |
| `CertProfilePageResponse(V1)` | `certProfileDetailResponses`, `page`, `size`, `totalElements`, `totalPages`, `blockStart`, `blockEnd` |
| `EkuResponse(V1)` | `id`, `oid`, `purpose` |
| 통계 응답 | `groupBy` + `certIssueRequestCount` / `issuedCertCount` / `revokedCertCount` |

enum 값: `CertStatus` `VALID`, `REVOKED`, `PENDING`, `EXPIRED`. `KeyUsage` `DIGITAL_SIGNATURE` … `DECIPHER_ONLY`(9개).
`CustomOidValueType` `UTF8_STRING`, `BIT_STRING`, `PRINTABLE_STRING`, `IA5_STRING`, `INTEGER`, `BOOLEAN`, `OBJECT_IDENTIFIER`, `OCTET_STRING_HEX`, `NULL`, `UNKNOWN`.

## 보안 (아직 옮기지 않음)

x509-ra 는 SecurityFilterChain 을 경로별로 나눈다. 계약만 옮긴 지금은 Spring Security 가 없어 모든 경로가 열려 있다.

| 체인 | 대상 | 동작 |
|---|---|---|
| 0 | `/internal/deployment/**` | CSRF 끔, `deploymentInternalAccessFilter` 로 접근 제한 후 허용 |
| 1 | `/actuator/**` | health, prometheus 만 허용 |
| 2 | `/api/**`, `/cmp` | 무상태. `/cmp`, `/api/ca/*.crt`, `/api/crl/*.crl` 은 항상 허용. `pki.features.openapi=false` 면 키 합의·동기화만 허용하고 나머지 거절. openapi + TLS 면 mTLS 클라이언트 인증서(`X509PrincipalExtractor`)의 권한으로 `RaApiPolicy` 검사, 정책에 없는 경로는 거절. openapi + 비TLS 면 모두 허용 |

## 구현 단계에서 옮길 것

- 각 엔드포인트의 동작과 도메인 에러 코드(`CERT_*`, `PROFILE_*`, `STATS_*` …)와 그 HTTP 상태
- 다운로드 `format` 검증, CRL·ZIP·PEM 응답 헤더(`Content-Disposition`, 파일명 규칙)
- v1 폐기의 `RevocationResponseV1` 오류 본문
- CMP 의 PKIMessage 오류 응답과 요청 본문 캐싱
- 보안 체인과 `RaApiPolicy`, 기능 플래그(`pki.features.openapi`, `pki.features.key-sync.enabled`, `key-agreement.role`)
