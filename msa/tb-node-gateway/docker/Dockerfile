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

FROM eclipse-temurin:17.0.11_9-jre-alpine

RUN addgroup --system 1000 && adduser --system --ingroup 1000 --disabled-password 1000

COPY --chown=1000:1000 ${pkg.name}-${project.version}-boot.jar app.jar
USER 1000

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]

HEALTHCHECK --interval=25s --timeout=3s --retries=2 CMD wget -qO- http://localhost:8080/actuator/health || exit 1