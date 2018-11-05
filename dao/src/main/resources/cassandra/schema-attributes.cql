--
-- Copyright © 2016-2018 The Thingsboard Authors
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE TABLE IF NOT EXISTS thingsboard.attributes_kv_cf (
    entity_type text, // (DEVICE, CUSTOMER, TENANT)
    entity_id timeuuid,
    attribute_type text, // (CLIENT_SIDE, SHARED, SERVER_SIDE)
    attribute_key text,
    bool_v boolean,
    str_v text,
    long_v bigint,
    dbl_v double,
    last_update_ts bigint,
    PRIMARY KEY ((entity_type, entity_id, attribute_type), attribute_key)
) WITH compaction = { 'class' :  'LeveledCompactionStrategy'  };