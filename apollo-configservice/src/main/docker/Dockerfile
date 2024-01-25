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
# Dockerfile for apollo-configservice
# 1. ./scripts/build.sh
# 2. Build with: mvn docker:build -pl apollo-configservice
# 3. Run with: docker run -p 8080:8080 -e SPRING_DATASOURCE_URL="jdbc:mysql://fill-in-the-correct-server:3306/ApolloConfigDB?characterEncoding=utf8" -e SPRING_DATASOURCE_USERNAME=FillInCorrectUser -e SPRING_DATASOURCE_PASSWORD=FillInCorrectPassword -d -v /tmp/logs:/opt/logs --name apollo-configservice apolloconfig/apollo-configservice

FROM alpine:3.15.5

ARG VERSION
ENV VERSION $VERSION

COPY apollo-configservice-${VERSION}-github.zip /apollo-configservice/apollo-configservice-${VERSION}-github.zip

RUN unzip /apollo-configservice/apollo-configservice-${VERSION}-github.zip -d /apollo-configservice \
    && rm -rf /apollo-configservice/apollo-configservice-${VERSION}-github.zip \
    && chmod +x /apollo-configservice/scripts/startup.sh

FROM openjdk:8-jre-slim
LABEL maintainer="g632104866@gmail.com;finchcn@gmail.com;ameizi<sxyx2008@163.com>"

ENV APOLLO_RUN_MODE "Docker"
ENV SERVER_PORT 8080

RUN echo "deb http://mirrors.aliyun.com/debian/ bullseye main non-free contrib" > /etc/apt/sources.list \
    && echo "deb http://mirrors.aliyun.com/debian-security/ bullseye-security main" >> /etc/apt/sources.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends procps curl bash tzdata \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

COPY --from=0 /apollo-configservice /apollo-configservice

EXPOSE $SERVER_PORT

CMD ["/apollo-configservice/scripts/startup.sh"]
