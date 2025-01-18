FROM ghcr.io/meshware/ubuntu-oracle-jdk:11.0.21
USER root

ENV TZ=Asia/Shanghai

WORKDIR /export/servers/
COPY ./target/joylive-demo-kubernetes-gateway.jar /export/servers/
COPY ./target/live-1.6.0-SNAPSHOT/ /export/servers/live/

VOLUME ["/export/servers/logs", "/export/servers/config"]
EXPOSE 80
ENTRYPOINT ["/bin/bash", "-c", "java ${JAVA_OPTS} -javaagent:/export/servers/live/live.jar -jar /export/servers/joylive-demo-kubernetes-gateway.jar"]
