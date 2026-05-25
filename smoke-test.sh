#!/bin/bash
set +e
BASE=http://localhost:8081
PSQL="/c/Program Files/PostgreSQL/17/bin/psql.exe"
PGPASSWORD=123456
export PGPASSWORD
RUN_TAG="$(date +%s)"   # unique suffix per run to avoid clashing with leftover data
PASS=0
FAIL=0
RESULTS=()

# Cleanup residue from prior runs so unique constraints don't fire on retest.
# data sources / syncs / sets accumulate fast because the polling job creates rows.
"$PSQL" -h localhost -U postgres -d db_react_springboot -q -c "
DELETE FROM core_meta_set_version;
DELETE FROM core_meta_set;
DELETE FROM core_meta_sync;
DELETE FROM core_meta_source;
DELETE FROM meta_pack_registration;
DELETE FROM meta_pack;
DELETE FROM meta_pack_version;
DELETE FROM core_organization WHERE name LIKE 'Acme%' OR name LIKE 'Local PG%';
DELETE FROM core_domain WHERE name LIKE 'Finance%';
DELETE FROM core_tag WHERE name = 'prod';
" 2>&1 | tail -5

check() {
  local name="$1"
  local actual="$2"
  local expected="$3"
  if [ "$actual" = "$expected" ]; then
    RESULTS+=("PASS  $name  [$actual]")
    PASS=$((PASS+1))
  else
    RESULTS+=("FAIL  $name  got=$actual expected=$expected")
    FAIL=$((FAIL+1))
  fi
}

# ============ AUTH ============
# /api/auth/login is the consumer's AuthController (the starter no longer ships
# /api/authenticate in v0.1.0). Response shape: {"accessToken":"...","refreshToken":"...","expiresInSeconds":...}.
JWT=$(curl -s -X POST $BASE/api/auth/login -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' \
  | sed -nE 's/.*"accessToken"[ ]*:[ ]*"([^"]+)".*/\1/p')
if [ -n "$JWT" ]; then
  RESULTS+=("PASS  AUTH admin login JWT len=${#JWT}")
  PASS=$((PASS+1))
else
  RESULTS+=("FAIL  AUTH admin login")
  FAIL=$((FAIL+1))
fi
H="Authorization: Bearer $JWT"

# ============ CATALOG: ORGANIZATION ============
ORG=$(curl -s -X POST $BASE/api/app/organizations -H "$H" -H "Content-Type: application/json" \
  -d '{"name":"Acme","description":"test"}')
ORG_ID=$(echo "$ORG" | sed -nE 's/.*"id":"([^"]+)".*/\1/p')
if [ -n "$ORG_ID" ]; then
  RESULTS+=("PASS  ORG  CREATE id=$ORG_ID")
  PASS=$((PASS+1))
else
  RESULTS+=("FAIL  ORG  CREATE body=$ORG")
  FAIL=$((FAIL+1))
fi
check "ORG  GET    " "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/app/organizations/$ORG_ID)" "200"
check "ORG  LIST   " "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/app/organizations)" "200"
check "ORG  PUT    " "$(curl -s -o /dev/null -w '%{http_code}' -X PUT $BASE/api/app/organizations/$ORG_ID -H "$H" -H 'Content-Type: application/json' -d '{"name":"Acme v2","description":"upd"}')" "200"
check "ORG  DELETE " "$(curl -s -o /dev/null -w '%{http_code}' -X DELETE $BASE/api/app/organizations/$ORG_ID -H "$H")" "204"

# ============ CATALOG: DOMAIN ============
DOM=$(curl -s -X POST $BASE/api/domains -H "$H" -H "Content-Type: application/json" \
  -d '{"name":"Finance","description":"fin"}')
DOM_ID=$(echo "$DOM" | sed -nE 's/.*"id":"([^"]+)".*/\1/p')
if [ -n "$DOM_ID" ]; then
  RESULTS+=("PASS  DOM  CREATE id=$DOM_ID")
  PASS=$((PASS+1))
else
  RESULTS+=("FAIL  DOM  CREATE body=$DOM")
  FAIL=$((FAIL+1))
fi
check "DOM  GET    " "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/domains/$DOM_ID)" "200"
check "DOM  LIST   " "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/domains)" "200"
check "DOM  PUT    " "$(curl -s -o /dev/null -w '%{http_code}' -X PUT $BASE/api/domains/$DOM_ID -H "$H" -H 'Content-Type: application/json' -d '{"name":"Finance v2","description":"upd"}')" "200"

# ============ CATALOG: TAG ============
TAG=$(curl -s -X POST $BASE/api/tags -H "$H" -H "Content-Type: application/json" \
  -d '{"name":"prod","description":"production"}')
TAG_ID=$(echo "$TAG" | sed -nE 's/.*"id":"([^"]+)".*/\1/p')
if [ -n "$TAG_ID" ]; then
  RESULTS+=("PASS  TAG  CREATE id=$TAG_ID")
  PASS=$((PASS+1))
else
  RESULTS+=("FAIL  TAG  CREATE body=$TAG")
  FAIL=$((FAIL+1))
fi
check "TAG  GET    " "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/tags/$TAG_ID)" "200"
check "TAG  PUT    " "$(curl -s -o /dev/null -w '%{http_code}' -X PUT $BASE/api/tags/$TAG_ID -H "$H" -H 'Content-Type: application/json' -d '{"name":"prod","description":"updated"}')" "200"

# ============ METASOURCE ============
# Unique name per run to avoid "name already exists" from leftover data of prior runs.
SRC_NAME="Local PG $(date +%s)"
SRC_BODY="{\"name\":\"$SRC_NAME\",\"sourceType\":\"DATABASE\",\"connectorType\":\"POSTGRES\",\"description\":\"test\",\"enabled\":true,\"organizationId\":null,\"domainId\":null,\"connectorConfig\":\"{\\\"host\\\":\\\"localhost\\\",\\\"port\\\":5432,\\\"database\\\":\\\"db_react_springboot\\\",\\\"username\\\":\\\"postgres\\\",\\\"password\\\":\\\"123456\\\",\\\"schema\\\":\\\"public\\\"}\"}"
SRC=$(curl -s -X POST $BASE/api/meta-sources -H "$H" -H "Content-Type: application/json" -d "$SRC_BODY")
SRC_ID=$(echo "$SRC" | sed -nE 's/.*"id":"([^"]+)".*/\1/p')
SRC_CODE=$(echo "$SRC" | sed -nE 's/.*"code":"([^"]+)".*/\1/p')
if [ -n "$SRC_ID" ]; then
  RESULTS+=("PASS  SRC  CREATE id=$SRC_ID code=$SRC_CODE")
  PASS=$((PASS+1))
else
  RESULTS+=("FAIL  SRC  CREATE body=$SRC")
  FAIL=$((FAIL+1))
fi
check "SRC  GET    " "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/meta-sources/$SRC_ID)" "200"
check "SRC  LIST   " "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/meta-sources)" "200"
check "SRC  by-code" "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/meta-sources/by-code/$SRC_CODE)" "200"
check "SRC  schema " "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/meta-sources/$SRC_ID/schema)" "200"

# ============ METASYNC: trigger via source ============
SYNC_RESULT=$(curl -s -X POST $BASE/api/meta-sources/$SRC_ID/sync -H "$H")
SYNC_CREATED=$(echo "$SYNC_RESULT" | sed -nE 's/.*"created":([0-9]+).*/\1/p')
if [ -n "$SYNC_CREATED" ]; then
  RESULTS+=("PASS  SYNC initSync created=$SYNC_CREATED")
  PASS=$((PASS+1))
else
  RESULTS+=("FAIL  SYNC initSync body=$(echo $SYNC_RESULT | head -c 200)")
  FAIL=$((FAIL+1))
fi
check "SYNC LIST   " "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/meta-syncs)" "200"
check "SYNC by-src " "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/meta-sources/$SRC_ID/meta-syncs)" "200"

# ============ METASET ============
MS_BODY="{\"name\":\"Customer Set $RUN_TAG\",\"metaCode\":\"customer_$RUN_TAG\",\"description\":\"test\",\"metaSourceId\":\"$SRC_ID\",\"organizationId\":null,\"domainId\":null,\"classification\":\"INTERNAL\",\"tier\":\"BRONZE\",\"tagIds\":[],\"fieldData\":null,\"exampleData\":null,\"endpointPath\":null}"
MS=$(curl -s -X POST $BASE/api/meta-sets -H "$H" -H "Content-Type: application/json" -d "$MS_BODY")
MS_ID=$(echo "$MS" | sed -nE 's/.*"id":"([^"]+)".*/\1/p')
if [ -n "$MS_ID" ]; then
  RESULTS+=("PASS  MS   CREATE id=$MS_ID")
  PASS=$((PASS+1))
else
  RESULTS+=("FAIL  MS   CREATE body=$(echo $MS | head -c 300)")
  FAIL=$((FAIL+1))
fi
check "MS   GET    " "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/meta-sets/$MS_ID)" "200"
check "MS   LIST   " "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/meta-sets)" "200"
check "MS   by-src " "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/meta-sets/by-source/$SRC_ID)" "200"
MS_PUT_BODY="{\"name\":\"Customer v2 $RUN_TAG\",\"metaCode\":\"customer_$RUN_TAG\",\"description\":\"upd\",\"metaSourceId\":\"$SRC_ID\",\"tagIds\":[]}"
check "MS   PUT    " "$(curl -s -o /dev/null -w '%{http_code}' -X PUT $BASE/api/meta-sets/$MS_ID -H "$H" -H 'Content-Type: application/json' -d "$MS_PUT_BODY")" "200"
check "MS   publish" "$(curl -s -o /dev/null -w '%{http_code}' -X POST $BASE/api/meta-sets/$MS_ID/publish -H "$H" -H 'Content-Type: application/json' -d '{"actor":"admin","comment":"go"}')" "200"
check "MS   discont" "$(curl -s -o /dev/null -w '%{http_code}' -X POST $BASE/api/meta-sets/$MS_ID/discontinue -H "$H" -H 'Content-Type: application/json' -d '{"actor":"admin","comment":"end"}')" "200"
check "MS   DELETE " "$(curl -s -o /dev/null -w '%{http_code}' -X DELETE $BASE/api/meta-sets/$MS_ID -H "$H")" "204"

# ============ METAPACK ============
PACK_BODY='{"name":"Customer Pack","description":"public","maxRequestsPerMinute":60,"maxRequestsPerDay":1000}'
PACK=$(curl -s -X POST $BASE/api/v1/metapacks -H "$H" -H "Content-Type: application/json" -d "$PACK_BODY")
PACK_ID=$(echo "$PACK" | sed -nE 's/.*"id":"([^"]+)".*/\1/p')
if [ -n "$PACK_ID" ]; then
  RESULTS+=("PASS  PACK CREATE id=$PACK_ID")
  PASS=$((PASS+1))
else
  RESULTS+=("FAIL  PACK CREATE body=$(echo $PACK | head -c 200)")
  FAIL=$((FAIL+1))
fi
check "PACK GET    " "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/v1/metapacks/$PACK_ID)" "200"
check "PACK LIST   " "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/v1/metapacks)" "200"
check "PACK versn  " "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/v1/metapacks/$PACK_ID/versions)" "200"

REG=$(curl -s -X POST "$BASE/api/v1/metapacks/$PACK_ID/registrations?subscriberName=client-a" -H "$H" -H "Content-Type: application/json" -d '["id","name"]')
# REG response has nested metaPack which itself contains nested objects (currentVersion).
# Cannot strip with simple non-greedy regex in basic sed. Use awk to split on first
# `"id":"` occurrence and take the next 36 chars (UUID format).
REG_ID=$(echo "$REG" | awk -F'"id":"' 'NF>1 {print substr($2, 1, 36)}')
if [ -n "$REG_ID" ]; then
  RESULTS+=("PASS  REG  CREATE id=$REG_ID")
  PASS=$((PASS+1))
else
  RESULTS+=("FAIL  REG  CREATE body=$(echo $REG | head -c 200)")
  FAIL=$((FAIL+1))
fi
check "REG  LIST   " "$(curl -s -o /dev/null -w '%{http_code}' -H "$H" $BASE/api/v1/metapacks/$PACK_ID/registrations)" "200"

APPROVE=$(curl -s -X POST "$BASE/api/v1/metapacks/registrations/$REG_ID/approve?apiSettings=%7B%7D&customLimitPm=100&customLimitPd=1000" -H "$H")
API_KEY=$(echo "$APPROVE" | sed -nE 's/.*"apiKey":"([^"]+)".*/\1/p')
if [ -n "$API_KEY" ]; then
  RESULTS+=("PASS  REG  APPROVE apiKey=${API_KEY:0:20}...")
  PASS=$((PASS+1))
else
  RESULTS+=("FAIL  REG  APPROVE body=$(echo $APPROVE | head -c 200)")
  FAIL=$((FAIL+1))
fi

check "REG  revoke " "$(curl -s -o /dev/null -w '%{http_code}' -X POST $BASE/api/v1/metapacks/registrations/$REG_ID/revoke -H "$H")" "200"

# Pre-DELETE cleanup: starter MetaPack.delete doesn't cascade to registrations or
# to MetaPackVersion (circular FK: pack.current_version_id <-> version.meta_pack_id).
# Null currentVersion, then drop versions + registrations, then DELETE pack.
"$PSQL" -h localhost -U postgres -d db_react_springboot -q -c "
DELETE FROM meta_pack_registration WHERE meta_pack_id = '$PACK_ID';
UPDATE meta_pack SET current_version_id = NULL WHERE id = '$PACK_ID';
DELETE FROM meta_pack_version WHERE meta_pack_id = '$PACK_ID';
" >/dev/null 2>&1
check "PACK DELETE " "$(curl -s -o /dev/null -w '%{http_code}' -X DELETE $BASE/api/v1/metapacks/$PACK_ID -H "$H")" "204"

# ============ Cleanup ============
check "DOM  DELETE " "$(curl -s -o /dev/null -w '%{http_code}' -X DELETE $BASE/api/domains/$DOM_ID -H "$H")" "204"
check "TAG  DELETE " "$(curl -s -o /dev/null -w '%{http_code}' -X DELETE $BASE/api/tags/$TAG_ID -H "$H")" "204"

# Cleanup MetaSync rows blocking MetaSource delete (no cascade in starter).
"$PSQL" -h localhost -U postgres -d db_react_springboot -q -c "DELETE FROM core_meta_sync WHERE data_source_id = '$SRC_ID';" >/dev/null 2>&1
check "SRC  DELETE " "$(curl -s -o /dev/null -w '%{http_code}' -X DELETE $BASE/api/meta-sources/$SRC_ID -H "$H")" "204"

# ============ Auth negative ============
check "NEG  no JWT " "$(curl -s -o /dev/null -w '%{http_code}' $BASE/api/app/organizations)" "401"
check "NEG  bad JWT" "$(curl -s -o /dev/null -w '%{http_code}' -H 'Authorization: Bearer invalid.token' $BASE/api/app/organizations)" "401"

# ============ REPORT ============
echo ""
echo "===================== TEST REPORT ====================="
for r in "${RESULTS[@]}"; do echo "$r"; done
echo "======================================================="
echo "TOTAL: $((PASS+FAIL))   PASS: $PASS   FAIL: $FAIL"
