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

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

source "${SCRIPT_DIR}/../../scripts/apollo-smoke-lib.sh"

DISCOVERY_PROVIDER="${DISCOVERY_PROVIDER:?DISCOVERY_PROVIDER is required}"
PROVIDER_HOST="${PROVIDER_HOST:?PROVIDER_HOST is required}"
PROVIDER_PORT="${PROVIDER_PORT:?PROVIDER_PORT is required}"
PROVIDER_CONTAINER_NAME="${PROVIDER_CONTAINER_NAME:?PROVIDER_CONTAINER_NAME is required}"
ARTIFACT_DIR="${ARTIFACT_DIR:-/tmp/external-discovery-smoke/${DISCOVERY_PROVIDER}}"

mkdir -p "${ARTIFACT_DIR}"

fetch_meta_service_response() {
  local service_path="$1"
  local output_file="$2"
  apollo_fetch_service_registration "${service_path}" >"${output_file}" 2>&1 || true
}

capture_discovery_artifacts() {
  fetch_meta_service_response "admin" "${ARTIFACT_DIR}/configservice-services-admin.json"
  fetch_meta_service_response "config" "${ARTIFACT_DIR}/configservice-services-config.json"
  "${SCRIPT_DIR}/provider.sh" assert-service "${DISCOVERY_PROVIDER}" "apollo-adminservice" \
    "${ARTIFACT_DIR}/provider-apollo-adminservice.txt" >/dev/null 2>&1 || true
  "${SCRIPT_DIR}/provider.sh" assert-service "${DISCOVERY_PROVIDER}" "apollo-configservice" \
    "${ARTIFACT_DIR}/provider-apollo-configservice.txt" >/dev/null 2>&1 || true
  "${SCRIPT_DIR}/provider.sh" logs "${DISCOVERY_PROVIDER}" \
    "${ARTIFACT_DIR}/provider.log" >/dev/null 2>&1 || true
}

wait_for_provider_service_registration() {
  local service_id="$1"
  local output_file="$2"
  local deadline

  apollo_smoke_init_env
  deadline=$((SECONDS + WAIT_TIMEOUT_SECONDS))

  while (( SECONDS < deadline )); do
    if "${SCRIPT_DIR}/provider.sh" assert-service "${DISCOVERY_PROVIDER}" "${service_id}" \
      "${output_file}" >/dev/null 2>&1; then
      return 0
    fi
    sleep 3
  done

  echo "Timed out waiting for ${DISCOVERY_PROVIDER} to expose ${service_id}" >&2
  "${SCRIPT_DIR}/provider.sh" assert-service "${DISCOVERY_PROVIDER}" "${service_id}" \
    "${output_file}" >/dev/null 2>&1 || true
  return 1
}

trap capture_discovery_artifacts EXIT

wait_for_apollo_discovery_path

if ! apollo_has_admin_service_registration; then
  echo "Meta service does not expose apollo-adminservice via /services/admin" >&2
  exit 1
fi

if ! apollo_has_config_service_registration; then
  echo "Meta service does not expose apollo-configservice via /services/config" >&2
  exit 1
fi

wait_for_provider_service_registration "apollo-adminservice" \
  "${ARTIFACT_DIR}/provider-apollo-adminservice.txt"
wait_for_provider_service_registration "apollo-configservice" \
  "${ARTIFACT_DIR}/provider-apollo-configservice.txt"

echo "External discovery smoke passed for ${DISCOVERY_PROVIDER}"
