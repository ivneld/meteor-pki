# REST API 호출 시나리오

인증서 프로파일을 만들고 Root CA → Sub CA → Leaf 순서로 체인을 만든 뒤, 조회·다운로드·오류 응답까지 확인하는 순서다. 같은 순서를 실행하는
스크립트 [`scripts/api-scenario.sh`](../scripts/api-scenario.sh) 가 있다. 스크립트는 각 응답을 `openssl` 로 검증하고, 기대와 다르면 멈춘다.

```bash
./gradlew :core:core-api:bootRun          # local 프로필. H2 메모리 DB, 개인키는 메모리 보관(재기동하면 사라진다)
scripts/api-scenario.sh                    # 기본 http://localhost:8080. 필요: curl, python3, openssl
scripts/api-scenario.sh http://host:port   # 다른 서버
```

스크립트는 실행할 때마다 프로파일 이름과 DN 뒤에 다른 접미어를 붙이므로 서버를 다시 띄우지 않고 여러 번 돌려도 된다. 아래 예시는 접미어 없이 적었다.
`{...}` 는 앞 단계 응답에서 꺼낸 값이다.

## 준비물

| 값 | 얻는 곳 |
|---|---|
| 프로파일 id | 3 단계 응답의 `id` |
| Root SKI | 7 단계 CA 목록의 `skiHex`. 하위 인증서를 발급할 때 `akiHex` 로 넘긴다 |
| Sub CA SKI | 8 단계 응답 인증서의 SKI 확장, 또는 CA 목록 |
| Leaf 공개키 | `openssl ecparam -name prime256v1 -genkey -noout -out device.key` 후 `openssl pkey -in device.key -pubout -outform der \| base64` |

## 1. 기본 EKU 확인

서버는 기동할 때 RFC 5280 의 표준 EKU 6개를 등록한다(`pki.bootstrap.eku.enabled=false` 로 끈다).

```bash
curl localhost:8080/api/extended-key-usages
```

```json
[{"id":1,"oid":"1.3.6.1.5.5.7.3.1","purpose":"TLS Web Server Authentication"}, ...]
```

## 2. EKU 등록

프로파일은 등록된 EKU 만 쓸 수 있다. 성공하면 200 에 본문이 없다. 같은 OID 를 다시 등록하면 400 `PROFILE_INVALID_INPUT`.

```bash
curl -X POST localhost:8080/api/extended-key-usages -H 'Content-Type: application/json' \
     -d '{"oid":"1.3.6.1.4.1.99999.1","purpose":"V2X Device"}'
```

## 3. 프로파일 생성

```bash
# Root CA: EC P-384, 20년. keyCertSign·cRLSign 과 critical 키 용도는 서버가 강제한다
curl -X POST localhost:8080/api/profiles -H 'Content-Type: application/json' -d '{
  "policyName":"Root CA","certType":"ROOT","validityPeriod":"P20Y","keyUsages":[],"keyAlgorithmSpec":"EC-P-384"}'

# Sub CA: 아래로 CA 를 더 둘 수 없게(pathLen 0), 발급하는 인증서에 발급자 인증서 주소(AIA)를 넣는다
curl -X POST localhost:8080/api/profiles -H 'Content-Type: application/json' -d '{
  "policyName":"Issuing CA","certType":"SUB","validityPeriod":"P10Y","pathLenConstraint":0,
  "keyUsages":["DIGITAL_SIGNATURE"],"useDefaultAiaCaIssuer":true,"keyAlgorithmSpec":"EC-P-256"}'

# TLS 서버 Leaf: SAN 템플릿과 serverAuth EKU. Leaf 의 KEY_CERT_SIGN 은 조용히 빠진다
curl -X POST localhost:8080/api/profiles -H 'Content-Type: application/json' -d '{
  "policyName":"TLS Server","certType":"LEAF","validityPeriod":"P1Y",
  "keyUsages":["DIGITAL_SIGNATURE","KEY_CERT_SIGN"],"extendKeyUsage":["1.3.6.1.5.5.7.3.1"],
  "subjectAltName":"DNS:www.example.com","useDefaultAiaCaIssuer":true,"keyAlgorithmSpec":"EC-P-256"}'

# 승인이 필요한 Leaf: 2 단계에서 등록한 EKU 를 쓴다
curl -X POST localhost:8080/api/profiles -H 'Content-Type: application/json' -d '{
  "policyName":"Device Approval","certType":"LEAF","validityPeriod":"P90D","keyUsages":["DIGITAL_SIGNATURE"],
  "extendKeyUsage":["1.3.6.1.4.1.99999.1"],"requireApproval":true,"keyAlgorithmSpec":"EC-P-256"}'
```

응답은 `{"id":1,"name":"Root CA"}` 모양이다. `keyAlgorithmSpec` 은 표시값(`EC-P-256`, `RSA-2048`, `ML-DSA-44`, `Ed448` ...)이다.
`useDefaultOcspUri` 를 빼면 true 라서 SUB·LEAF 프로파일에는 기본 OCSP 주소(`http://127.0.0.1/api/ocsp`)가 들어간다. ROOT 는 OCSP 주소를 갖지 않는다.

## 4. 프로파일 상세 조회

```bash
curl 'localhost:8080/api/profiles/detail?profileName=TLS%20Server'
```

`keyUsages` 가 `["DIGITAL_SIGNATURE"]`, `isCa` 가 false, `ocspUris` 에 기본 OCSP 주소가 있는지 본다.

## 5. 프로파일 목록 조회

```bash
curl 'localhost:8080/api/profiles?page=0&size=20'
```

`certProfileDetailResponses`, `totalElements`, `totalPages` 와 화면 페이지 버튼용 `blockStart`·`blockEnd`(5 페이지 단위)가 온다.

## 6. Root CA 발급

새 키를 만들어 자기 서명한다. 응답은 PEM 텍스트(`application/x-pem-file`)다.

```bash
curl -X POST localhost:8080/api/certificates/root -H 'Content-Type: application/json' \
     -d '{"certProfileId":1,"subjectDn":"CN=Scenario Root CA,O=Meteor,C=KR"}' > root.pem
openssl x509 -in root.pem -noout -text
```

`validityPeriod` 로 프로파일보다 짧게만 줄일 수 있다. `notBefore` 를 빼면 지금이다.

## 7. CA 목록 조회

체인((AKI, SKI) 쌍)마다 가장 최근 CA 인증서를 돌려준다. `skiHex` 가 하위 인증서를 발급할 때 쓰는 값이다.

```bash
curl localhost:8080/api/certificates/ca
```

```json
[{"subjectDn":"CN=Scenario Root CA,O=Meteor,C=KR","skiHex":"B296F3...","certType":"ROOT","certStatus":"VALID",
  "trackingId":"...","certPem":"-----BEGIN CERTIFICATE-----...", ...}]
```

## 8. Sub CA 발급

`akiHex` 는 발급자(Root)의 SKI 다. `includeDefaultCrlDp` 가 true 면 발급자 SKI 로 만든 기본 CRL 주소가 들어간다. 응답의
`x509CertificateDer` 는 Base64 DER 다.

```bash
curl -X POST localhost:8080/api/certificates/sub-ca -H 'Content-Type: application/json' -d '{
  "certProfileId":2,"subjectDn":"CN=Scenario Issuing CA,O=Meteor,C=KR",
  "akiHex":"{Root SKI}","includeDefaultCrlDp":true,"validityPeriod":"P5Y"}'
```

인증서에서 확인할 것:

```
X509v3 Basic Constraints: critical  CA:TRUE, pathlen:0
X509v3 CRL Distribution Points:     URI:http://127.0.0.1/api/crl/{Root SKI}.crl
Authority Information Access:       CA Issuers - URI:http://127.0.0.1/api/ca/{Root SKI}.crt
                                    OCSP - URI:http://127.0.0.1/api/ocsp
```

기본 주소의 호스트는 `pki.distribution-point.{ocsp,ca,crl}-base-url` 로 바꾼다.

## 9. CA 단건 조회

```bash
curl localhost:8080/api/certificates/ca/{Sub CA SKI}
```

`certType` SUB, `certStatus` VALID, `akiHex` 가 Root SKI 인지 본다. `signatureAlgorithmName` 은 발급자(Root, P-384) 키에 맞는
`SHA384withECDSA` 다.

## 10. Leaf 발급: 공개키(SPKI)

신청자가 키를 갖고 공개키만 보낸다. 공개키 알고리즘은 프로파일의 `keyAlgorithm` 과 같아야 한다.

```bash
curl -X POST localhost:8080/api/certificates/leaf/spki -H 'Content-Type: application/json' -d '{
  "subjectDn":"CN=www.example.com,O=Meteor","akiHex":"{Sub CA SKI}",
  "subjectPublicKeyInfoDer":"{Base64 SPKI}","certProfileId":3,"includeDefaultCrlDp":true}'
```

응답은 `{"x509CertificateDer":"..."}` 다. SAN 은 프로파일 템플릿(`DNS:www.example.com`), EKU 는 serverAuth 다.

## 11. Leaf 발급: CSR(PKCS#10)

주체 DN 과 공개키는 CSR 에서 꺼낸다. CSR 이 SAN 을 요청했으면 프로파일 SAN 대신 그것을 쓰고, 그 밖의 요청 확장은 무시한다. CSR 서명이
맞지 않으면 400 "Failed to verify P10CR signature." 다.

```bash
openssl req -new -key device.key -subj "/CN=api.example.com/O=Meteor" \
    -addext "subjectAltName=DNS:api.example.com,DNS:api2.example.com" -out device.csr
curl -X POST localhost:8080/api/certificates/leaf/p10cr -H 'Content-Type: application/json' -d "{
  \"p10crPem\":$(python3 -c 'import json; print(json.dumps(open("device.csr").read()))'),
  \"certProfileId\":3,\"akiHex\":\"{Sub CA SKI}\",\"validityPeriod\":\"P90D\"}"
```

응답에는 인증서와 함께 `issuerDn`, `subjectDn`, `notBefore`, `notAfter`, `serialNumberHex`, `akiHex`, `skiHex`, 서명 알고리즘,
`keyUsages`, `extendedKeyUsages` 가 온다.

## 12. 승인이 필요한 프로파일로 발급

응답은 10 단계와 같다. 서버에는 인증서가 `PENDING` 으로 저장되고 CA 로 쓰이지 않는다. 승인·반려 API 는 x509-ra 에서도 화면 전용이라 아직 없다.

```bash
curl -X POST localhost:8080/api/certificates/leaf/spki -H 'Content-Type: application/json' -d '{
  "subjectDn":"CN=device-001","akiHex":"{Sub CA SKI}","subjectPublicKeyInfoDer":"{Base64 SPKI}","certProfileId":4}'
```

## 13. 체인 검증

```bash
openssl verify -CAfile root.pem -untrusted sub.pem sub.pem leaf-spki.pem leaf-csr.pem
```

모두 `OK` 여야 한다.

## 14. 인증서 다운로드

발급자 SKI(경로)와 일련번호(쿼리)로 찾는다. `format` 은 `pem`(기본) 또는 `der` 이고, 파일명은 `{일련번호}.{pem|der}` 다.

```bash
curl -OJ 'localhost:8080/api/certificates/{Sub CA SKI}/download?serialNumberHex={일련번호}&format=der'
```

```
Content-Type: application/pkix-cert
Content-Disposition: attachment; filename="4903BF....der"
```

## 15. 오류 응답

오류는 모두 `title: PKI-ERROR` 인 ProblemDetail 이고, 클라이언트는 `errorCode` 로 분기한다.

| 요청 | 상태 | errorCode | detail |
|---|---|---|---|
| 같은 이름으로 프로파일 생성 | 400 | `PROFILE_INVALID_INPUT` | Certificate profile with the same name already exists. |
| 등록되지 않은 EKU 를 프로파일에 넣음 | 400 | `PROFILE_INVALID_INPUT` | Unknown EKU OID. |
| `keyAlgorithmSpec` 에 enum 이름(`EC_P256`)을 씀 | 400 | `CERT_INVALID_INPUT` | Unsupported key algorithm spec. |
| 없는 프로파일 이름으로 상세 조회 | 404 | `PROFILE_RESOURCE_NOT_FOUND` | Certificate profile not found. |
| Leaf 프로파일로 Root 발급 | 404 | `PROFILE_RESOURCE_NOT_FOUND` | Certificate profile not found. |
| 없는 발급자 SKI | 404 | `CERT_RESOURCE_NOT_FOUND` | Failed to find a CA certificate matching the AKI. |
| pathLen 0 인 Sub CA 아래 CA 발급 | 400 | `CERT_INVALID_INPUT` | Issuer certificate cannot issue: pathLenConstraint is 0. |
| 프로파일보다 긴 `validityPeriod` | 400 | `CERT_INVALID_INPUT` | Validity is outside allowed policy range. |
| 프로파일과 다른 키 알고리즘의 공개키 | 400 | `CERT_INVALID_INPUT` | The subject public key does not match the profile's key spec. |
| PEM 이 아닌 CSR | 400 | `CERT_INVALID_INPUT` | Invalid PEM format |
| 지원하지 않는 다운로드 형식 | 400 | `CERT_INVALID_INPUT` | Unsupported certificate download format. |

```json
{"type":"about:blank","title":"PKI-ERROR","status":400,"detail":"Issuer certificate cannot issue: pathLenConstraint is 0.",
 "instance":"/api/certificates/sub-ca","errorCode":"CERT_INVALID_INPUT","timestamp":"2026-10-13T05:47:38Z","skiHex":"54F5..."}
```
