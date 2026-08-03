# Dev image for the ThingsBoard backend only (no Angular / ui-ngx build).
# The source tree is bind-mounted at /workspace, the Maven repository lives in
# a named volume at /root/.m2 so dependencies are downloaded only once.
FROM maven:3.9-eclipse-temurin-25-noble

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
 && apt-get install -y --no-install-recommends \
      postgresql-client \
      procps \
      curl \
      bash \
 && rm -rf /var/lib/apt/lists/*

ENV MAVEN_OPTS="-Xmx3g -XX:+TieredCompilation -XX:TieredStopAtLevel=1"
ENV MAVEN_CONFIG=/root/.m2

WORKDIR /workspace

COPY entrypoint.sh /opt/tb/entrypoint.sh
COPY build.sh /opt/tb/build.sh
COPY install-db.sh /opt/tb/install-db.sh
COPY run-watch.sh /opt/tb/run-watch.sh

RUN chmod +x /opt/tb/*.sh \
 && sed -i 's/\r$//' /opt/tb/*.sh

EXPOSE 8080 1883 5683/udp 7070 5005

ENTRYPOINT ["/opt/tb/entrypoint.sh"]
