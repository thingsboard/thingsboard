#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

FROM thingsboard/node:22.23.4-bookworm-slim

ARG TARGETPLATFORM
ARG DEBIAN_FRONTEND=noninteractive

ENV NODE_ENV production
ENV DOCKER_MODE true
# The browser is the chromium package of Debian, installed below
ENV CHROME_EXECUTABLE=/usr/bin/chromium

COPY start-web-report.sh /tmp/
# The pinned chromium package of Debian, written by install.js from chromium.js
COPY chromium.env /tmp/

WORKDIR ${pkg.installFolder}

COPY ["src/package.json", "src/yarn.lock", "./"]

COPY package/linux/conf ./conf
COPY package/linux/conf ./config
COPY ${pkg.name}.conf ./conf
COPY src/api ./api
COPY src/config ./config
COPY src/inspectorShim.js src/server.js ./

RUN . /tmp/chromium.env \
    && echo "deb [check-valid-until=no] ${CHROMIUM_DEBIAN_ARCHIVE} bookworm-security main" > /etc/apt/sources.list.d/chromium.list \
    && apt-get update --error-on=any \
    && apt-get install -yq --no-install-recommends chromium=${CHROMIUM_DEBIAN_VERSION} chromium-common=${CHROMIUM_DEBIAN_VERSION} fontconfig fonts-noto-cjk-extra fonts-roboto fonts-noto-color-emoji fonts-unifont \
    xfonts-scalable fonts-liberation fonts-ipafont-gothic fonts-wqy-zenhei fonts-tlwg-loma-otf fonts-freefont-ttf \
    && rm -rf /var/lib/apt/lists/* \
    && rm /etc/apt/sources.list.d/chromium.list /tmp/chromium.env \
    && chmod a+x /tmp/*.sh \
    && mv /tmp/start-web-report.sh /usr/bin \
    && chown -R node:node ${pkg.installFolder} \
    && yarn install --production --non-interactive --network-concurrency 4 --network-timeout 100000 --mutex network && yarn cache clean --all

USER node

CMD ["start-web-report.sh"]
