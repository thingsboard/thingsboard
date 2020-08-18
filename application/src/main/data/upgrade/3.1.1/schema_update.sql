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

ALTER TABLE device ADD COLUMN device_profile_id uuid;
ALTER TABLE tenant ADD COLUMN tenant_profile_id uuid;

CREATE TABLE IF NOT EXISTS device_profile (
  id uuid NOT NULL CONSTRAINT device_profile_pkey PRIMARY KEY,
  created_time bigint NOT NULL,
  name varchar(255),
  profile_data varchar,
  description varchar,
  search_text varchar(255),
  default boolean,
  tenant_id uuid,
  default_rule_chain_id uuid
);

CREATE TABLE IF NOT EXISTS tenant_profile (
  id uuid NOT NULL CONSTRAINT tenant_profile_pkey PRIMARY KEY,
  created_time bigint NOT NULL,
  name varchar(255),
  profile_data varchar,
  description varchar,
  search_text varchar(255),
  default boolean,
  isolated_tb_core boolean,
  isolated_tb_rule_engine boolean
);
