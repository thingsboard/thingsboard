#!/bin/bash
#
# Copyright © 2016-2020 The Thingsboard Authors
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

if [[ -z "${FROM_VERSION// }" ]]; then
    echo "--fromVersion parameter is invalid or unspecified!"
    echo "Usage: k8s-upgrade-tb.sh --fromVersion={VERSION}"
    exit 1
else
    fromVersion="${FROM_VERSION// }"
fi

kubectl apply -f common/database-setup.yml &&
kubectl wait --for=condition=Ready pod/tb-db-setup --timeout=120s &&
kubectl exec tb-db-setup -- sh -c 'export UPGRADE_TB=true; export FROM_VERSION='"$fromVersion"'; start-tb-node.sh; touch /tmp/install-finished;'

kubectl delete pod tb-db-setup
