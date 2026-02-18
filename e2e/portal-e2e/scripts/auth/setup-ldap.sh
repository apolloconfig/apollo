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

LDAP_CONTAINER_NAME="${LDAP_CONTAINER_NAME:-apollo-e2e-ldap}"
LDAP_PORT="${LDAP_PORT:-3389}"
LDAP_DOMAIN="${LDAP_DOMAIN:-example.org}"
LDAP_ORGANISATION="${LDAP_ORGANISATION:-Apollo E2E Org}"
LDAP_ADMIN_PASSWORD="${LDAP_ADMIN_PASSWORD:-admin}"

LDAP_ALLOWED_USER="${LDAP_ALLOWED_USER:-apollo}"
LDAP_ALLOWED_USER_PASSWORD="${LDAP_ALLOWED_USER_PASSWORD:-admin}"
LDAP_ALLOWED_USER_DISPLAY_NAME="${LDAP_ALLOWED_USER_DISPLAY_NAME:-Apollo Admin}"
LDAP_ALLOWED_USER_EMAIL="${LDAP_ALLOWED_USER_EMAIL:-apollo@example.org}"

LDAP_ALLOWED_USER_SECONDARY="${LDAP_ALLOWED_USER_SECONDARY:-devops1}"
LDAP_ALLOWED_USER_SECONDARY_PASSWORD="${LDAP_ALLOWED_USER_SECONDARY_PASSWORD:-admin}"
LDAP_ALLOWED_USER_SECONDARY_DISPLAY_NAME="${LDAP_ALLOWED_USER_SECONDARY_DISPLAY_NAME:-Dev Ops One}"
LDAP_ALLOWED_USER_SECONDARY_EMAIL="${LDAP_ALLOWED_USER_SECONDARY_EMAIL:-devops1@example.org}"

LDAP_BLOCKED_USER="${LDAP_BLOCKED_USER:-blocked1}"
LDAP_BLOCKED_USER_PASSWORD="${LDAP_BLOCKED_USER_PASSWORD:-admin}"
LDAP_BLOCKED_USER_DISPLAY_NAME="${LDAP_BLOCKED_USER_DISPLAY_NAME:-Blocked User}"
LDAP_BLOCKED_USER_EMAIL="${LDAP_BLOCKED_USER_EMAIL:-blocked1@example.org}"

LDAP_BASE_DN="dc=example,dc=org"

echo "[setup-ldap] Preparing container ${LDAP_CONTAINER_NAME}"
docker rm -f "${LDAP_CONTAINER_NAME}" >/dev/null 2>&1 || true

docker run --name "${LDAP_CONTAINER_NAME}" \
  --detach \
  --publish "${LDAP_PORT}:389" \
  --env "LDAP_ORGANISATION=${LDAP_ORGANISATION}" \
  --env "LDAP_DOMAIN=${LDAP_DOMAIN}" \
  --env "LDAP_ADMIN_PASSWORD=${LDAP_ADMIN_PASSWORD}" \
  osixia/openldap:1.5.0 >/dev/null

echo "[setup-ldap] Waiting for OpenLDAP to be ready"
for _ in $(seq 1 60); do
  if docker exec "${LDAP_CONTAINER_NAME}" ldapsearch \
    -x \
    -H ldap://localhost:389 \
    -D "cn=admin,${LDAP_BASE_DN}" \
    -w "${LDAP_ADMIN_PASSWORD}" \
    -b "${LDAP_BASE_DN}" \
    "(objectClass=*)" dn >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

tmp_ldif="$(mktemp)"
cleanup() {
  rm -f "${tmp_ldif}"
}
trap cleanup EXIT

cat > "${tmp_ldif}" <<LDIF
dn: ou=people,${LDAP_BASE_DN}
objectClass: organizationalUnit
ou: people

dn: ou=group,${LDAP_BASE_DN}
objectClass: organizationalUnit
ou: group

dn: uid=${LDAP_ALLOWED_USER},ou=people,${LDAP_BASE_DN}
objectClass: inetOrgPerson
objectClass: organizationalPerson
objectClass: person
objectClass: top
cn: ${LDAP_ALLOWED_USER_DISPLAY_NAME}
sn: Allowed
uid: ${LDAP_ALLOWED_USER}
mail: ${LDAP_ALLOWED_USER_EMAIL}
userPassword: ${LDAP_ALLOWED_USER_PASSWORD}

dn: uid=${LDAP_ALLOWED_USER_SECONDARY},ou=people,${LDAP_BASE_DN}
objectClass: inetOrgPerson
objectClass: organizationalPerson
objectClass: person
objectClass: top
cn: ${LDAP_ALLOWED_USER_SECONDARY_DISPLAY_NAME}
sn: Secondary
uid: ${LDAP_ALLOWED_USER_SECONDARY}
mail: ${LDAP_ALLOWED_USER_SECONDARY_EMAIL}
userPassword: ${LDAP_ALLOWED_USER_SECONDARY_PASSWORD}

dn: uid=${LDAP_BLOCKED_USER},ou=people,${LDAP_BASE_DN}
objectClass: inetOrgPerson
objectClass: organizationalPerson
objectClass: person
objectClass: top
cn: ${LDAP_BLOCKED_USER_DISPLAY_NAME}
sn: Blocked
uid: ${LDAP_BLOCKED_USER}
mail: ${LDAP_BLOCKED_USER_EMAIL}
userPassword: ${LDAP_BLOCKED_USER_PASSWORD}

dn: cn=dev,ou=group,${LDAP_BASE_DN}
objectClass: posixGroup
cn: dev
gidNumber: 5000
memberUid: ${LDAP_ALLOWED_USER}
memberUid: ${LDAP_ALLOWED_USER_SECONDARY}
LDIF

docker cp "${tmp_ldif}" "${LDAP_CONTAINER_NAME}:/tmp/apollo-e2e-init.ldif"
docker exec "${LDAP_CONTAINER_NAME}" ldapadd \
  -x \
  -D "cn=admin,${LDAP_BASE_DN}" \
  -w "${LDAP_ADMIN_PASSWORD}" \
  -f /tmp/apollo-e2e-init.ldif >/dev/null

echo "[setup-ldap] LDAP fixtures imported"
docker exec "${LDAP_CONTAINER_NAME}" ldapsearch \
  -x \
  -H ldap://localhost:389 \
  -D "cn=admin,${LDAP_BASE_DN}" \
  -w "${LDAP_ADMIN_PASSWORD}" \
  -b "ou=group,${LDAP_BASE_DN}" \
  "(cn=dev)" memberUid >/dev/null

echo "[setup-ldap] Done"
