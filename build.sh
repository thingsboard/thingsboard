#!/bin/bash
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

set -e # exit on any error

#PROJECTS="msa/tb-node,msa/web-ui,rule-engine-pe/rule-node-twilio-sms"
PROJECTS=""

if [ "$1" ]; then
  PROJECTS="--projects $1"
fi

echo "Building and pushing [amd64,arm64] projects '$PROJECTS' ..."
echo "HELP: usage ./build.sh [projects]"
echo "HELP: example ./build.sh msa/web-ui,msa/web-report"
java -version
#echo "Cleaning ui-ngx/node_modules" && rm -rf ui-ngx/node_modules

MAVEN_OPTS="-Xmx1024m" NODE_OPTIONS="--max_old_space_size=4096" DOCKER_CLI_EXPERIMENTAL=enabled DOCKER_BUILDKIT=0 \
mvn -T2 license:format clean install -DskipTests \
  $PROJECTS --also-make
#   \
#  -Dpush-docker-amd-arm-images
#  -Ddockerfile.skip=false -Dpush-docker-image=true
#  --offline
#  --projects '!msa/web-report' --also-make

# push all
# mvn -T 1C license:format clean install -DskipTests -Ddockerfile.skip=false -Dpush-docker-image=true


## Build and push AMD and ARM docker images using docker buildx
## Reference to article how to setup docker miltiplatform build environment: https://medium.com/@artur.klauser/building-multi-architecture-docker-images-with-buildx-27d80f7e2408
## install docker-ce from docker repo https://docs.docker.com/engine/install/ubuntu/
# sudo apt install -y qemu-user-static binfmt-support
# export DOCKER_CLI_EXPERIMENTAL=enabled
# docker version
# docker run --rm --privileged multiarch/qemu-user-static --reset -p yes
# docker buildx create --name mybuilder
# docker buildx use mybuilder
# docker buildx inspect --bootstrap
# docker buildx ls
# mvn clean install -P push-docker-amd-arm-images