#
# Copyright 2022 Apollo Authors
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
# Dockerfile for apollo-db
# 1. docker build --build-arg COMPONENT=portal -t apollodb-portal .
# or docker build --build-arg COMPONENT=config -t apollodb-config .
# or docker build -t apollodb-all .
# 2. Run with(apollodb-all for example): docker run -dti -p 3306:3306 -e MYSQL_ALLOW_EMPTY_PASSWORD=yes -e MYSQL_USER=apollo -e MYSQL_PASSWORD=apollo -e  -e  -e   -v /tmp/dbdata:/var/lib/mysql --name apollo-db apollodb-all

# mariadb can run with arm64, you can change it to mysql:5.7 for amd64.
FROM mariadb:10.7.3 

LABEL maintainer="guox@goodrain.com"

ARG COMPONENT

EXPOSE 3306

VOLUME [ "/var/lib/mysql" ]

COPY ./*.sql /tmp/

RUN if [ ! -z ${COMPONENT} ];then cp /tmp/apollo${COMPONENT}db.sql /docker-entrypoint-initdb.d;else cp /tmp/*.sql /docker-entrypoint-initdb.d;fi

