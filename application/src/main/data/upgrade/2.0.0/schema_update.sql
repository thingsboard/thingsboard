--
-- Copyright © 2016-2021 The Thingsboard Authors
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

CREATE TABLE IF NOT EXISTS rule_chain (
    id varchar(31) NOT NULL CONSTRAINT rule_chain_pkey PRIMARY KEY,
    additional_info varchar,
    configuration varchar(10000000),
    name varchar(255),
    first_rule_node_id varchar(31),
    root boolean,
    debug_mode boolean,
    search_text varchar(255),
    tenant_id varchar(31)
);

CREATE TABLE IF NOT EXISTS rule_node (
    id varchar(31) NOT NULL CONSTRAINT rule_node_pkey PRIMARY KEY,
    rule_chain_id varchar(31),
    additional_info varchar,
    configuration varchar(10000000),
    type varchar(255),
    name varchar(255),
    debug_mode boolean,
    search_text varchar(255)
);

DROP TABLE rule;
DROP TABLE plugin;

DELETE FROM alarm WHERE originator_type = 3 OR originator_type = 4;
UPDATE alarm SET originator_type = (originator_type - 2) where  originator_type > 2;
