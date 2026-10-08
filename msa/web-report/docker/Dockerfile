#
# SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
# SPDX-License-Identifier: BUSL-1.1
#

FROM thingsboard/node:22.22.2-bookworm-slim

ARG TARGETPLATFORM
ARG DEBIAN_FRONTEND=noninteractive

ENV NODE_ENV production
ENV DOCKER_MODE true
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

COPY start-web-report.sh /tmp/

WORKDIR ${pkg.installFolder}

COPY ["src/package.json", "src/yarn.lock", "./"]

COPY package/linux/conf ./conf
COPY package/linux/conf ./config
COPY ${pkg.name}.conf ./conf
COPY src/api ./api
COPY src/config ./config
COPY src/inspectorShim.js src/server.js ./

RUN apt-get update \
    && apt-get install -yq --no-install-recommends fonts-noto-cjk-extra fonts-roboto fonts-noto-color-emoji fonts-unifont libfreetype6 xfonts-scalable fonts-liberation \
    fonts-ipafont-gothic fonts-wqy-zenhei fonts-tlwg-loma-otf fonts-freefont-ttf fontconfig libasound2 libatk-bridge2.0-0 libatk1.0-0 libatspi2.0-0 \
    libcairo2 libcups2 libdbus-1-3 libdrm2 libgbm1 libglib2.0-0 libnspr4 libnss3 libpango-1.0-0 libwayland-client0 libx11-6 libxcb1 libxcomposite1 \
    libxdamage1 libxext6 libxfixes3 libxkbcommon0 libxrandr2 \
    && rm -rf /var/lib/apt/lists/* \
    && chmod a+x /tmp/*.sh \
    && mv /tmp/start-web-report.sh /usr/bin \
    && chown -R node:node ${pkg.installFolder} \
    && mkdir /ms-playwright \
    && yarn install --production --non-interactive --network-concurrency 4 --network-timeout 100000 --mutex network && yarn cache clean --all \
    && chmod -R 777 /ms-playwright

USER node

CMD ["start-web-report.sh"]
