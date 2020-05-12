#
# Copyright © 2016-2020 The Thingsboard Authors
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

FROM thingsboard/openjdk8

RUN apt-get update
RUN apt-get install -y curl nmap procps
RUN echo 'deb http://www.apache.org/dist/cassandra/debian 311x main' | tee --append /etc/apt/sources.list.d/cassandra.list > /dev/null
RUN curl -L https://www.apache.org/dist/cassandra/KEYS | apt-key add -
RUN apt-get update
RUN apt-get install -y cassandra cassandra-tools
RUN update-rc.d cassandra disable
RUN sed -i.old '/ulimit/d' /etc/init.d/cassandra

COPY logback.xml ${pkg.name}.conf start-db.sh stop-db.sh start-tb.sh upgrade-tb.sh install-tb.sh ${pkg.name}.deb /tmp/

RUN chmod a+x /tmp/*.sh \
    && mv /tmp/start-tb.sh /usr/bin \
    && mv /tmp/upgrade-tb.sh /usr/bin \
    && mv /tmp/install-tb.sh /usr/bin \
    && mv /tmp/start-db.sh /usr/bin \
    && mv /tmp/stop-db.sh /usr/bin

RUN dpkg -i /tmp/${pkg.name}.deb

RUN systemctl --no-reload disable --now ${pkg.name}.service > /dev/null 2>&1 || :

RUN mv /tmp/logback.xml ${pkg.installFolder}/conf \
    && mv /tmp/${pkg.name}.conf ${pkg.installFolder}/conf

ENV DATA_FOLDER=/data

ENV HTTP_BIND_PORT=9090
ENV DATABASE_TS_TYPE=cassandra
ENV DATABASE_ENTITIES_TYPE=cassandra

ENV CASSANDRA_HOST=localhost
ENV CASSANDRA_PORT=9042
ENV CASSANDRA_HOME=/opt/cassandra
ENV PATH $CASSANDRA_HOME/bin:$PATH

RUN rm -rf /var/lib/cassandra

RUN chmod a+w /var/lib

RUN mkdir -p $DATA_FOLDER
RUN chown -R cassandra:cassandra /data

RUN chown -R cassandra:cassandra /var/log/${pkg.name}
RUN chmod 555 ${pkg.installFolder}/bin/${pkg.name}.jar


USER cassandra

EXPOSE 9090
EXPOSE 1883
EXPOSE 5683/udp

VOLUME ["/data"]

CMD ["start-tb.sh"]
