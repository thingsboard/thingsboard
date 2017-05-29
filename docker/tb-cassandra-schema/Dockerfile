#
# Copyright © 2016-2017 The Thingsboard Authors
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

FROM cassandra:3.9

ADD install-schema.sh /root/install-schema.sh

RUN apt-get update \
        && apt-get install -y nmap

RUN chmod +x /root/install-schema.sh

ADD schema.cql /root/schema.cql
ADD demo-data.cql /root/demo-data.cql
ADD system-data.cql /root/system-data.cql

WORKDIR /root
