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
# Exercise the real launch commands without a JVM, server, or network request.
set -euo pipefail

root=$(cd "$(dirname "$0")/../.." && pwd)
temporary=$(mktemp -d)
trap 'rm -rf "$temporary"' EXIT
mkdir -p "$temporary/bin" "$temporary/jdk/bin"

cat > "$temporary/jdk/bin/java" <<'JAVA'
#!/bin/bash
if [[ "$1" == "-version" ]]; then
    printf 'openjdk version "%s.0.1"\n' "$TEST_JAVA_VERSION" >&2
    exit 0
fi
printf '%s\0' "$@" > "$TEST_ARGUMENTS.tmp"
mv "$TEST_ARGUMENTS.tmp" "$TEST_ARGUMENTS"
JAVA

# Background startup checks are outside the argument-construction contract.
cat > "$temporary/bin/curl" <<'CURL'
#!/bin/bash
for arg in "$@"; do
    [[ "$arg" == "--head" ]] && exit 0
done
printf 'UP\n'
CURL
cat > "$temporary/bin/ps" <<'PS'
#!/bin/bash
printf 'java -jar %s.jar\n' "$TEST_SERVICE"
PS
cat > "$temporary/bin/sleep" <<'SLEEP'
#!/bin/bash
# Wait for the Java stub's atomic snapshot without depending on launch timing.
for ((attempt = 0; attempt < 100; attempt++)); do
    [[ -f "$TEST_ARGUMENTS" ]] && exit 0
    /bin/sleep 0.05
done
exit 1
SLEEP
chmod +x "$temporary/jdk/bin/java" "$temporary/bin/"*

assert_argument() {
    local expected="$1" count=0 argument
    while IFS= read -r -d '' argument; do
        if [[ "$argument" == "$expected" ]]; then
            count=$((count + 1))
        fi
    done < "$TEST_ARGUMENTS"
    if [[ "$count" != "1" ]]; then
        printf 'Expected exactly one argument <%s>, found %s\n' "$expected" "$count" >&2
        tr '\0' '\n' < "$TEST_ARGUMENTS" >&2
        return 1
    fi
}

cases=0
for service in apollo-adminservice apollo-configservice apollo-portal; do
    distribution="$temporary/$service"
    mkdir -p "$distribution/scripts"
    # Keep the test usable in a Windows checkout with CRLF conversion enabled.
    sed $'s/\r$//' "$root/$service/src/main/scripts/startup.sh" > "$distribution/scripts/startup.sh"
    for mode in Docker background; do
        for version in 8 9 10 11 17; do
            for directory_name in logs 'log directory'; do
                for log_name in app.log 'my app.log' 'my [app] *.log'; do
                    log_directory="$temporary/$directory_name"
                    application_log="$temporary/$log_name"
                    export TEST_ARGUMENTS="$temporary/arguments"
                    export TEST_SERVICE="$service"
                    export TEST_JAVA_VERSION="$version"
                    rm -f "$TEST_ARGUMENTS"
                    PATH="$temporary/bin:$PATH" JAVA_HOME="$temporary/jdk" \
                        LOG_DIR="$log_directory" APP_LOG="$application_log" \
                        CONSOLE_LOG="$temporary/console.log" APOLLO_RUN_MODE="$mode" \
                        JAVA_OPTS='-Xms64m -Xmx128m -Dcustom.option=preserved' \
                        bash "$distribution/scripts/startup.sh" > "$temporary/startup.log" 2>&1
                    assert_argument "-Dlogging.file.name=$application_log"
                    assert_argument "-XX:HeapDumpPath=$log_directory/HeapDumpOnOutOfMemoryError/"
                    if [[ "$version" == "8" ]]; then
                        assert_argument "-Xloggc:$log_directory/$service.gc.log"
                    else
                        assert_argument "-Xlog:gc*:$log_directory/$service.gc.log:time,level,tags"
                    fi
                    assert_argument '-Xms64m'
                    assert_argument '-Xmx128m'
                    assert_argument '-Dcustom.option=preserved'
                    assert_argument '-jar'
                    assert_argument "$service.jar"
                    cases=$((cases + 1))
                done
            done
        done
    done
done
printf 'Passed %s startup argument cases\n' "$cases"
