#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

FROM ${docker.base.image}

COPY logback.xml start-tb-node.sh ${pkg.name}.deb /tmp/

RUN chmod a+x /tmp/*.sh \
    && mv /tmp/start-tb-node.sh /usr/bin && \
    (yes | dpkg -i /tmp/${pkg.name}.deb) && \
    rm /tmp/${pkg.name}.deb && \
    mv /tmp/logback.xml ${pkg.installFolder}/conf && \
    chown -R ${pkg.user}:${pkg.user} ${pkg.installFolder}/conf/logback.xml && \
    (systemctl --no-reload disable --now ${pkg.name}.service > /dev/null 2>&1 || :) && \
    chown -R ${pkg.user}:${pkg.user} /tmp && \
    chmod 555 ${pkg.installFolder}/bin/${pkg.name}.jar

USER ${pkg.user}

CMD ["start-tb-node.sh"]
