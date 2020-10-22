# Dockerfile for apollo-configservice
# 1. ./scripts/build.sh
# 2. Build with: mvn docker:build -pl apollo-configservice
# 3. Run with: docker run -p 8080:8080 -e SPRING_DATASOURCE_URL="jdbc:mysql://fill-in-the-correct-server:3306/ApolloConfigDB?characterEncoding=utf8" -e SPRING_DATASOURCE_USERNAME=FillInCorrectUser -e SPRING_DATASOURCE_PASSWORD=FillInCorrectPassword -d -v /tmp/logs:/opt/logs --name apollo-configservice apolloconfig/apollo-configservice

FROM openjdk:8-jre-alpine
MAINTAINER ameizi <sxyx2008@163.com>

ENV APOLLO_RUN_MODE "Docker"
ENV VERSION 1.8.0-SNAPSHOT
ENV SERVER_PORT 8080

RUN echo "http://mirrors.aliyun.com/alpine/v3.8/main" > /etc/apk/repositories \
    && echo "http://mirrors.aliyun.com/alpine/v3.8/community" >> /etc/apk/repositories \
    && apk update upgrade \
    && apk add --no-cache procps unzip curl bash tzdata \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && echo "Asia/Shanghai" > /etc/timezone

ADD apollo-configservice-${VERSION}-github.zip /apollo-configservice/apollo-configservice-${VERSION}-github.zip

RUN unzip /apollo-configservice/apollo-configservice-${VERSION}-github.zip -d /apollo-configservice \
    && rm -rf /apollo-configservice/apollo-configservice-${VERSION}-github.zip \
    && chmod +x /apollo-configservice/scripts/startup.sh

EXPOSE $SERVER_PORT

CMD ["/apollo-configservice/scripts/startup.sh"]
