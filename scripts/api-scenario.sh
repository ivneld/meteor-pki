#!/usr/bin/env bash
# docs/API_SCENARIOS.md 의 시나리오를 실행 중인 서버에 차례로 호출하고 openssl 로 결과를 검증한다.
#
#   ./gradlew :core:core-api:bootRun                 # 다른 터미널에서 서버 실행 (local 프로필, H2 메모리 DB)
#   scripts/api-scenario.sh [http://localhost:8080]   # 요구사항: curl, python3, openssl
#
# 서버를 다시 띄우지 않고 여러 번 돌려도 되도록 프로파일 이름과 DN 뒤에 실행마다 다른 접미어를 붙인다.
set -euo pipefail

BASE=${1:-http://localhost:8080}
RUN=$(date +%s)
WORK=$(mktemp -d)
JSON='Content-Type: application/json'
trap 'rm -rf "$WORK"' EXIT

step() { printf '\n== %s\n' "$*"; }
json() { python3 -c "import sys, json; print(json.load(sys.stdin)$1)"; }
expect_status() {  # expect_status <기대 상태> <curl 인자...>
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
    -d "{\"certProfileId\":$ROOT_PROFILE,\"subjectDn\":\"CN=Scenario Root CA $RUN,O=Autocrypt,C=KR\"}" > "$WORK/root.pem"
openssl x509 -in "$WORK/root.pem" -noout -subject -ext basicConstraints

step "7. CA 목록에서 Root 의 SKI 찾기"
CA_LIST=$(expect_status 200 "$BASE/api/certificates/ca")
ROOT_SKI=$(echo "$CA_LIST" | python3 -c "import sys,json; print([c['skiHex'] for c in json.load(sys.stdin) if c['subjectDn']=='CN=Scenario Root CA $RUN,O=Autocrypt,C=KR'][0])")
echo "root SKI=$ROOT_SKI"

step "8. Sub CA 발급 (발급자 = Root, 기본 CRL DP 포함)"
expect_status 200 -X POST "$BASE/api/certificates/sub-ca" -H "$JSON" -d "{
  \"certProfileId\":$SUB_PROFILE,\"subjectDn\":\"CN=Scenario Issuing CA $RUN,O=Autocrypt,C=KR\",
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
  \"subjectDn\":\"CN=www.example.com,O=Autocrypt\",\"akiHex\":\"$SUB_SKI\",\"subjectPublicKeyInfoDer\":\"$SPKI\",
  \"certProfileId\":$TLS_PROFILE,\"includeDefaultCrlDp\":true}" \
    | json "['x509CertificateDer']" | pem_from_der_b64 > "$WORK/leaf-spki.pem"
openssl x509 -in "$WORK/leaf-spki.pem" -noout -subject -ext subjectAltName,extendedKeyUsage

step "11. Leaf 발급 - CSR(PKCS#10). SAN 은 CSR 의 것을 쓴다"
openssl req -new -key "$WORK/device.key" -subj "/CN=api.example.com/O=Autocrypt" \
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

step "15. 오류 응답"
echo "- 같은 이름의 프로파일:"
expect_status 400 -X POST "$BASE/api/profiles" -H "$JSON" -d "{
  \"policyName\":\"Root CA $RUN\",\"certType\":\"ROOT\",\"validityPeriod\":\"P20Y\",\"keyUsages\":[],
  \"keyAlgorithmSpec\":\"EC-P-384\"}" | json "['errorCode']"
echo "- 프로파일 종류가 다름(Leaf 프로파일로 Root 발급):"
expect_status 404 -X POST "$BASE/api/certificates/root" -H "$JSON" \
    -d "{\"certProfileId\":$TLS_PROFILE,\"subjectDn\":\"CN=Wrong\"}" | json "['errorCode']"
echo "- pathLenConstraint 0 인 Sub CA 아래 CA 발급:"
expect_status 400 -X POST "$BASE/api/certificates/sub-ca" -H "$JSON" \
    -d "{\"certProfileId\":$SUB_PROFILE,\"subjectDn\":\"CN=Too Deep\",\"akiHex\":\"$SUB_SKI\"}" | json "['detail']"
echo "- 프로파일보다 긴 유효기간:"
expect_status 400 -X POST "$BASE/api/certificates/leaf/spki" -H "$JSON" -d "{
  \"subjectDn\":\"CN=x\",\"akiHex\":\"$SUB_SKI\",\"subjectPublicKeyInfoDer\":\"$SPKI\",
  \"certProfileId\":$TLS_PROFILE,\"validityPeriod\":\"P2Y\"}" | json "['detail']"
echo "- 없는 발급자:"
expect_status 404 -X POST "$BASE/api/certificates/leaf/spki" -H "$JSON" -d "{
  \"subjectDn\":\"CN=x\",\"akiHex\":\"00FF\",\"subjectPublicKeyInfoDer\":\"$SPKI\",
  \"certProfileId\":$TLS_PROFILE}" | json "['errorCode']"

printf '\n모든 단계 통과\n'
