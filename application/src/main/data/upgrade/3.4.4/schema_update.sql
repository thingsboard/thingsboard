--
-- Copyright © 2016-2022 The Thingsboard Authors
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

CREATE TABLE IF NOT EXISTS alarm_rule (
    id uuid NOT NULL CONSTRAINT alarm_rule_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    alarm_type varchar(255),
    name varchar(255),
    enabled boolean,
    configuration jsonb,
    description varchar,
    CONSTRAINT alarm_rule_name_unq_key UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS alarm_rule_entity_state (
    tenant_id uuid NOT NULL,
    entity_id uuid NOT NULL,
    entity_type varchar(255) NOT NULL,
    data varchar(16384),
    CONSTRAINT entity_state_pkey PRIMARY KEY (entity_id)
);
