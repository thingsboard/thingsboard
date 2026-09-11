#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

FROM ${docker.base.image}

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
    && apt-get install -yq --no-install-recommends ncdu \
    && rm -rf /var/lib/apt/lists/*

COPY start-tb-vc-executor.sh ${pkg.name}.deb /tmp/

RUN mkdir -p /home/thingsboard/.config/jgit \
    && chown -R ${pkg.user}:${pkg.user} /home/thingsboard \
    && chmod a+x /tmp/*.sh \
    && mv /tmp/start-tb-vc-executor.sh /usr/bin && \
    (yes | dpkg -i /tmp/${pkg.name}.deb) && \
    rm /tmp/${pkg.name}.deb && \
    (systemctl --no-reload disable --now ${pkg.name}.service > /dev/null 2>&1 || :) && \
    chmod 555 ${pkg.installFolder}/bin/${pkg.name}.jar

USER ${pkg.user}

CMD ["start-tb-vc-executor.sh"]
