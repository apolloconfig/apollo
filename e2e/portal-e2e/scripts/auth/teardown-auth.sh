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
KEYCLOAK_CONTAINER_NAME="${KEYCLOAK_CONTAINER_NAME:-apollo-e2e-keycloak}"

docker rm -f "${LDAP_CONTAINER_NAME}" >/dev/null 2>&1 || true
docker rm -f "${KEYCLOAK_CONTAINER_NAME}" >/dev/null 2>&1 || true

echo "[teardown-auth] Containers cleaned"
