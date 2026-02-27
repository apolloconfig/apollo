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

KEYCLOAK_CONTAINER_NAME="${KEYCLOAK_CONTAINER_NAME:-apollo-e2e-keycloak}"
KEYCLOAK_PORT="${KEYCLOAK_PORT:-9080}"
KEYCLOAK_IMAGE="${KEYCLOAK_IMAGE:-quay.io/keycloak/keycloak:26.1.0}"
KEYCLOAK_ADMIN_USER="${KEYCLOAK_ADMIN_USER:-admin}"
KEYCLOAK_ADMIN_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD:-admin}"

OIDC_REALM="${OIDC_REALM:-apollo}"
OIDC_USERNAME="${OIDC_USERNAME:-apollo}"
OIDC_PASSWORD="${OIDC_PASSWORD:-admin}"
OIDC_PRIMARY_EMAIL="${OIDC_PRIMARY_EMAIL:-apollo@example.org}"
OIDC_PRIMARY_FIRST_NAME="${OIDC_PRIMARY_FIRST_NAME:-Apollo}"
OIDC_PRIMARY_LAST_NAME="${OIDC_PRIMARY_LAST_NAME:-Admin}"

OIDC_SECONDARY_USERNAME="${OIDC_SECONDARY_USERNAME:-oidcdev1}"
OIDC_SECONDARY_PASSWORD="${OIDC_SECONDARY_PASSWORD:-admin}"
OIDC_SECONDARY_EMAIL="${OIDC_SECONDARY_EMAIL:-oidcdev1@example.org}"
OIDC_SECONDARY_FIRST_NAME="${OIDC_SECONDARY_FIRST_NAME:-Oidc}"
OIDC_SECONDARY_LAST_NAME="${OIDC_SECONDARY_LAST_NAME:-DevOne}"

OIDC_CLIENT_ID="${OIDC_CLIENT_ID:-apollo-portal}"
OIDC_CLIENT_SECRET="${OIDC_CLIENT_SECRET:-apollo-secret}"
PORTAL_BASE_URL="${PORTAL_BASE_URL:-http://127.0.0.1:8070}"
ALT_PORTAL_BASE_URL="${ALT_PORTAL_BASE_URL:-http://localhost:8070}"

KEYCLOAK_URL="http://127.0.0.1:${KEYCLOAK_PORT}"

echo "[setup-oidc] Preparing container ${KEYCLOAK_CONTAINER_NAME}"
docker rm -f "${KEYCLOAK_CONTAINER_NAME}" >/dev/null 2>&1 || true

docker run --name "${KEYCLOAK_CONTAINER_NAME}" \
  --detach \
  --publish "${KEYCLOAK_PORT}:8080" \
  --env "KEYCLOAK_ADMIN=${KEYCLOAK_ADMIN_USER}" \
  --env "KEYCLOAK_ADMIN_PASSWORD=${KEYCLOAK_ADMIN_PASSWORD}" \
  "${KEYCLOAK_IMAGE}" \
  start-dev >/dev/null

echo "[setup-oidc] Waiting for Keycloak master realm"
for _ in $(seq 1 120); do
  if curl -fsS "${KEYCLOAK_URL}/realms/master/.well-known/openid-configuration" >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

docker exec "${KEYCLOAK_CONTAINER_NAME}" /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user "${KEYCLOAK_ADMIN_USER}" \
  --password "${KEYCLOAK_ADMIN_PASSWORD}" >/dev/null

docker exec "${KEYCLOAK_CONTAINER_NAME}" /opt/keycloak/bin/kcadm.sh create realms \
  -s "realm=${OIDC_REALM}" \
  -s enabled=true >/dev/null

create_user() {
  local username="$1"
  local password="$2"
  local email="$3"
  local first_name="$4"
  local last_name="$5"

  local user_id
  user_id="$(docker exec "${KEYCLOAK_CONTAINER_NAME}" /opt/keycloak/bin/kcadm.sh create users \
    -r "${OIDC_REALM}" \
    -s "username=${username}" \
    -s enabled=true \
    -s "email=${email}" \
    -s "firstName=${first_name}" \
    -s "lastName=${last_name}" \
    -s emailVerified=true \
    -i | tr -d '\r\n')"

  docker exec "${KEYCLOAK_CONTAINER_NAME}" /opt/keycloak/bin/kcadm.sh set-password \
    -r "${OIDC_REALM}" \
    --userid "${user_id}" \
    --new-password "${password}" >/dev/null

  docker exec "${KEYCLOAK_CONTAINER_NAME}" /opt/keycloak/bin/kcadm.sh update "users/${user_id}" \
    -r "${OIDC_REALM}" \
    -s 'requiredActions=[]' >/dev/null
}

create_user "${OIDC_USERNAME}" "${OIDC_PASSWORD}" "${OIDC_PRIMARY_EMAIL}" \
  "${OIDC_PRIMARY_FIRST_NAME}" "${OIDC_PRIMARY_LAST_NAME}"
create_user "${OIDC_SECONDARY_USERNAME}" "${OIDC_SECONDARY_PASSWORD}" "${OIDC_SECONDARY_EMAIL}" \
  "${OIDC_SECONDARY_FIRST_NAME}" "${OIDC_SECONDARY_LAST_NAME}"

client_id="$(docker exec "${KEYCLOAK_CONTAINER_NAME}" /opt/keycloak/bin/kcadm.sh create clients \
  -r "${OIDC_REALM}" \
  -s "clientId=${OIDC_CLIENT_ID}" \
  -s enabled=true \
  -s protocol=openid-connect \
  -s publicClient=false \
  -s "secret=${OIDC_CLIENT_SECRET}" \
  -s standardFlowEnabled=true \
  -s directAccessGrantsEnabled=true \
  -i | tr -d '\r\n')"

primary_redirect="${PORTAL_BASE_URL%/}/login/oauth2/code/*"
secondary_redirect="${ALT_PORTAL_BASE_URL%/}/login/oauth2/code/*"

primary_origin="${PORTAL_BASE_URL%/}"
secondary_origin="${ALT_PORTAL_BASE_URL%/}"

docker exec "${KEYCLOAK_CONTAINER_NAME}" /opt/keycloak/bin/kcadm.sh update "clients/${client_id}" \
  -r "${OIDC_REALM}" \
  -s "redirectUris=[\"${primary_redirect}\",\"${secondary_redirect}\"]" \
  -s "webOrigins=[\"${primary_origin}\",\"${secondary_origin}\"]" >/dev/null

echo "[setup-oidc] Keycloak fixtures imported"
curl -fsS "${KEYCLOAK_URL}/realms/${OIDC_REALM}/.well-known/openid-configuration" >/dev/null

echo "[setup-oidc] Done"
