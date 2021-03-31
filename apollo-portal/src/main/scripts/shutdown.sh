#!/bin/bash
SERVICE_NAME=apollo-portal
export APP_NAME=$SERVICE_NAME

if [[ -z "$JAVA_HOME" && -d /usr/java/latest/ ]]; then
    export JAVA_HOME=/usr/java/latest/
fi

cd `dirname $0`/..

if [[ ! -f $SERVICE_NAME".jar" && -d current ]]; then
    cd current
fi

if [[ -f $SERVICE_NAME".jar" ]]; then
  chmod a+x $SERVICE_NAME".jar"
  ./$SERVICE_NAME".jar" stop
fi
