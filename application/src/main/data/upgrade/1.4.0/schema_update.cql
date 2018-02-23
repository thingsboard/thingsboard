--
-- Copyright © 2016-2017 The Thingsboard Authors
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

CREATE TABLE IF NOT EXISTS thingsboard.audit_log_by_entity_id (
  tenant_id timeuuid,
  id timeuuid,
  customer_id timeuuid,
  entity_id timeuuid,
  entity_type text,
  entity_name text,
  user_id timeuuid,
  user_name text,
  action_type text,
  action_data text,
  action_status text,
  action_failure_details text,
  PRIMARY KEY ((tenant_id, entity_id, entity_type), id)
);

CREATE TABLE IF NOT EXISTS thingsboard.audit_log_by_customer_id (
  tenant_id timeuuid,
  id timeuuid,
  customer_id timeuuid,
  entity_id timeuuid,
  entity_type text,
  entity_name text,
  user_id timeuuid,
  user_name text,
  action_type text,
  action_data text,
  action_status text,
  action_failure_details text,
  PRIMARY KEY ((tenant_id, customer_id), id)
);

CREATE TABLE IF NOT EXISTS thingsboard.audit_log_by_user_id (
  tenant_id timeuuid,
  id timeuuid,
  customer_id timeuuid,
  entity_id timeuuid,
  entity_type text,
  entity_name text,
  user_id timeuuid,
  user_name text,
  action_type text,
  action_data text,
  action_status text,
  action_failure_details text,
  PRIMARY KEY ((tenant_id, user_id), id)
);



CREATE TABLE IF NOT EXISTS thingsboard.audit_log_by_tenant_id (
  tenant_id timeuuid,
  id timeuuid,
  partition bigint,
  customer_id timeuuid,
  entity_id timeuuid,
  entity_type text,
  entity_name text,
  user_id timeuuid,
  user_name text,
  action_type text,
  action_data text,
  action_status text,
  action_failure_details text,
  PRIMARY KEY ((tenant_id, partition), id)
);

CREATE TABLE IF NOT EXISTS thingsboard.audit_log_by_tenant_id_partitions (
  tenant_id timeuuid,
  partition bigint,
  PRIMARY KEY (( tenant_id ), partition)
) WITH CLUSTERING ORDER BY ( partition ASC )
AND compaction = { 'class' :  'LeveledCompactionStrategy'  };
