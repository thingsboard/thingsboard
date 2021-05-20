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

set -e

source .env

kubectl apply -f common/tb-namespace.yml
kubectl config set-context $(kubectl config current-context) --namespace=thingsboard

kubectl apply -f $DEPLOYMENT_TYPE/thirdparty.yml


if [ "$DEPLOYMENT_TYPE" == "high-availability" ]; then
    echo -n "waiting for all redis pods to be ready";
    while [[ $(kubectl get pods tb-redis-5 -o 'jsonpath={..status.conditions[?(@.type=="Ready")].status}' 2>/dev/null) != "True" ]];
    do
      echo -n "." && sleep 5;
    done

    if [[ $(kubectl exec -it tb-redis-0 -- redis-cli cluster info 2>&1 | head -n 1) =~ "cluster_state:ok" ]]
    then
      echo "redis cluster is already configured"
    else
      echo "starting redis cluster"
      redisNodes=$(kubectl get pods -l app=tb-redis -o jsonpath='{range.items[*]}{.status.podIP}:6379 ')
      kubectl exec -it tb-redis-0 -- redis-cli --cluster create --cluster-replicas 1 $redisNodes
    fi

fi

