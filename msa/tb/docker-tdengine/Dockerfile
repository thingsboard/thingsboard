FROM tdengine/tdengine
ENV PG_MAJOR=12
ENV DATA_FOLDER=/data
ENV HTTP_BIND_PORT=9090
ENV DATABASE_TS_TYPE=tdengine
ENV DATABASE_TS_LATEST_TYPE=tdengine

ENV PGDATA=/data/db
ENV PATH=$PATH:/usr/lib/postgresql/$PG_MAJOR/bin

ENV SPRING_DRIVER_CLASS_NAME=org.postgresql.Driver
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/thingsboard
ENV SPRING_DATASOURCE_USERNAME=${pkg.user}
ENV SPRING_DATASOURCE_PASSWORD=postgres

ENV TDENGINE_URL=jdbc:TAOS-RS://localhost:6041/thingsboard
ENV TDENGINE_USERNAME=root
ENV TDENGINE_PASSWORD=taosdata
ENV TDENGINE_STR_LEN=1024
ENV TDENGINE_USE_STABLE=true

ENV PGLOG=/var/log/postgres

COPY ubuntu-sources.list /etc/apt/sources.list
COPY logback.xml ${pkg.name}.conf start-db.sh stop-db.sh start-tb.sh upgrade-tb.sh install-tb.sh ${pkg.name}.deb /tmp/

RUN useradd --user-group --system --create-home --no-log-init ${pkg.user}

RUN apt-get update \
    && apt-get install -y --no-install-recommends apt-utils wget gnupg2 openjdk-11-jdk \
    && echo "deb http://apt.postgresql.org/pub/repos/apt/ $(. /etc/os-release && echo -n $VERSION_CODENAME)-pgdg main" | tee --append /etc/apt/sources.list.d/pgdg.list > /dev/null \
    && wget -q https://www.postgresql.org/media/keys/ACCC4CF8.asc -O- | apt-key add - \
    && apt-get update \
    && apt-get install -y --no-install-recommends postgresql-${PG_MAJOR} \
    && rm -rf /var/lib/apt/lists/* \
    && update-rc.d postgresql disable \
    && apt-get purge -y --auto-remove \
    && chmod a+x /tmp/*.sh \
    && mv /tmp/start-tb.sh /usr/bin \
    && mv /tmp/upgrade-tb.sh /usr/bin \
    && mv /tmp/install-tb.sh /usr/bin \
    && mv /tmp/start-db.sh /usr/bin \
    && mv /tmp/stop-db.sh /usr/bin \
    && dpkg -i /tmp/${pkg.name}.deb \
    && rm /tmp/${pkg.name}.deb \
    && (systemctl --no-reload disable --now ${pkg.name}.service > /dev/null 2>&1 || :) \
    && mv /tmp/logback.xml ${pkg.installFolder}/conf \
    && mv /tmp/${pkg.name}.conf ${pkg.installFolder}/conf \
# postgres config
    && mkdir -p $PGLOG \
    && chown -R ${pkg.user}:${pkg.user} $PGLOG \
    && chown -R ${pkg.user}:${pkg.user} /var/log/postgresql \
    && chown -R ${pkg.user}:${pkg.user} /var/run/postgresql \
    && mkdir -p /data \
    && chown -R ${pkg.user}:${pkg.user} /data \
    && chown -R ${pkg.user}:${pkg.user} /var/log/${pkg.name} \
    && chmod 555 ${pkg.installFolder}/bin/${pkg.name}.jar

ENV TAOS_DATA_DIR=/data/tdengine/data
ENV TAOS_LOG_DIR=/data/tdengine/log

USER ${pkg.user}

EXPOSE 9090
EXPOSE 1883
EXPOSE 5683/udp
EXPOSE 5685/udp

VOLUME ["/data"]

ENTRYPOINT ["/tini", "--"]

CMD ["start-tb.sh"]