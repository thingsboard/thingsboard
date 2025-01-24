#
# Copyright © 2016-2024 The Thingsboard Authors
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

FROM thingsboard/openjdk17:bookworm-slim

LABEL org.opencontainers.image.title="${docker.name}" \
      org.opencontainers.image.description="${description}" \
      org.opencontainers.image.url="https://www.thingsboard.io" \
      org.opencontainers.image.source="https://github.com/thingsboard/thingsboard" \
      org.opencontainers.image.version="${project.version}" \
      org.opencontainers.image.licenses="Apache-2.0" \
      org.opencontainers.image.created="${maven.build.timestamp}" \
      org.opencontainers.image.vendor="The ThingsBoard Authors"

COPY start-tb-lwm2m-transport.sh ${pkg.name}.deb /tmp/

RUN chmod a+x /tmp/*.sh \
    && mv /tmp/start-tb-lwm2m-transport.sh /usr/bin && \
    (yes | dpkg -i /tmp/${pkg.name}.deb) && \
    rm /tmp/${pkg.name}.deb && \
    (systemctl --no-reload disable --now ${pkg.name}.service > /dev/null 2>&1 || :) && \
    chmod 555 ${pkg.installFolder}/bin/${pkg.name}.jar

USER ${pkg.user}

CMD ["start-tb-lwm2m-transport.sh"]
