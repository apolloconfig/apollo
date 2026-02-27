#!/usr/bin/env bash
#
# Copyright 2026 Apollo Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -euo pipefail

BASE_HOST="${BASE_HOST:-127.0.0.1}"
PORTAL_URL="${PORTAL_URL:-http://${BASE_HOST}:8070}"
CONFIG_URL="${CONFIG_URL:-http://${BASE_HOST}:8080}"
ADMIN_URL="${ADMIN_URL:-http://${BASE_HOST}:8090}"
WAIT_TIMEOUT_SECONDS="${WAIT_TIMEOUT_SECONDS:-300}"
PORTAL_USERNAME="${PORTAL_USERNAME:-apollo}"
PORTAL_PASSWORD="${PORTAL_PASSWORD:-admin}"
PORTAL_AUTH_MODE="${PORTAL_AUTH_MODE:-auth}"

probe() {
  local url="$1"
  curl -fsS "$url" >/dev/null 2>&1
}

portal_http_status() {
  local url="$1"
  curl -sS -o /dev/null -w '%{http_code}' "$url" 2>/dev/null || echo "000"
}

portal_ready_for_oidc() {
  local code
  code="$(portal_http_status "${PORTAL_URL}/")"
  [[ "$code" == "200" || "$code" == "302" || "$code" == "401" ]]
}

has_admin_service_registration() {
  local services
  services="$(curl -fsS "${CONFIG_URL}/services/admin" 2>/dev/null || true)"
  [[ -n "$services" ]] && [[ "$services" != "[]" ]] && [[ "$services" == *"apollo-adminservice"* ]]
}

warm_up_portal_admin_path() {
  local app_id cookie_file app_payload item_payload release_payload app_status item_status release_status

  cookie_file="$(mktemp)"
  app_id="warmup$(date +%s)$RANDOM"

  curl -fsS -c "$cookie_file" -b "$cookie_file" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -X POST "${PORTAL_URL}/signin" \
    --data-urlencode "username=${PORTAL_USERNAME}" \
    --data-urlencode "password=${PORTAL_PASSWORD}" >/dev/null 2>&1 || {
    rm -f "$cookie_file"
    return 1
  }

  app_payload=$(cat <<JSON
{"appId":"${app_id}","name":"${app_id}","orgId":"TEST1","orgName":"样例部门1","ownerName":"apollo","admins":["apollo"]}
JSON
)

  app_status="$(curl -sS -o /dev/null -w '%{http_code}' \
    -b "$cookie_file" -H 'Content-Type: application/json' -X POST \
    "${PORTAL_URL}/apps" -d "$app_payload" || true)"

  item_payload='{"key":"timeout","value":"100","comment":"warmup","lineNum":1}'
  item_status="$(curl -sS -o /dev/null -w '%{http_code}' \
    -b "$cookie_file" -H 'Content-Type: application/json' -X POST \
    "${PORTAL_URL}/apps/${app_id}/envs/LOCAL/clusters/default/namespaces/application/item" \
    -d "$item_payload" || true)"

  release_payload='{"releaseTitle":"warmup-release","releaseComment":"warmup"}'
  release_status="$(curl -sS -o /dev/null -w '%{http_code}' \
    -b "$cookie_file" -H 'Content-Type: application/json' -X POST \
    "${PORTAL_URL}/apps/${app_id}/envs/LOCAL/clusters/default/namespaces/application/releases" \
    -d "$release_payload" || true)"

  rm -f "$cookie_file"

  [[ "$app_status" == "200" ]] && [[ "$item_status" == "200" ]] && [[ "$release_status" == "200" ]]
}

deadline=$((SECONDS + WAIT_TIMEOUT_SECONDS))
while (( SECONDS < deadline )); do
  if probe "${CONFIG_URL}/health" && probe "${ADMIN_URL}/health"; then
    if has_admin_service_registration; then
      if [[ "${PORTAL_AUTH_MODE}" == "oidc" ]]; then
        if portal_ready_for_oidc; then
          echo "Portal/Admin path is ready (oidc mode)"
          exit 0
        fi
      elif probe "${PORTAL_URL}/signin" && warm_up_portal_admin_path; then
        echo "Portal/Admin path is ready"
        exit 0
      fi
    fi
  fi
  sleep 3
done

echo "Timed out waiting for Apollo Portal readiness"
exit 1
