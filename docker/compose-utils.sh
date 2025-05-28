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

function additionalComposeArgs() {
    source .env
    ADDITIONAL_COMPOSE_ARGS=""
    case $DATABASE in
        postgres)
        ADDITIONAL_COMPOSE_ARGS="-f docker-compose.postgres.yml"
        ;;
        hybrid)
        ADDITIONAL_COMPOSE_ARGS="-f docker-compose.hybrid.yml"
        ;;
        *)
        echo "Unknown DATABASE value specified in the .env file: '${DATABASE}'. Should be either 'postgres' or 'hybrid'." >&2
        exit 1
    esac
    echo $ADDITIONAL_COMPOSE_ARGS
}

function additionalComposeQueueArgs() {
    source .env
    ADDITIONAL_COMPOSE_QUEUE_ARGS=""
    case $TB_QUEUE_TYPE in
        kafka)
        ADDITIONAL_COMPOSE_QUEUE_ARGS="-f docker-compose.kafka.yml"
        ;;
        confluent)
        ADDITIONAL_COMPOSE_QUEUE_ARGS="-f docker-compose.confluent.yml"
        ;;
        *)
        echo "Unknown Queue service TB_QUEUE_TYPE value specified in the .env file: '${TB_QUEUE_TYPE}'. Should be either 'kafka' or 'confluent'." >&2
        exit 1
    esac
    echo $ADDITIONAL_COMPOSE_QUEUE_ARGS
}

function additionalComposeMonitoringArgs() {
    source .env

    if [ "$MONITORING_ENABLED" = true ]
    then
      ADDITIONAL_COMPOSE_MONITORING_ARGS="-f docker-compose.prometheus-grafana.yml"
      echo $ADDITIONAL_COMPOSE_MONITORING_ARGS
    else
      echo ""
    fi
}

function additionalComposeCacheArgs() {
    source .env
    CACHE_COMPOSE_ARGS=""
    CACHE="${CACHE:-valkey}"
    case $CACHE in
        valkey)
        CACHE_COMPOSE_ARGS="-f docker-compose.valkey.yml"
        ;;
        valkey-cluster)
        CACHE_COMPOSE_ARGS="-f docker-compose.valkey-cluster.yml"
        ;;
        valkey-sentinel)
        CACHE_COMPOSE_ARGS="-f docker-compose.valkey-sentinel.yml"
        ;;
        *)
        echo "Unknown CACHE value specified in the .env file: '${CACHE}'. Should be either 'valkey' or 'valkey-cluster' or 'valkey-sentinel'." >&2
        exit 1
    esac
    echo $CACHE_COMPOSE_ARGS
}

function additionalStartupServices() {
    source .env
    ADDITIONAL_STARTUP_SERVICES=""
    case $DATABASE in
        postgres)
        ADDITIONAL_STARTUP_SERVICES="$ADDITIONAL_STARTUP_SERVICES postgres"
        ;;
        hybrid)
        ADDITIONAL_STARTUP_SERVICES="$ADDITIONAL_STARTUP_SERVICES postgres cassandra"
        ;;
        *)
        echo "Unknown DATABASE value specified in the .env file: '${DATABASE}'. Should be either 'postgres' or 'hybrid'." >&2
        exit 1
    esac

    CACHE="${CACHE:-valkey}"
    case $CACHE in
        valkey)
        ADDITIONAL_STARTUP_SERVICES="$ADDITIONAL_STARTUP_SERVICES valkey"
        ;;
        valkey-cluster)
        ADDITIONAL_STARTUP_SERVICES="$ADDITIONAL_STARTUP_SERVICES valkey-node-0 valkey-node-1 valkey-node-2 valkey-node-3 valkey-node-4 valkey-node-5"
        ;;
        valkey-sentinel)
        ADDITIONAL_STARTUP_SERVICES="$ADDITIONAL_STARTUP_SERVICES valkey-primary valkey-replica valkey-sentinel"
        ;;
        *)
        echo "Unknown CACHE value specified in the .env file: '${CACHE}'. Should be either 'valkey' or 'valkey-cluster' or 'valkey-sentinel'." >&2
        exit 1
    esac

    echo $ADDITIONAL_STARTUP_SERVICES
}

function additionalComposeEdqsArgs() {
    source .env

    if [ "$EDQS_ENABLED" = true ]
    then
      ADDITIONAL_COMPOSE_EDQS_ARGS="-f docker-compose.edqs.yml"
      echo $ADDITIONAL_COMPOSE_EDQS_ARGS
    else
      echo ""
    fi
}

function permissionList() {
    PERMISSION_LIST="
      799  799  tb-node/log
      799  799  tb-transports/lwm2m/log
      799  799  tb-transports/http/log
      799  799  tb-transports/mqtt/log
      799  799  tb-transports/snmp/log
      799  799  tb-transports/coap/log
      799  799  tb-vc-executor/log
      999  999  tb-node/postgres
      "

    source .env

    if [ "$DATABASE" = "hybrid" ]; then
      PERMISSION_LIST="$PERMISSION_LIST
      999  999  tb-node/cassandra
      "
    fi

    if [ "$EDQS_ENABLED" = true ]; then
      PERMISSION_LIST="$PERMISSION_LIST
      799  799  edqs/log
      "
    fi

    CACHE="${CACHE:-valkey}"
    case $CACHE in
        valkey)
          PERMISSION_LIST="$PERMISSION_LIST
          1001 1001 tb-node/valkey-data
          "
        ;;
        valkey-cluster)
          PERMISSION_LIST="$PERMISSION_LIST
          1001 1001 tb-node/valkey-cluster-data-0
          1001 1001 tb-node/valkey-cluster-data-1
          1001 1001 tb-node/valkey-cluster-data-2
          1001 1001 tb-node/valkey-cluster-data-3
          1001 1001 tb-node/valkey-cluster-data-4
          1001 1001 tb-node/valkey-cluster-data-5
          "
        ;;
        valkey-sentinel)
          PERMISSION_LIST="$PERMISSION_LIST
          1001 1001 tb-node/valkey-sentinel-data-primary
          1001 1001 tb-node/valkey-sentinel-data-replica
          1001 1001 tb-node/valkey-sentinel-data-sentinel
          "
        ;;
        *)
        echo "Unknown CACHE value specified in the .env file: '${CACHE}'. Should be either 'valkey' or 'valkey-cluster' or 'valkey-sentinel'." >&2
        exit 1
    esac

    echo "$PERMISSION_LIST"
}

function checkFolders() {
  CREATE=false
  SKIP_CHOWN=false
  for i in "$@"
    do
      case $i in
          --create)
          CREATE=true
          shift
          ;;
          --skipChown)
          SKIP_CHOWN=true
          shift
          ;;
          *)
                  # unknown option
          ;;
      esac
    done
  EXIT_CODE=0
  PERMISSION_LIST=$(permissionList) || exit $?
  set -e
  while read -r USR GRP DIR
  do
    IS_EXIST_CHECK_PASSED=false
    IS_OWNER_CHECK_PASSED=false

    # skip empty lines
    if [ -z "$DIR" ]; then
          continue
    fi

    # checks section
    echo "Checking if dir ${DIR} exists..."
    if [[ -d "$DIR" ]]; then
      echo "> OK"
      IS_EXIST_CHECK_PASSED=true
      if [ "$SKIP_CHOWN" = false ]; then
        echo "Checking user ${USR} group ${GRP} ownership for dir ${DIR}..."
        if [[ $(ls -ldn "$DIR" | awk '{print $3}') -eq "$USR" ]] && [[ $(ls -ldn "$DIR" | awk '{print $4}') -eq "$GRP" ]]; then
          echo "> OK"
          IS_OWNER_CHECK_PASSED=true
        else
          echo "...ownership check failed"
          if [ "$CREATE" = false ]; then
            EXIT_CODE=1
          fi
        fi
      fi
    else
      echo "...does not exist"
      if [ "$CREATE" = false ]; then
        EXIT_CODE=1
      fi
    fi

    # create/chown section
    if [ "$CREATE" = true ]; then
      if [ "$IS_EXIST_CHECK_PASSED" = false ]; then
        echo "...will create dir ${DIR}"
        if [ "$SKIP_CHOWN" = false ]; then
        echo "...will change ownership to user ${USR} group ${GRP} for dir ${DIR}"
          mkdir -p "$DIR" && sudo chown -R "$USR":"$GRP" "$DIR" && echo "> OK"
        else
          mkdir -p "$DIR" && echo "> OK"
        fi
      elif [ "$IS_OWNER_CHECK_PASSED" = false ] && [ "$SKIP_CHOWN" = false ]; then
        echo "...will change ownership to user ${USR} group ${GRP} for dir ${DIR}"
        sudo chown -R "$USR":"$GRP" "$DIR" && echo "> OK"
      fi
    fi

  done < <(echo "$PERMISSION_LIST")
  return $EXIT_CODE
}

function composeVersion() {
    #Checking whether "set -e" shell option should be restored after Compose version check
    FLAG_SET=false
    if [[ $SHELLOPTS =~ errexit ]]; then
        set +e
        FLAG_SET=true
    fi

    #Checking Compose V1 availablity
    docker-compose version >/dev/null 2>&1
    if [ $? -eq 0 ]; then status_v1=true; else status_v1=false; fi

    #Checking Compose V2 availablity
    docker compose version >/dev/null 2>&1
    if [ $? -eq 0 ]; then status_v2=true; else status_v2=false; fi

    COMPOSE_VERSION=""

    if $status_v2 ; then
        COMPOSE_VERSION="V2"
    elif $status_v1 ; then
        COMPOSE_VERSION="V1"
    else
        echo "Docker Compose plugin is not detected. Please check your environment." >&2
        exit 1
    fi

    echo $COMPOSE_VERSION

    if $FLAG_SET ; then set -e; fi
}
