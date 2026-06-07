FROM openjdk:8-jre-alpine

LABEL maintainer="core-ledger-platform"

ENV TZ=Asia/Shanghai
ENV LANG=C.UTF-8

RUN apk add --no-cache curl wget tzdata \
    && ln -sf /usr/share/zoneinfo/${TZ} /etc/localtime \
    && echo "${TZ}" > /etc/timezone

ENV SKYWALKING_VERSION=8.11.0
ENV SKYWALKING_AGENT_HOME=/skywalking-agent

RUN mkdir -p ${SKYWALKING_AGENT_HOME} \
    && wget -O /tmp/skywalking-agent.tar.gz \
    "https://archive.apache.org/dist/skywalking/${SKYWALKING_VERSION}/apache-skywalking-apm-${SKYWALKING_VERSION}.tar.gz" \
    && tar -xzf /tmp/skywalking-agent.tar.gz -C /tmp \
    && cp -r /tmp/apache-skywalking-apm-bin/agent/* ${SKYWALKING_AGENT_HOME}/ \
    && rm -rf /tmp/apache-skywalking-apm-bin /tmp/skywalking-agent.tar.gz

ENV APP_HOME=/app
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC"
ENV JAVA_AGENT_OPTS="-javaagent:${SKYWALKING_AGENT_HOME}/skywalking-agent.jar"
ENV SW_AGENT_NAME=core-ledger-account
ENV SW_AGENT_COLLECTOR_BACKEND_SERVICES=127.0.0.1:11800

RUN mkdir -p ${APP_HOME}
WORKDIR ${APP_HOME}

COPY docker/skywalking/agent.config ${SKYWALKING_AGENT_HOME}/config/agent.config

COPY core-ledger-account/target/core-ledger-account-*.jar ${APP_HOME}/app.jar

EXPOSE 8081 20881

ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} ${JAVA_AGENT_OPTS} -DSW_AGENT_NAME=${SW_AGENT_NAME} -DSW_AGENT_COLLECTOR_BACKEND_SERVICES=${SW_AGENT_COLLECTOR_BACKEND_SERVICES} -jar app.jar"]
