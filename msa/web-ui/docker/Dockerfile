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

FROM thingsboard/node:22.18.0-bookworm-slim

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
