#!/bin/bash
#
# Copyright © 2016-2026 The Thingsboard Authors
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

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
REPO_DIR=$(cd "${SCRIPT_DIR}/../.." && pwd)

TB_DOCKER_REPO=${TB_DOCKER_REPO:-tb-local}
TB_SOURCE_VERSION=${TB_SOURCE_VERSION:-$(awk '
    /<artifactId>thingsboard<\/artifactId>/ {root=1; next}
    root && /<version>/ {
        sub(/^.*<version>/, "");
        sub(/<\/version>.*$/, "");
        print;
        exit;
    }
' "${REPO_DIR}/pom.xml")}
TB_IMAGE_TAG=${TB_IMAGE_TAG:-${TB_SOURCE_VERSION}-custom}
TB_MAVEN_PROJECTS=${TB_MAVEN_PROJECTS:-msa/tb-node,msa/web-ui,msa/js-executor,msa/transport/mqtt,msa/transport/http,msa/transport/coap}

MAVEN_GOALS=(package)
IMAGES=(
    tb-node
    tb-web-ui
    tb-js-executor
    tb-mqtt-transport
    tb-http-transport
    tb-coap-transport
)

require_command() {
    local command_name=$1
    if ! command -v "${command_name}" >/dev/null 2>&1; then
        echo "Missing required command: ${command_name}" >&2
        exit 1
    fi
}

tag_images() {
    local image_name=$1
    local source_ref="${TB_DOCKER_REPO}/${image_name}:${TB_SOURCE_VERSION}"
    local latest_ref="${TB_DOCKER_REPO}/${image_name}:latest"
    local target_ref="${TB_DOCKER_REPO}/${image_name}:${TB_IMAGE_TAG}"

    if docker image inspect "${source_ref}" >/dev/null 2>&1; then
        docker tag "${source_ref}" "${target_ref}"
        return
    fi

    if docker image inspect "${latest_ref}" >/dev/null 2>&1; then
        docker tag "${latest_ref}" "${target_ref}"
        return
    fi

    echo "Unable to find a built image for ${image_name}. Expected ${source_ref} or ${latest_ref}." >&2
    exit 1
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --clean)
            MAVEN_GOALS=(clean package)
        ;;
        *)
            echo "Unknown option: $1" >&2
            echo "Usage: ./.github/scripts/build-images.sh [--clean]" >&2
            exit 1
        ;;
    esac
    shift
done

require_command mvn
require_command docker

(
    cd "${REPO_DIR}"
    NODE_OPTIONS="${NODE_OPTIONS:---max_old_space_size=4096}" \
    mvn -T2 \
        -DskipTests \
        -Ddockerfile.skip=false \
        -Ddocker.repo="${TB_DOCKER_REPO}" \
        --projects "${TB_MAVEN_PROJECTS}" \
        --also-make \
        "${MAVEN_GOALS[@]}"
)

for image_name in "${IMAGES[@]}"; do
    tag_images "${image_name}"
done
