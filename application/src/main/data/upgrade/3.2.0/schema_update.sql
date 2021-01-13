--
-- Copyright © 2016-2020 The Thingsboard Authors
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

CREATE TABLE IF NOT EXISTS entity_config (
       id uuid NOT NULL CONSTRAINT entity_config_pkey PRIMARY KEY,
       created_time bigint NOT NULL,
       entity_id uuid,
       entity_type varchar(255),
       tenant_id uuid,
       version bigint,
       configuration varchar(10000000),
       additional_info varchar
);