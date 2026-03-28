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

usage() {
  cat <<EOF
Usage:
  provider.sh start <provider> <env-file>
  provider.sh assert-service <provider> <service-id> <output-file>
  provider.sh logs <provider> <output-file>
  provider.sh stop <provider>
EOF
}

ensure_provider() {
  if [[ $# -lt 1 ]] || [[ -z "$1" ]]; then
    usage >&2
    exit 1
  fi
}

load_provider_metadata() {
  local provider="$1"

  PROVIDER_HOST="127.0.0.1"
  PROVIDER_WAIT_TIMEOUT_SECONDS="${PROVIDER_WAIT_TIMEOUT_SECONDS:-180}"
  RUN_SUFFIX="${GITHUB_RUN_ID:-local}-${GITHUB_RUN_ATTEMPT:-0}"

  case "$provider" in
    nacos)
      PROVIDER_PORT="8848"
      PROVIDER_GRPC_PORT="9848"
      PROVIDER_RAFT_PORT="9849"
      PROVIDER_CONTAINER_NAME="apollo-e2e-nacos-${RUN_SUFFIX}"
      ;;
    consul)
      PROVIDER_PORT="8500"
      PROVIDER_CONTAINER_NAME="apollo-e2e-consul-${RUN_SUFFIX}"
      ;;
    zookeeper)
      PROVIDER_PORT="2181"
      PROVIDER_CONTAINER_NAME="apollo-e2e-zookeeper-${RUN_SUFFIX}"
      ;;
    *)
      echo "Unsupported discovery provider: ${provider}" >&2
      exit 1
      ;;
  esac
}

write_env_file() {
  local env_file="$1"

  cat >"${env_file}" <<EOF
DISCOVERY_PROVIDER=${DISCOVERY_PROVIDER}
PROVIDER_HOST=${PROVIDER_HOST}
PROVIDER_PORT=${PROVIDER_PORT}
PROVIDER_CONTAINER_NAME=${PROVIDER_CONTAINER_NAME}
SPRING_CLOUD_DISCOVERY_ENABLED=true
EOF

  case "${DISCOVERY_PROVIDER}" in
    nacos)
      cat >>"${env_file}" <<EOF
SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR=${PROVIDER_HOST}:${PROVIDER_PORT}
SPRING_CLOUD_NACOS_SERVER_ADDR=${PROVIDER_HOST}:${PROVIDER_PORT}
SPRING_CLOUD_NACOS_DISCOVERY_FAIL_FAST=false
EOF
      ;;
    consul)
      cat >>"${env_file}" <<EOF
SPRING_CLOUD_NACOS_DISCOVERY_ENABLED=false
SPRING_CLOUD_NACOS_DISCOVERY_REGISTER_ENABLED=false
SPRING_CLOUD_CONSUL_ENABLED=true
SPRING_CLOUD_CONSUL_HOST=${PROVIDER_HOST}
SPRING_CLOUD_CONSUL_PORT=${PROVIDER_PORT}
SPRING_CLOUD_CONSUL_DISCOVERY_ENABLED=true
SPRING_CLOUD_CONSUL_SERVICE_REGISTRY_ENABLED=true
EOF
      ;;
    zookeeper)
      cat >>"${env_file}" <<EOF
SPRING_CLOUD_NACOS_DISCOVERY_ENABLED=false
SPRING_CLOUD_NACOS_DISCOVERY_REGISTER_ENABLED=false
SPRING_CLOUD_ZOOKEEPER_ENABLED=true
SPRING_CLOUD_ZOOKEEPER_CONNECT_STRING=${PROVIDER_HOST}:${PROVIDER_PORT}
SPRING_CLOUD_ZOOKEEPER_DISCOVERY_ENABLED=true
SPRING_CLOUD_ZOOKEEPER_DISCOVERY_REGISTER=true
EOF
      ;;
  esac
}

start_container() {
  local nacos_auth_token

  docker rm -f "${PROVIDER_CONTAINER_NAME}" >/dev/null 2>&1 || true

  case "${DISCOVERY_PROVIDER}" in
    nacos)
      # Nacos 3.1.1-slim still validates these envs at startup even when auth is disabled.
      nacos_auth_token="$(printf '%s' "${NACOS_AUTH_TOKEN_SEED:-0123456789abcdef0123456789abcdef}" | base64)"
      docker run -d \
        --name "${PROVIDER_CONTAINER_NAME}" \
        -p "127.0.0.1:${PROVIDER_PORT}:8848" \
        -p "127.0.0.1:${PROVIDER_GRPC_PORT}:9848" \
        -p "127.0.0.1:${PROVIDER_RAFT_PORT}:9849" \
        -e MODE=standalone \
        -e NACOS_AUTH_ENABLE=false \
        -e NACOS_AUTH_TOKEN="${nacos_auth_token}" \
        -e NACOS_AUTH_IDENTITY_KEY="${NACOS_AUTH_IDENTITY_KEY:-serverIdentity}" \
        -e NACOS_AUTH_IDENTITY_VALUE="${NACOS_AUTH_IDENTITY_VALUE:-security}" \
        nacos/nacos-server:v3.1.1-slim >/dev/null
      ;;
    consul)
      docker run -d \
        --name "${PROVIDER_CONTAINER_NAME}" \
        -p "127.0.0.1:${PROVIDER_PORT}:8500" \
        hashicorp/consul:1.22 \
        agent -dev -client=0.0.0.0 >/dev/null
      ;;
    zookeeper)
      docker run -d \
        --name "${PROVIDER_CONTAINER_NAME}" \
        -p "127.0.0.1:${PROVIDER_PORT}:2181" \
        -e ZOO_4LW_COMMANDS_WHITELIST=ruok,stat,conf,isro,mntr \
        zookeeper:3.9.4-jre-17 >/dev/null
      ;;
  esac
}

provider_ready_nacos() {
  local code
  code="$(curl -sS -o /dev/null -w '%{http_code}' \
    "http://${PROVIDER_HOST}:${PROVIDER_PORT}/nacos/v1/ns/instance/list?serviceName=bootstrap-check" \
    2>/dev/null || echo "000")"
  [[ "${code}" == "200" ]]
}

provider_ready_consul() {
  local leader
  leader="$(curl -fsS "http://${PROVIDER_HOST}:${PROVIDER_PORT}/v1/status/leader" 2>/dev/null || true)"
  [[ -n "${leader}" ]] && [[ "${leader}" != "\"\"" ]]
}

provider_ready_zookeeper() {
  local response=""

  exec 3<>"/dev/tcp/${PROVIDER_HOST}/${PROVIDER_PORT}" || return 1
  printf 'ruok' >&3
  IFS= read -r -t 2 response <&3 || true
  exec 3>&-
  exec 3<&-

  [[ "${response}" == "imok" ]]
}

wait_for_provider_ready() {
  local deadline

  deadline=$((SECONDS + PROVIDER_WAIT_TIMEOUT_SECONDS))
  while (( SECONDS < deadline )); do
    case "${DISCOVERY_PROVIDER}" in
      nacos)
        provider_ready_nacos && return 0
        ;;
      consul)
        provider_ready_consul && return 0
        ;;
      zookeeper)
        provider_ready_zookeeper && return 0
        ;;
    esac
    sleep 3
  done

  echo "Timed out waiting for ${DISCOVERY_PROVIDER} readiness" >&2
  return 1
}

query_service_registration() {
  local service_id="$1"

  case "${DISCOVERY_PROVIDER}" in
    nacos)
      curl -fsS \
        "http://${PROVIDER_HOST}:${PROVIDER_PORT}/nacos/v1/ns/instance/list?serviceName=${service_id}" \
        2>&1
      ;;
    consul)
      curl -fsS \
        "http://${PROVIDER_HOST}:${PROVIDER_PORT}/v1/health/service/${service_id}?passing=true" \
        2>&1
      ;;
    zookeeper)
      docker exec "${PROVIDER_CONTAINER_NAME}" \
        zkCli.sh -server "127.0.0.1:${PROVIDER_PORT}" ls "/services/${service_id}" 2>&1
      ;;
  esac
}

assert_query_result() {
  local service_id="$1"
  local query_result_file="$2"

  case "${DISCOVERY_PROVIDER}" in
    nacos)
      grep -q "${service_id}" "${query_result_file}" && ! grep -q '"hosts":\[\]' "${query_result_file}"
      ;;
    consul)
      grep -q "${service_id}" "${query_result_file}" && ! grep -q '^\[\]$' "${query_result_file}"
      ;;
    zookeeper)
      ! grep -q 'Node does not exist' "${query_result_file}" \
        && ! grep -q '\[\]' "${query_result_file}"
      ;;
  esac
}

log_provider() {
  local output_file="$1"
  docker logs "${PROVIDER_CONTAINER_NAME}" >"${output_file}" 2>&1 || true
}

stop_provider() {
  docker rm -f "${PROVIDER_CONTAINER_NAME}" >/dev/null 2>&1 || true
}

main() {
  local command provider

  if [[ $# -lt 2 ]]; then
    usage >&2
    exit 1
  fi

  command="$1"
  provider="$2"
  ensure_provider "${provider}"

  DISCOVERY_PROVIDER="${provider}"
  load_provider_metadata "${provider}"

  case "${command}" in
    start)
      if [[ $# -ne 3 ]]; then
        usage >&2
        exit 1
      fi
      start_container
      wait_for_provider_ready
      write_env_file "$3"
      ;;
    assert-service)
      if [[ $# -ne 4 ]]; then
        usage >&2
        exit 1
      fi
      query_service_registration "$3" >"$4"
      assert_query_result "$3" "$4"
      ;;
    logs)
      if [[ $# -ne 3 ]]; then
        usage >&2
        exit 1
      fi
      log_provider "$3"
      ;;
    stop)
      stop_provider
      ;;
    *)
      usage >&2
      exit 1
      ;;
  esac
}

main "$@"
