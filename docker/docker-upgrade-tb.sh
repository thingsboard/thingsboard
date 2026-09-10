#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright The Thingsboard Authors
# SPDX-License-Identifier: Apache-2.0
#

for i in "$@"
do
case $i in
    --fromVersion=*)
    FROM_VERSION="${i#*=}"
    shift
    ;;
    *)
            # unknown option
    ;;
esac
done

fromVersion="${FROM_VERSION// }"

set -e

source compose-utils.sh

COMPOSE_VERSION=$(composeVersion) || exit $?

ADDITIONAL_COMPOSE_QUEUE_ARGS=$(additionalComposeQueueArgs) || exit $?

ADDITIONAL_COMPOSE_ARGS=$(additionalComposeArgs) || exit $?

ADDITIONAL_CACHE_ARGS=$(additionalComposeCacheArgs) || exit $?

ADDITIONAL_COMPOSE_EDQS_ARGS=$(additionalComposeEdqsArgs) || exit $?

ADDITIONAL_STARTUP_SERVICES=$(additionalStartupServices) || exit $?

COMPOSE_ARGS_PULL="\
      -f docker-compose.yml ${ADDITIONAL_CACHE_ARGS} ${ADDITIONAL_COMPOSE_ARGS} ${ADDITIONAL_COMPOSE_QUEUE_ARGS} ${ADDITIONAL_COMPOSE_EDQS_ARGS} \
      pull \
      tb-core1"

COMPOSE_ARGS_UP="\
      -f docker-compose.yml ${ADDITIONAL_CACHE_ARGS} ${ADDITIONAL_COMPOSE_ARGS} ${ADDITIONAL_COMPOSE_QUEUE_ARGS} ${ADDITIONAL_COMPOSE_EDQS_ARGS} \
      up -d ${ADDITIONAL_STARTUP_SERVICES}"

COMPOSE_ARGS_RUN="\
      -f docker-compose.yml ${ADDITIONAL_CACHE_ARGS} ${ADDITIONAL_COMPOSE_ARGS} ${ADDITIONAL_COMPOSE_QUEUE_ARGS} ${ADDITIONAL_COMPOSE_EDQS_ARGS} \
      run --no-deps --rm -e UPGRADE_TB=true -e FROM_VERSION=${fromVersion} \
      tb-core1"

case $COMPOSE_VERSION in
    V2)
        docker compose $COMPOSE_ARGS_PULL
        docker compose $COMPOSE_ARGS_UP
        docker compose $COMPOSE_ARGS_RUN
    ;;
    V1)
        docker-compose $COMPOSE_ARGS_PULL
        docker-compose $COMPOSE_ARGS_UP
        docker-compose $COMPOSE_ARGS_RUN
    ;;
    *)
        # unknown option
    ;;
esac
