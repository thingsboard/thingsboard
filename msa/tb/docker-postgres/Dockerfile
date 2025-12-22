#
# Copyright © 2016-2025 The Thingsboard Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

FROM ${docker.base.image}

ENV PG_MAJOR 12

ENV DATA_FOLDER=/data

ENV HTTP_BIND_PORT=9090
ENV DATABASE_TS_TYPE=sql

ENV PGDATA=/data/db
ENV PATH=$PATH:/usr/lib/postgresql/$PG_MAJOR/bin

ENV SPRING_DRIVER_CLASS_NAME=org.postgresql.Driver
ENV SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/thingsboard
ENV SPRING_DATASOURCE_USERNAME=${pkg.user}

ENV PGLOG=/var/log/postgres

COPY logback.xml ${pkg.name}.conf start-db.sh stop-db.sh start-tb.sh upgrade-tb.sh install-tb.sh ${pkg.name}.deb /tmp/

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget gnupg2 \
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
    && mkdir -p $PGLOG \
    && chown -R ${pkg.user}:${pkg.user} $PGLOG \
    && chown -R ${pkg.user}:${pkg.user} /var/run/postgresql \
    && mkdir -p /data \
    && chown -R ${pkg.user}:${pkg.user} /data \
    && chown -R ${pkg.user}:${pkg.user} /var/log/${pkg.name} \
    && chmod 555 ${pkg.installFolder}/bin/${pkg.name}.jar

USER ${pkg.user}

EXPOSE 9090
EXPOSE 1883
EXPOSE 5683/udp
EXPOSE 5685/udp
EXPOSE 5686/udp

VOLUME ["/data"]

CMD ["start-tb.sh"]
