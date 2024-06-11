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
## Adjust log dir if necessary
LOG_DIR=/opt/logs
## Adjust server port if necessary
SERVER_PORT=${SERVER_PORT:=8070}

## Create log directory if not existed because JDK 8+ won't do that
mkdir -p $LOG_DIR
# Create directory of -XX:HeapDumpPath
mkdir -p $LOG_DIR/HeapDumpOnOutOfMemoryError/

## Adjust memory settings if necessary
#export JAVA_OPTS="-Xms2560m -Xmx2560m -Xss256k -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=384m -XX:NewSize=1536m -XX:MaxNewSize=1536m -XX:SurvivorRatio=8"

## Only uncomment the following when you are using server jvm
#export JAVA_OPTS="$JAVA_OPTS -server -XX:-ReduceInitialCardMarks"

########### The following is the same for configservice, adminservice, portal ###########
export JAVA_OPTS="$JAVA_OPTS -XX:ParallelGCThreads=4 -XX:MaxTenuringThreshold=9 -XX:+DisableExplicitGC -XX:+ScavengeBeforeFullGC -XX:SoftRefLRUPolicyMSPerMB=0 -XX:+ExplicitGCInvokesConcurrent -XX:+HeapDumpOnOutOfMemoryError -XX:-OmitStackTraceInFastThrow -Duser.timezone=Asia/Shanghai -Dclient.encoding.override=UTF-8 -Dfile.encoding=UTF-8 -Djava.security.egd=file:/dev/./urandom"
# DS_URL, DS_USERNAME, DS_PASSWORD are deprecated, please use SPRING_DATASOURCE_URL, SPRING_DATASOURCE_USERNAME, SPRING_DATASOURCE_PASSWORD instead
# DataSource URL USERNAME PASSWORD
if [ "$DS_URL"x != x ]
then
    export SPRING_DATASOURCE_URL=$DS_URL
    export SPRING_DATASOURCE_USERNAME=$DS_USERNAME
    export SPRING_DATASOURCE_PASSWORD=$DS_PASSWORD
fi
export JAVA_OPTS="$JAVA_OPTS -Dserver.port=$SERVER_PORT -Dlogging.file.name=$LOG_DIR/$SERVICE_NAME.log -XX:HeapDumpPath=$LOG_DIR/HeapDumpOnOutOfMemoryError/"
export APP_NAME=$SERVICE_NAME

PATH_TO_JAR=$SERVICE_NAME".jar"
SERVER_URL="http://localhost:$SERVER_PORT"

function getPid() {
    pgrep -f $SERVICE_NAME
}

function checkPidAlive() {
    for i in `ls -t $APP_NAME/$APP_NAME.pid 2>/dev/null`
    do
        read pid < $i

        result=$(ps -p "$pid")
        if [ "$?" -eq 0 ]; then
            return 0
        else
            printf "\npid - $pid just quit unexpectedly, please check logs under $LOG_DIR and /tmp for more information!\n"
            exit 1;
        fi
    done

    printf "\nNo pid file found, startup may failed. Please check logs under $LOG_DIR and /tmp for more information!\n"
    exit 1;
}

function existProcessUsePort() {
    if [ "$(curl -X GET --silent --connect-timeout 1 --max-time 2 --head $SERVER_URL | grep "HTTP")" != "" ]; then
        true
    else
        false
    fi
}

function isServiceRunning() {
    if [ "$(curl -X GET --silent --connect-timeout 1 --max-time 2 $SERVER_URL/health | grep "UP")" != "" ]; then
        true
    else
        false
    fi
}

if [ "$(uname)" == "Darwin" ]; then
    windows="0"
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    windows="0"
elif [ "$(expr substr $(uname -s) 1 5)" == "MINGW" ]; then
    windows="1"
else
    windows="0"
fi

# for Windows
if [ "$windows" == "1" ] && [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]]; then
    tmp_java_home=`cygpath -sw "$JAVA_HOME"`
    export JAVA_HOME=`cygpath -u $tmp_java_home`
    echo "Windows new JAVA_HOME is: $JAVA_HOME"
fi

# Find Java
if [[ -n "$JAVA_HOME" ]] && [[ -x "$JAVA_HOME/bin/java" ]]; then
    javaexe="$JAVA_HOME/bin/java"
elif type -p java > /dev/null 2>&1; then
    javaexe=$(type -p java)
elif [[ -x "/usr/bin/java" ]];  then
    javaexe="/usr/bin/java"
else
    echo "Unable to find Java"
    exit 1
fi

if [[ "$javaexe" ]]; then
    version=$("$javaexe" -version 2>&1 | awk -F '"' '/version/ {print $2}')
    version=$(echo "$version" | awk -F. '{printf("%03d%03d",$1,$2);}')
    # now version is of format 009003 (9.3.x)
    if [ $version -ge 011000 ]; then
        JAVA_OPTS="$JAVA_OPTS -Xlog:gc*:$LOG_DIR/$SERVICE_NAME.gc.log:time,level,tags -Xlog:safepoint -Xlog:gc+heap=trace"
    elif [ $version -ge 010000 ]; then
        JAVA_OPTS="$JAVA_OPTS -Xlog:gc*:$LOG_DIR/$SERVICE_NAME.gc.log:time,level,tags -Xlog:safepoint -Xlog:gc+heap=trace"
    elif [ $version -ge 009000 ]; then
        JAVA_OPTS="$JAVA_OPTS -Xlog:gc*:$LOG_DIR/$SERVICE_NAME.gc.log:time,level,tags -Xlog:safepoint -Xlog:gc+heap=trace"
    else
        JAVA_OPTS="$JAVA_OPTS -XX:+UseParNewGC"
        JAVA_OPTS="$JAVA_OPTS -Xloggc:$LOG_DIR/$SERVICE_NAME.gc.log -XX:+PrintGCDetails"
        JAVA_OPTS="$JAVA_OPTS -XX:+UseConcMarkSweepGC -XX:+UseCMSCompactAtFullCollection -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=60 -XX:+CMSClassUnloadingEnabled -XX:+CMSParallelRemarkEnabled -XX:CMSFullGCsBeforeCompaction=9 -XX:+CMSClassUnloadingEnabled  -XX:+PrintGCDateStamps -XX:+PrintGCApplicationConcurrentTime -XX:+PrintHeapAtGC -XX:+UseGCLogFileRotation -XX:NumberOfGCLogFiles=5 -XX:GCLogFileSize=5M"
    fi
fi

cd `dirname $0`/..

for i in `ls $SERVICE_NAME-*.jar 2>/dev/null`
do
    if [[ ! $i == *"-sources.jar" ]]
    then
        PATH_TO_JAR=$i
        break
    fi
done

if [[ ! -f $PATH_TO_JAR && -d current ]]; then
    cd current
    for i in `ls $SERVICE_NAME-*.jar 2>/dev/null`
    do
        if [[ ! $i == *"-sources.jar" ]]
        then
            PATH_TO_JAR=$i
            break
        fi
    done
fi

# For Docker environment, start in foreground mode
if [[ -n "$APOLLO_RUN_MODE" ]] && [[ "$APOLLO_RUN_MODE" == "Docker" ]]; then
    exec $javaexe -Dsun.misc.URLClassPath.disableJarChecking=true $JAVA_OPTS -jar $PATH_TO_JAR
else
    # before running check there is another process use port or not
    if existProcessUsePort; then
        if isServiceRunning; then
            echo "$(date) ==== $SERVICE_NAME is running already with port $SERVER_PORT, pid $(getPid)"
            exit 0
        else
            echo "$(date) ==== $SERVICE_NAME failed to start. The port $SERVER_PORT already be in use by another process"
            echo "maybe you can figure out which process use port $SERVER_PORT by following ways:"
            echo "1. access http://change-to-this-machine-ip:$SERVER_PORT by browser"
            echo "2. run command 'curl $SERVER_URL'"
            echo "3. run command 'sudo netstat -tunlp | grep :$SERVER_PORT'"
            echo "4. run command 'sudo lsof -nP -iTCP:$SERVER_PORT -sTCP:LISTEN'"
            exit 1
        fi
    fi

    printf "$(date) ==== $SERVICE_NAME Starting ==== \n"

    if [[ -f $SERVICE_NAME".jar" ]]; then
        rm -rf $SERVICE_NAME".jar"
    fi
    ln $PATH_TO_JAR $SERVICE_NAME".jar"
    chmod a+x $SERVICE_NAME".jar"
    ./$SERVICE_NAME".jar" start

    rc=$?;

    if [[ $rc != 0 ]];
    then
        echo "$(date) Failed to start $SERVICE_NAME.jar, return code: $rc"
        exit $rc;
    fi

    declare -i counter=0
    declare -i max_counter=48 # 48*5=240s
    declare -i total_time=0

    printf "Waiting for server startup"
    until [[ (( counter -ge max_counter )) ]];
    do
        printf "."
        sleep 5
        counter+=1
        total_time=$((counter*5))

        checkPidAlive
        if isServiceRunning; then
            printf "\n$(date) Server started in $total_time seconds!\n"
            exit 0;
        fi
    done
    printf "\n$(date) Server failed to start in $total_time seconds!\n"
    exit 1;
fi
