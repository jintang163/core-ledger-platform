FROM registry.cn-hangzhou.aliyuncs.com/ubuntu:18.04

ENV LANG=C.UTF-8 \
    TZ=Asia/Shanghai \
    JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 \
    JRE_HOME=$JAVA_HOME/jre \
    PATH=$JAVA_HOME/bin:$PATH \
    APP_NAME=core-ledger-account \
    APP_HOME=/opt/app

RUN set -eux; \
    apt-get update; \
    apt-get install -y --no-install-recommends \
        openjdk-8-jdk-headless \
        curl \
        tzdata \
        fontconfig \
        libfreetype6; \
    ln -snf /usr/share/zoneinfo/$TZ /etc/localtime; \
    echo $TZ > /etc/timezone; \
    mkdir -p $APP_HOME; \
    apt-get clean; \
    rm -rf /var/lib/apt/lists/*

WORKDIR $APP_HOME

COPY core-ledger-account/target/core-ledger-account.jar $APP_HOME/app.jar

RUN set -eux; \
    groupadd -r appuser; \
    useradd -r -g appuser appuser; \
    chown -R appuser:appuser $APP_HOME

USER appuser

EXPOSE 8081 20881

ENTRYPOINT ["java", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-Dfile.encoding=UTF-8", \
    "-Duser.timezone=Asia/Shanghai", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/var/log/app/heapdump.hprof", \
    "-Xms512m", \
    "-Xmx2048m", \
    "-jar", \
    "/opt/app/app.jar"]
