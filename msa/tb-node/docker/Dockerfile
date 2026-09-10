#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

FROM ${docker.base.image}

# For SVG renderer Debian Trixie slim fonts should be installed explicitly
RUN echo "Previously installed font libraries:" \
 && (dpkg -l | grep -E 'font|dejavu|harfbuzz' || echo "  (none)") \
 && apt-get update \
 && apt-get install -y --no-install-recommends \
      libharfbuzz0b fontconfig fonts-dejavu-core \
 && rm -rf /var/lib/apt/lists/* \
 && echo "Current fonts libraries:" \
 && (dpkg -l | grep -E 'font|dejavu|harfbuzz') \
 && echo "Font config list:" \
 && fc-list && test -n "$(fc-list)" \
 && (fc-list | grep -i dejavu) \
 && echo "Harfbuzz library test with ldconfig:" \
 && (ldconfig -p | grep harfbuzz)

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
