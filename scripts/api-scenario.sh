#!/usr/bin/env bash
set -euo pipefail

BASE=${1:-http://localhost:8080}
RUN=$(date +%s)
WORK=$(mktemp -d)
JSON='Content-Type: application/json'
trap 'rm -rf "$WORK"' EXIT

step() { printf '\n== %s\n' "$*"; }
json() { python3 -c "import sys, json; print(json.load(sys.stdin)$1)"; }
expect_status() {
    local expected=$1; shift
    local status
    status=$(curl -s -o "$WORK/body" -w '%{http_code}' "$@")
    if [[ "$status" != "$expected" ]]; then
        echo "FAIL: expected $expected but got $status"; cat "$WORK/body"; echo; exit 1
    fi
    cat "$WORK/body"
}
pem_from_der_b64() { python3 -c "import sys,base64,textwrap; d=sys.stdin.read().strip(); print('-----BEGIN CERTIFICATE-----'); print('\n'.join(textwrap.wrap(d,64))); print('-----END CERTIFICATE-----')"; }

step "1. 기본 EKU 확인"
expect_status 200 "$BASE/api/extended-key-usages" | json "[0]"

step "2. EKU 등록 (V2X 용 사설 OID)"
expect_status 200 -X POST "$BASE/api/extended-key-usages" -H "$JSON" \
    -d "{\"oid\":\"1.3.6.1.4.1.99999.$RUN\",\"purpose\":\"V2X Device\"}"
echo "(빈 본문)"

step "3. 프로파일 생성: Root, Sub CA, TLS 서버 Leaf, 승인 필요 Leaf"
ROOT_PROFILE=$(expect_status 200 -X POST "$BASE/api/profiles" -H "$JSON" -d "{
  \"policyName\":\"Root CA $RUN\",\"certType\":\"ROOT\",\"validityPeriod\":\"P20Y\",
  \"keyUsages\":[],\"keyAlgorithmSpec\":\"EC-P-384\"}" | json "['id']")
SUB_PROFILE=$(expect_status 200 -X POST "$BASE/api/profiles" -H "$JSON" -d "{
  \"policyName\":\"Issuing CA $RUN\",\"certType\":\"SUB\",\"validityPeriod\":\"P10Y\",\"pathLenConstraint\":0,
  \"keyUsages\":[\"DIGITAL_SIGNATURE\"],\"useDefaultAiaCaIssuer\":true,\"keyAlgorithmSpec\":\"EC-P-256\"}" | json "['id']")
TLS_PROFILE=$(expect_status 200 -X POST "$BASE/api/profiles" -H "$JSON" -d "{
  \"policyName\":\"TLS Server $RUN\",\"certType\":\"LEAF\",\"validityPeriod\":\"P1Y\",
  \"keyUsages\":[\"DIGITAL_SIGNATURE\",\"KEY_CERT_SIGN\"],\"extendKeyUsage\":[\"1.3.6.1.5.5.7.3.1\"],
  \"subjectAltName\":\"DNS:www.example.com\",\"useDefaultAiaCaIssuer\":true,\"keyAlgorithmSpec\":\"EC-P-256\"}" | json "['id']")
APPROVAL_PROFILE=$(expect_status 200 -X POST "$BASE/api/profiles" -H "$JSON" -d "{
  \"policyName\":\"Device Approval $RUN\",\"certType\":\"LEAF\",\"validityPeriod\":\"P90D\",
  \"keyUsages\":[\"DIGITAL_SIGNATURE\"],\"extendKeyUsage\":[\"1.3.6.1.4.1.99999.$RUN\"],
  \"requireApproval\":true,\"keyAlgorithmSpec\":\"EC-P-256\"}" | json "['id']")
echo "root=$ROOT_PROFILE sub=$SUB_PROFILE tls=$TLS_PROFILE approval=$APPROVAL_PROFILE"

step "4. 프로파일 상세 조회 (TLS: KEY_CERT_SIGN 은 Leaf 라 빠진다)"
expect_status 200 -G "$BASE/api/profiles/detail" --data-urlencode "profileName=TLS Server $RUN" | json "['keyUsages']"

step "5. 프로파일 목록 조회"
expect_status 200 "$BASE/api/profiles?page=0&size=5" | json "['totalElements']"

step "6. Root CA 발급 (PEM 응답)"
expect_status 200 -X POST "$BASE/api/certificates/root" -H "$JSON" \
    -d "{\"certProfileId\":$ROOT_PROFILE,\"subjectDn\":\"CN=Scenario Root CA $RUN,O=Meteor,C=KR\"}" > "$WORK/root.pem"
openssl x509 -in "$WORK/root.pem" -noout -subject -ext basicConstraints

step "7. CA 목록에서 Root 의 SKI 찾기"
CA_LIST=$(expect_status 200 "$BASE/api/certificates/ca")
ROOT_SKI=$(echo "$CA_LIST" | python3 -c "import sys,json; print([c['skiHex'] for c in json.load(sys.stdin) if c['subjectDn']=='CN=Scenario Root CA $RUN,O=Meteor,C=KR'][0])")
echo "root SKI=$ROOT_SKI"

step "8. Sub CA 발급 (발급자 = Root, 기본 CRL DP 포함)"
expect_status 200 -X POST "$BASE/api/certificates/sub-ca" -H "$JSON" -d "{
  \"certProfileId\":$SUB_PROFILE,\"subjectDn\":\"CN=Scenario Issuing CA $RUN,O=Meteor,C=KR\",
  \"akiHex\":\"$ROOT_SKI\",\"includeDefaultCrlDp\":true,\"validityPeriod\":\"P5Y\"}" \
    | json "['x509CertificateDer']" | pem_from_der_b64 > "$WORK/sub.pem"
openssl x509 -in "$WORK/sub.pem" -noout -subject -issuer -ext basicConstraints,crlDistributionPoints,authorityInfoAccess
SUB_SKI=$(openssl x509 -in "$WORK/sub.pem" -noout -ext subjectKeyIdentifier | tail -1 | tr -d ' :')

step "9. Sub CA 단건 조회"
expect_status 200 "$BASE/api/certificates/ca/$SUB_SKI" | json "['certStatus']"

step "10. Leaf 발급 - 공개키(SPKI)"
openssl ecparam -name prime256v1 -genkey -noout -out "$WORK/device.key" 2>/dev/null
SPKI=$(openssl pkey -in "$WORK/device.key" -pubout -outform der | base64 | tr -d '\n')
expect_status 200 -X POST "$BASE/api/certificates/leaf/spki" -H "$JSON" -d "{
  \"subjectDn\":\"CN=www.example.com,O=Meteor\",\"akiHex\":\"$SUB_SKI\",\"subjectPublicKeyInfoDer\":\"$SPKI\",
  \"certProfileId\":$TLS_PROFILE,\"includeDefaultCrlDp\":true}" \
    | json "['x509CertificateDer']" | pem_from_der_b64 > "$WORK/leaf-spki.pem"
openssl x509 -in "$WORK/leaf-spki.pem" -noout -subject -ext subjectAltName,extendedKeyUsage

step "11. Leaf 발급 - CSR(PKCS#10). SAN 은 CSR 의 것을 쓴다"
openssl req -new -key "$WORK/device.key" -subj "/CN=api.example.com/O=Meteor" \
    -addext "subjectAltName=DNS:api.example.com,DNS:api2.example.com" -out "$WORK/device.csr" 2>/dev/null
CSR=$(python3 -c "import json; print(json.dumps(open('$WORK/device.csr').read()))")
expect_status 200 -X POST "$BASE/api/certificates/leaf/p10cr" -H "$JSON" -d "{
  \"p10crPem\":$CSR,\"certProfileId\":$TLS_PROFILE,\"akiHex\":\"$SUB_SKI\",\"validityPeriod\":\"P90D\"}" > "$WORK/leaf-csr.json"
json "['serialNumberHex']" < "$WORK/leaf-csr.json"
json "['x509CertificateDer']" < "$WORK/leaf-csr.json" | pem_from_der_b64 > "$WORK/leaf-csr.pem"
openssl x509 -in "$WORK/leaf-csr.pem" -noout -subject -dates -ext subjectAltName

step "12. 승인이 필요한 프로파일로 발급 (응답은 같고 서버에는 PENDING 으로 저장)"
expect_status 200 -X POST "$BASE/api/certificates/leaf/spki" -H "$JSON" -d "{
  \"subjectDn\":\"CN=device-001\",\"akiHex\":\"$SUB_SKI\",\"subjectPublicKeyInfoDer\":\"$SPKI\",
  \"certProfileId\":$APPROVAL_PROFILE}" | json "['x509CertificateDer'][:20]"

step "13. 체인 검증"
openssl verify -CAfile "$WORK/root.pem" -untrusted "$WORK/sub.pem" "$WORK/sub.pem" "$WORK/leaf-spki.pem" "$WORK/leaf-csr.pem"

step "14. 인증서 다운로드 (DER, PEM)"
SERIAL=$(json "['serialNumberHex']" < "$WORK/leaf-csr.json")
curl -s -D - -o "$WORK/dl.der" "$BASE/api/certificates/$SUB_SKI/download?serialNumberHex=$SERIAL&format=der" \
    | grep -iE '^content-(type|disposition)'
cmp <(openssl x509 -in "$WORK/leaf-csr.pem" -outform der) "$WORK/dl.der" && echo "DER 다운로드가 발급 결과와 같다"
curl -s "$BASE/api/certificates/$SUB_SKI/download?serialNumberHex=$SERIAL" | head -1

step "15. Sub CA 갱신 (새 키로 발급하고 원본은 SUPERSEDED 로 폐기)"
SUB_SERIAL=$(openssl x509 -in "$WORK/sub.pem" -noout -serial | cut -d= -f2)
expect_status 200 -X POST "$BASE/api/certificates/sub-ca/renewal" -H "$JSON" -d "{
  \"serialNumberHex\":\"$SUB_SERIAL\",\"akiHex\":\"$ROOT_SKI\",\"activationStartDaysAgo\":0,\"validityPeriod\":\"P5Y\",
  \"revokeOriginalCertificate\":true,\"useLatestIssuer\":false}" \
    | json "['x509CertificateDer']" | pem_from_der_b64 > "$WORK/sub-renewed.pem"
RENEWED_SKI=$(openssl x509 -in "$WORK/sub-renewed.pem" -noout -ext subjectKeyIdentifier | tail -1 | tr -d ' :')
openssl x509 -in "$WORK/sub-renewed.pem" -noout -subject -dates -ext crlDistributionPoints
echo "원본 SKI=$SUB_SKI 갱신본 SKI=$RENEWED_SKI"
[[ "$SUB_SKI" != "$RENEWED_SKI" ]] && echo "키를 재사용하지 않았다"
openssl verify -CAfile "$WORK/root.pem" "$WORK/sub-renewed.pem"

step "16. CA 목록에서 원본은 REVOKED, 갱신본은 VALID"
expect_status 200 "$BASE/api/certificates/ca" | python3 -c "import sys,json
for c in json.load(sys.stdin):
    if c['subjectDn']=='CN=Scenario Issuing CA $RUN,O=Meteor,C=KR': print(c['skiHex'], c['certStatus'])"

step "17. 갱신한 Sub CA 로 Leaf 발급"
expect_status 200 -X POST "$BASE/api/certificates/leaf/spki" -H "$JSON" -d "{
  \"subjectDn\":\"CN=www.example.com,O=Meteor\",\"akiHex\":\"$RENEWED_SKI\",\"subjectPublicKeyInfoDer\":\"$SPKI\",
  \"certProfileId\":$TLS_PROFILE,\"includeDefaultCrlDp\":true}" \
    | json "['x509CertificateDer']" | pem_from_der_b64 > "$WORK/leaf-renewed.pem"
openssl verify -CAfile "$WORK/root.pem" -untrusted "$WORK/sub-renewed.pem" "$WORK/leaf-renewed.pem"

step "18. Sub CA 폐기 (reason 5 = cessationOfOperation)"
expect_status 200 -X POST "$BASE/api/certificates/sub-ca" -H "$JSON" -d "{
  \"certProfileId\":$SUB_PROFILE,\"subjectDn\":\"CN=Scenario Retired CA $RUN,O=Meteor,C=KR\",
  \"akiHex\":\"$ROOT_SKI\",\"validityPeriod\":\"P1Y\"}" | json "['x509CertificateDer']" | pem_from_der_b64 > "$WORK/retired.pem"
RETIRED_SERIAL=$(openssl x509 -in "$WORK/retired.pem" -noout -serial | cut -d= -f2)
RETIRED_SKI=$(openssl x509 -in "$WORK/retired.pem" -noout -ext subjectKeyIdentifier | tail -1 | tr -d ' :')
expect_status 200 -X POST "$BASE/api/certificates/sub-ca/$ROOT_SKI/revocation" -H "$JSON" \
    -d "{\"serialNumberHex\":\"$RETIRED_SERIAL\",\"reason\":5}"
echo "(빈 본문)"
echo "- 폐기한 Sub CA 로는 발급할 수 없다:"
expect_status 404 -X POST "$BASE/api/certificates/leaf/spki" -H "$JSON" -d "{
  \"subjectDn\":\"CN=x\",\"akiHex\":\"$RETIRED_SKI\",\"subjectPublicKeyInfoDer\":\"$SPKI\",
  \"certProfileId\":$TLS_PROFILE}" | json "['detail']"

step "19. 오류 응답"
echo "- 같은 이름의 프로파일:"
expect_status 400 -X POST "$BASE/api/profiles" -H "$JSON" -d "{
  \"policyName\":\"Root CA $RUN\",\"certType\":\"ROOT\",\"validityPeriod\":\"P20Y\",\"keyUsages\":[],
  \"keyAlgorithmSpec\":\"EC-P-384\"}" | json "['errorCode']"
echo "- 프로파일 종류가 다름(Leaf 프로파일로 Root 발급):"
expect_status 404 -X POST "$BASE/api/certificates/root" -H "$JSON" \
    -d "{\"certProfileId\":$TLS_PROFILE,\"subjectDn\":\"CN=Wrong\"}" | json "['errorCode']"
echo "- pathLenConstraint 0 인 Sub CA 아래 CA 발급:"
expect_status 400 -X POST "$BASE/api/certificates/sub-ca" -H "$JSON" \
    -d "{\"certProfileId\":$SUB_PROFILE,\"subjectDn\":\"CN=Too Deep\",\"akiHex\":\"$RENEWED_SKI\"}" | json "['detail']"
echo "- 프로파일보다 긴 유효기간:"
expect_status 400 -X POST "$BASE/api/certificates/leaf/spki" -H "$JSON" -d "{
  \"subjectDn\":\"CN=x\",\"akiHex\":\"$RENEWED_SKI\",\"subjectPublicKeyInfoDer\":\"$SPKI\",
  \"certProfileId\":$TLS_PROFILE,\"validityPeriod\":\"P2Y\"}" | json "['detail']"
echo "- 상위 CA(Root 20년)의 유효기간을 넘는 Sub CA 발급 (2040 년부터 10년):"
expect_status 400 -X POST "$BASE/api/certificates/sub-ca" -H "$JSON" -d "{
  \"certProfileId\":$SUB_PROFILE,\"subjectDn\":\"CN=Too Long\",\"akiHex\":\"$ROOT_SKI\",
  \"notBefore\":\"2040-01-01T00:00:00Z\"}" | json "['detail']"
echo "- 상위 CA(Root 20년)의 유효기간을 넘는 갱신:"
RENEWED_SERIAL=$(openssl x509 -in "$WORK/sub-renewed.pem" -noout -serial | cut -d= -f2)
expect_status 400 -X POST "$BASE/api/certificates/sub-ca/renewal" -H "$JSON" -d "{
  \"serialNumberHex\":\"$RENEWED_SERIAL\",\"akiHex\":\"$ROOT_SKI\",\"activationStartDaysAgo\":0,
  \"validityPeriod\":\"P25Y\",\"revokeOriginalCertificate\":false,\"useLatestIssuer\":false}" | json "['detail']"
echo "- Root 폐기:"
ROOT_SERIAL=$(openssl x509 -in "$WORK/root.pem" -noout -serial | cut -d= -f2)
expect_status 400 -X POST "$BASE/api/certificates/sub-ca/$ROOT_SKI/revocation" -H "$JSON" \
    -d "{\"serialNumberHex\":\"$ROOT_SERIAL\",\"reason\":1}" | json "['errorCode']"
echo "- 이미 폐기한 Sub CA 폐기:"
expect_status 400 -X POST "$BASE/api/certificates/sub-ca/$ROOT_SKI/revocation" -H "$JSON" \
    -d "{\"serialNumberHex\":\"$RETIRED_SERIAL\",\"reason\":1}" | json "['detail']"
echo "- 없는 발급자:"
expect_status 404 -X POST "$BASE/api/certificates/leaf/spki" -H "$JSON" -d "{
  \"subjectDn\":\"CN=x\",\"akiHex\":\"00FF\",\"subjectPublicKeyInfoDer\":\"$SPKI\",
  \"certProfileId\":$TLS_PROFILE}" | json "['errorCode']"

printf '\n모든 단계 통과\n'
