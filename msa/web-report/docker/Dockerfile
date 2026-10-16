#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

FROM thingsboard/node:22.22.2-bookworm-slim

ARG TARGETPLATFORM
ARG DEBIAN_FRONTEND=noninteractive

ENV NODE_ENV production
ENV DOCKER_MODE true
ENV PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1
ENV CHROME_EXECUTABLE=/usr/bin/chromium

COPY start-web-report.sh /tmp/

WORKDIR ${pkg.installFolder}

COPY ["src/package.json", "src/yarn.lock", "./"]

COPY package/linux/conf ./conf
COPY package/linux/conf ./config
COPY ${pkg.name}.conf ./conf
COPY src/api ./api
COPY src/config ./config
COPY src/browserExecutable.js src/inspectorShim.js src/server.js ./

RUN apt-get update \
    && apt-get install -yq --no-install-recommends chromium fonts-noto-cjk-extra fonts-roboto fonts-noto-color-emoji fonts-unifont libfreetype6 \
    xfonts-scalable fonts-liberation fonts-ipafont-gothic fonts-wqy-zenhei fonts-tlwg-loma-otf fonts-freefont-ttf fontconfig \
    && rm -rf /var/lib/apt/lists/* \
    && chmod a+x /tmp/*.sh \
    && mv /tmp/start-web-report.sh /usr/bin \
    && chown -R node:node ${pkg.installFolder} \
    && yarn install --production --non-interactive --network-concurrency 4 --network-timeout 100000 --mutex network && yarn cache clean --all

USER node

CMD ["start-web-report.sh"]
