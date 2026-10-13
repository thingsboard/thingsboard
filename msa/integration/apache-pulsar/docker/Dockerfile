#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

FROM ${docker.base.image}

COPY start-tb-apache-pulsar-integration.sh ${pkg.name}.deb /tmp/

RUN chmod a+x /tmp/*.sh \
    && mv /tmp/start-tb-apache-pulsar-integration.sh /usr/bin && \
    (yes | dpkg -i /tmp/${pkg.name}.deb) && \
    rm /tmp/${pkg.name}.deb && \
    (systemctl --no-reload disable --now ${pkg.name}.service > /dev/null 2>&1 || :) && \
    chmod 555 ${pkg.installFolder}/bin/${pkg.name}.jar

USER ${pkg.user}

CMD ["start-tb-apache-pulsar-integration.sh"]
