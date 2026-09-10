#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

FROM thingsboard/node:22.22.2-bookworm-slim

ENV NODE_ENV production
ENV DOCKER_MODE true

COPY start-web-ui.sh /tmp/

WORKDIR ${pkg.installFolder}

COPY ["src/package.json", "src/yarn.lock", "./"]

COPY package/linux/conf ./conf
COPY package/linux/conf ./config
COPY web ./web
COPY src/config ./config
COPY src/server.js ./

RUN chmod a+x /tmp/*.sh \
    && mv /tmp/start-web-ui.sh /usr/bin \
    && chown -R node:node ${pkg.installFolder} \
    && yarn install --production --non-interactive --network-concurrency 4 --network-timeout 100000 --mutex network && yarn cache clean --all

USER node

CMD ["start-web-ui.sh"]
