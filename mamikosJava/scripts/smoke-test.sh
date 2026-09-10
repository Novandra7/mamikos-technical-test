#!/bin/bash
# Manual end-to-end smoke test against a running instance on localhost:8080.
# Mirrors the PRD's demo scenario (PRD-02-SpringBoot.md, section 20).
set -u
BASE="http://localhost:8080/api/v1"
pass=0
fail=0

check() {
  local desc="$1" expected="$2" actual="$3"
  if [ "$expected" = "$actual" ]; then
    echo "PASS: $desc (got $actual)"
    pass=$((pass+1))
  else
    echo "FAIL: $desc (expected $expected, got $actual)"
    fail=$((fail+1))
  fi
}

echo "== Register regular user =="
REG=$(curl -s -X POST "$BASE/auth/register" -H "Content-Type: application/json" \
  -d '{"name":"Rina","email":"rina-smoke@example.com","password":"Password123","passwordConfirmation":"Password123","role":"regular"}')
echo "$REG" | python -m json.tool
USER_TOKEN=$(echo "$REG" | python -c "import json,sys;print(json.load(sys.stdin)['data']['token']['accessToken'])")
BAL=$(echo "$REG" | python -c "import json,sys;print(json.load(sys.stdin)['data']['credit']['balance'])")
check "regular initial balance is 20" "20" "$BAL"

echo "== Register premium user =="
REGP=$(curl -s -X POST "$BASE/auth/register" -H "Content-Type: application/json" \
  -d '{"name":"Dimas","email":"dimas-smoke@example.com","password":"Password123","passwordConfirmation":"Password123","role":"premium"}')
BALP=$(echo "$REGP" | python -c "import json,sys;print(json.load(sys.stdin)['data']['credit']['balance'])")
check "premium initial balance is 40" "40" "$BALP"

echo "== Register owner =="
REGO=$(curl -s -X POST "$BASE/auth/register" -H "Content-Type: application/json" \
  -d '{"name":"Pak Owner","email":"owner-smoke@example.com","password":"Password123","passwordConfirmation":"Password123","role":"owner"}')
OWNER_TOKEN=$(echo "$REGO" | python -c "import json,sys;print(json.load(sys.stdin)['data']['token']['accessToken'])")
CREDIT_NULL=$(echo "$REGO" | python -c "import json,sys;print(json.load(sys.stdin)['data'].get('credit'))")
check "owner has no credit block" "None" "$CREDIT_NULL"

echo "== Owner creates a kost =="
KOST=$(curl -s -X POST "$BASE/owner/kosts" -H "Authorization: Bearer $OWNER_TOKEN" -H "Content-Type: application/json" \
  -d '{"name":"Kost Melati Residence","description":"Kost putri dekat kampus","address":{"street":"Jl. Kaliurang KM 5","district":"Depok","city":"Sleman","province":"DI Yogyakarta"},"pricePerMonth":950000,"roomType":"PUTRI","totalRooms":12,"availableRooms":4,"facilities":["wifi","kamar mandi dalam"]}')
echo "$KOST" | python -m json.tool
KOST_ID=$(echo "$KOST" | python -c "import json,sys;print(json.load(sys.stdin)['data']['id'])")
check "kost created" "true" "$(echo "$KOST" | python -c "import json,sys;print(str(json.load(sys.stdin)['success']).lower())")"

echo "== Public search finds it, hides availableRooms =="
SEARCH=$(curl -s "$BASE/kosts?location=Sleman&sortBy=price&order=asc")
HAS_AVAIL=$(echo "$SEARCH" | python -c "import json,sys;d=json.load(sys.stdin);print('availableRooms' in json.dumps(d['data']))")
check "search result has no availableRooms field" "False" "$HAS_AVAIL"

echo "== Public detail hides availableRooms =="
DETAIL=$(curl -s "$BASE/kosts/$KOST_ID")
DISCLOSED=$(echo "$DETAIL" | python -c "import json,sys;print(json.load(sys.stdin)['data']['availability']['disclosed'])")
check "public detail availability.disclosed is false" "False" "$DISCLOSED"

echo "== Owner cannot inquire =="
OWNER_INQUIRY_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/kosts/$KOST_ID/availability-inquiries" \
  -H "Authorization: Bearer $OWNER_TOKEN" -H "Content-Type: application/json" -d '{"message":"test"}')
check "owner inquiry is 403" "403" "$OWNER_INQUIRY_STATUS"

echo "== User asks about availability (-5 credit) =="
INQ=$(curl -s -X POST "$BASE/kosts/$KOST_ID/availability-inquiries" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" \
  -d '{"message":"Masih ada kamar kosong?"}')
echo "$INQ" | python -m json.tool
AFTER=$(echo "$INQ" | python -c "import json,sys;print(json.load(sys.stdin)['data']['credit']['balanceAfter'])")
check "balance after first inquiry is 15" "15" "$AFTER"
AVAIL_ROOMS=$(echo "$INQ" | python -c "import json,sys;print(json.load(sys.stdin)['data']['availability']['availableRooms'])")
check "inquiry discloses availableRooms=4" "4" "$AVAIL_ROOMS"

echo "== Exhaust credit: 3 more inquiries should succeed, 5th must fail =="
for i in 1 2 3; do
  curl -s -o /dev/null -X POST "$BASE/kosts/$KOST_ID/availability-inquiries" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"message":"lagi"}'
done
FIFTH=$(curl -s -X POST "$BASE/kosts/$KOST_ID/availability-inquiries" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{"message":"lagi"}')
echo "$FIFTH" | python -m json.tool
FIFTH_CODE=$(echo "$FIFTH" | python -c "import json,sys;print(json.load(sys.stdin)['code'])")
check "5th inquiry rejected with INSUFFICIENT_CREDIT" "INSUFFICIENT_CREDIT" "$FIFTH_CODE"

BALANCE_NOW=$(curl -s "$BASE/me/credits" -H "Authorization: Bearer $USER_TOKEN" | python -c "import json,sys;print(json.load(sys.stdin)['data']['balance'])")
check "balance is 0 after exactly 4 successful inquiries" "0" "$BALANCE_NOW"

echo "== Owner sees and replies to inquiry =="
OWNER_INQ=$(curl -s "$BASE/owner/inquiries" -H "Authorization: Bearer $OWNER_TOKEN")
FIRST_INQ_ID=$(echo "$OWNER_INQ" | python -c "import json,sys;print(json.load(sys.stdin)['data'][0]['id'])")
REPLY=$(curl -s -X POST "$BASE/owner/inquiries/$FIRST_INQ_ID/reply" -H "Authorization: Bearer $OWNER_TOKEN" -H "Content-Type: application/json" \
  -d '{"reply":"Masih ada 3 kamar"}')
REPLY_STATUS=$(echo "$REPLY" | python -c "import json,sys;print(json.load(sys.stdin)['data']['status'])")
check "inquiry status is ANSWERED" "ANSWERED" "$REPLY_STATUS"

echo "== Cross-owner access is forbidden =="
KOST2=$(curl -s -X POST "$BASE/owner/kosts" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{}')
KOST2_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE/owner/kosts" -H "Authorization: Bearer $USER_TOKEN" -H "Content-Type: application/json" -d '{}')
check "regular user cannot create kost" "403" "$KOST2_CODE"

echo ""
echo "===================="
echo "PASS: $pass   FAIL: $fail"
echo "===================="
