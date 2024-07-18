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

set -e #exit if any line failed

version=001
registry_src=docker.io
image_src=redislabs/redisgears:1.2.5
registry=docker.io
image=$image_src-$version

echo Building image $image ...

docker pull $registry_src/$image_src

docker build -t $image .

docker tag $image $registry/$image

read -n 1 -p "Press any key to push to $registry/$image or ^C to stop... "

# docker push $registry/$image

echo Build completed.

