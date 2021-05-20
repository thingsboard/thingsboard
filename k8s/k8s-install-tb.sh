#!/bin/bash
#
# Copyright © 2016-2021 The Thingsboard Authors
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

function installTb() {

    loadDemo=$1

    kubectl apply -f common/tb-node-configmap.yml
    kubectl apply -f common/database-setup.yml &&
    kubectl wait --for=condition=Ready pod/tb-db-setup --timeout=120s &&
    kubectl exec tb-db-setup -- sh -c 'export INSTALL_TB=true; export LOAD_DEMO='"$loadDemo"'; start-tb-node.sh; touch /tmp/install-finished;'

    kubectl delete pod tb-db-setup

}

function installPostgres() {

    kubectl apply -f common/postgres.yml
    kubectl apply -f common/tb-node-postgres-configmap.yml

    kubectl rollout status deployment/postgres
}

function installHybrid() {

    kubectl apply -f common/postgres.yml
    kubectl apply -f common/cassandra.yml
    kubectl apply -f common/tb-node-hybrid-configmap.yml

    kubectl rollout status deployment/postgres
    kubectl rollout status statefulset/cassandra

    kubectl exec -it cassandra-0 -- bash -c "cqlsh -e \
                    \"CREATE KEYSPACE IF NOT EXISTS thingsboard \
                    WITH replication = { \
                        'class' : 'SimpleStrategy', \
                        'replication_factor' : 1 \
                    };\""
}

while [[ $# -gt 0 ]]
do
key="$1"

case $key in
    --loadDemo)
    LOAD_DEMO=true
    shift # past argument
    ;;
    *)
            # unknown option
    ;;
esac
shift # past argument or value
done

if [ "$LOAD_DEMO" == "true" ]; then
    loadDemo=true
else
    loadDemo=false
fi

source .env

kubectl apply -f common/tb-namespace.yml
kubectl config set-context $(kubectl config current-context) --namespace=thingsboard

case $DEPLOYMENT_TYPE in
        basic)
        ;;
        high-availability)
        ;;
        *)
        echo "Unknown DEPLOYMENT_TYPE value specified: '${DEPLOYMENT_TYPE}'. Should be either basic or high-availability." >&2
        exit 1
esac

case $DATABASE in
        postgres)
            installPostgres
            installTb ${loadDemo}
        ;;
        hybrid)
            installHybrid
            installTb ${loadDemo}
        ;;
        *)
        echo "Unknown DATABASE value specified: '${DATABASE}'. Should be either postgres or hybrid." >&2
        exit 1
esac

