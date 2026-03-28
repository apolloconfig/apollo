#!/bin/bash
#
# Copyright 2024 Apollo Authors
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
SERVICE_NAME=apollo-portal
export APP_NAME=$SERVICE_NAME
PID_FILE="$APP_NAME/$APP_NAME.pid"
EXPECTED_PATTERN="${SERVICE_NAME}(-[^[:space:]]+)?\\.jar"

cd `dirname $0`/..

if [[ ! -f $SERVICE_NAME".jar" && -d current ]]; then
    cd current
fi

matches_service_process() {
  local pid="$1"
  local command_line

  command_line="$(ps -p "$pid" -o command= 2>/dev/null || true)"
  [[ -n "$command_line" ]] && [[ "$command_line" =~ $EXPECTED_PATTERN ]]
}

wait_for_pid_exit() {
  local pid="$1"
  local timeout="${2:-30}"
  local i

  for (( i=0; i<timeout; i++ )); do
    if ! matches_service_process "$pid"; then
      return 0
    fi
    sleep 1
  done

  return 1
}

if [[ -f "$PID_FILE" ]]; then
  read -r pid < "$PID_FILE"
  if [[ -n "$pid" ]] && matches_service_process "$pid"; then
    if ! kill "$pid" 2>/dev/null; then
      echo "Failed to send SIGTERM to process $pid for $SERVICE_NAME" >&2
      exit 1
    fi
    if wait_for_pid_exit "$pid"; then
      rm -f "$PID_FILE"
      exit 0
    fi
    echo "Timed out waiting for process $pid ($SERVICE_NAME) to exit" >&2
    exit 1
  fi
fi

rm -f "$PID_FILE"
mapfile -t pids < <(pgrep -f "$EXPECTED_PATTERN" || true)
if (( ${#pids[@]} > 0 )); then
  if ! kill "${pids[@]}" 2>/dev/null; then
    echo "Failed to send SIGTERM to one or more $SERVICE_NAME processes" >&2
    exit 1
  fi
  for pid in "${pids[@]}"; do
    if ! wait_for_pid_exit "$pid"; then
      echo "Timed out waiting for process $pid ($SERVICE_NAME) to exit" >&2
      exit 1
    fi
  done
fi
