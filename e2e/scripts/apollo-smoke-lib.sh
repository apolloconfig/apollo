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

apollo_smoke_init_env() {
  BASE_HOST="${BASE_HOST:-127.0.0.1}"
  PORTAL_URL="${PORTAL_URL:-http://${BASE_HOST}:8070}"
  CONFIG_URL="${CONFIG_URL:-http://${BASE_HOST}:8080}"
  ADMIN_URL="${ADMIN_URL:-http://${BASE_HOST}:8090}"
  WAIT_TIMEOUT_SECONDS="${WAIT_TIMEOUT_SECONDS:-300}"
  CURL_CONNECT_TIMEOUT_SECONDS="${CURL_CONNECT_TIMEOUT_SECONDS:-3}"
  CURL_MAX_TIME_SECONDS="${CURL_MAX_TIME_SECONDS:-10}"
  PORTAL_AUTH_MODE="${PORTAL_AUTH_MODE:-auth}"
}

apollo_curl() {
  curl \
    --connect-timeout "${CURL_CONNECT_TIMEOUT_SECONDS:-3}" \
    --max-time "${CURL_MAX_TIME_SECONDS:-10}" \
    "$@"
}

apollo_require_portal_credentials() {
  : "${PORTAL_USERNAME:?PORTAL_USERNAME must be set}"
  : "${PORTAL_PASSWORD:?PORTAL_PASSWORD must be set}"
}

apollo_probe() {
  local url="$1"
  apollo_curl -fsS "$url" >/dev/null 2>&1
}

apollo_portal_http_status() {
  local url="$1"
  apollo_curl -sS -o /dev/null -w '%{http_code}' "$url" 2>/dev/null || echo "000"
}

apollo_portal_ready_for_oidc() {
  local code
  code="$(apollo_portal_http_status "${PORTAL_URL}/")"
  [[ "$code" == "200" || "$code" == "302" || "$code" == "401" ]]
}

apollo_fetch_service_registration() {
  local service_path="$1"
  apollo_curl -fsS "${CONFIG_URL}/services/${service_path}" 2>/dev/null || true
}

apollo_has_service_registration() {
  local service_path="$1"
  local expected_service="$2"
  local response
  response="$(apollo_fetch_service_registration "${service_path}")"
  [[ -n "$response" ]] && [[ "$response" != "[]" ]] && [[ "$response" == *"${expected_service}"* ]]
}

apollo_has_config_service_registration() {
  apollo_has_service_registration "config" "apollo-configservice"
}

apollo_has_admin_service_registration() {
  apollo_has_service_registration "admin" "apollo-adminservice"
}

apollo_discovery_path_ready() {
  apollo_probe "${CONFIG_URL}/health" \
    && apollo_probe "${ADMIN_URL}/health" \
    && apollo_has_config_service_registration \
    && apollo_has_admin_service_registration
}

wait_for_apollo_discovery_path() {
  local deadline

  apollo_smoke_init_env
  deadline=$((SECONDS + WAIT_TIMEOUT_SECONDS))

  while (( SECONDS < deadline )); do
    if apollo_discovery_path_ready; then
      echo "Discovery path is ready"
      return 0
    fi
    sleep 3
  done

  echo "Timed out waiting for Apollo discovery readiness" >&2
  return 1
}

apollo_warm_up_portal_admin_path() {
  local app_id cookie_file app_payload item_payload release_payload
  local app_status item_status release_status

  apollo_require_portal_credentials
  cookie_file="$(mktemp)"
  app_id="warmup$(date +%s)$RANDOM"

  apollo_curl -fsS -c "$cookie_file" -b "$cookie_file" \
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

  app_status="$(apollo_curl -sS -o /dev/null -w '%{http_code}' \
    -b "$cookie_file" -H 'Content-Type: application/json' -X POST \
    "${PORTAL_URL}/apps" -d "$app_payload" || true)"

  item_payload='{"key":"timeout","value":"100","comment":"warmup","lineNum":1}'
  item_status="$(apollo_curl -sS -o /dev/null -w '%{http_code}' \
    -b "$cookie_file" -H 'Content-Type: application/json' -X POST \
    "${PORTAL_URL}/apps/${app_id}/envs/LOCAL/clusters/default/namespaces/application/item" \
    -d "$item_payload" || true)"

  release_payload='{"releaseTitle":"warmup-release","releaseComment":"warmup"}'
  release_status="$(apollo_curl -sS -o /dev/null -w '%{http_code}' \
    -b "$cookie_file" -H 'Content-Type: application/json' -X POST \
    "${PORTAL_URL}/apps/${app_id}/envs/LOCAL/clusters/default/namespaces/application/releases" \
    -d "$release_payload" || true)"

  rm -f "$cookie_file"

  [[ "$app_status" == "200" ]] && [[ "$item_status" == "200" ]] && [[ "$release_status" == "200" ]]
}

wait_for_apollo_portal_admin_path() {
  local deadline

  apollo_smoke_init_env
  deadline=$((SECONDS + WAIT_TIMEOUT_SECONDS))

  while (( SECONDS < deadline )); do
    if apollo_discovery_path_ready; then
      if [[ "${PORTAL_AUTH_MODE}" == "oidc" ]]; then
        if apollo_portal_ready_for_oidc; then
          echo "Portal/Admin path is ready (oidc mode)"
          return 0
        fi
      elif apollo_probe "${PORTAL_URL}/signin" && apollo_warm_up_portal_admin_path; then
        echo "Portal/Admin path is ready"
        return 0
      fi
    fi
    sleep 3
  done

  echo "Timed out waiting for Apollo Portal readiness" >&2
  return 1
}
