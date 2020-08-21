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

CREATE TABLE IF NOT EXISTS device_profile (
  id uuid NOT NULL CONSTRAINT device_profile_pkey PRIMARY KEY,
  created_time bigint NOT NULL,
  name varchar(255),
  type varchar(255),
  profile_data varchar,
  description varchar,
  search_text varchar(255),
  is_default boolean,
  tenant_id uuid,
  default_rule_chain_id uuid,
  CONSTRAINT device_profile_name_unq_key UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS tenant_profile (
  id uuid NOT NULL CONSTRAINT tenant_profile_pkey PRIMARY KEY,
  created_time bigint NOT NULL,
  name varchar(255),
  profile_data varchar,
  description varchar,
  search_text varchar(255),
  is_default boolean,
  isolated_tb_core boolean,
  isolated_tb_rule_engine boolean,
  CONSTRAINT tenant_profile_name_unq_key UNIQUE (name)
);

CREATE OR REPLACE PROCEDURE update_tenant_profiles()
    LANGUAGE plpgsql AS
$$
BEGIN
    UPDATE tenant as t SET tenant_profile_id = p.id
    FROM
        (SELECT id from tenant_profile WHERE isolated_tb_core = false AND isolated_tb_rule_engine = false) as p
    WHERE t.tenant_profile_id IS NULL AND t.isolated_tb_core = false AND t.isolated_tb_rule_engine = false;

    UPDATE tenant as t SET tenant_profile_id = p.id
    FROM
        (SELECT id from tenant_profile WHERE isolated_tb_core = true AND isolated_tb_rule_engine = false) as p
    WHERE t.tenant_profile_id IS NULL AND t.isolated_tb_core = true AND t.isolated_tb_rule_engine = false;

    UPDATE tenant as t SET tenant_profile_id = p.id
    FROM
        (SELECT id from tenant_profile WHERE isolated_tb_core = false AND isolated_tb_rule_engine = true) as p
    WHERE t.tenant_profile_id IS NULL AND t.isolated_tb_core = false AND t.isolated_tb_rule_engine = true;

    UPDATE tenant as t SET tenant_profile_id = p.id
    FROM
        (SELECT id from tenant_profile WHERE isolated_tb_core = true AND isolated_tb_rule_engine = true) as p
    WHERE t.tenant_profile_id IS NULL AND t.isolated_tb_core = true AND t.isolated_tb_rule_engine = true;
END;
$$;

CREATE OR REPLACE PROCEDURE update_device_profiles()
    LANGUAGE plpgsql AS
$$
BEGIN
    UPDATE device as d SET device_profile_id = p.id, device_data = '{"configuration":{"type":"DEFAULT"}}'
        FROM
           (SELECT id, tenant_id from device_profile WHERE is_default = true) as p
                WHERE d.device_profile_id IS NULL AND p.tenant_id = d.tenant_id;
END;
$$;
