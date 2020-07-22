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


CREATE TABLE IF NOT EXISTS admin_settings (
    id uuid NOT NULL CONSTRAINT admin_settings_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    json_value varchar,
    key varchar(255)
);

CREATE TABLE IF NOT EXISTS alarm (
    id uuid NOT NULL CONSTRAINT alarm_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    ack_ts bigint,
    clear_ts bigint,
    additional_info varchar,
    end_ts bigint,
    originator_id uuid,
    originator_type integer,
    propagate boolean,
    severity varchar(255),
    start_ts bigint,
    status varchar(255),
    tenant_id uuid,
    propagate_relation_types varchar,
    type varchar(255)
);

CREATE TABLE IF NOT EXISTS asset (
    id uuid NOT NULL CONSTRAINT asset_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    customer_id uuid,
    name varchar(255),
    label varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    type varchar(255),
    CONSTRAINT asset_name_unq_key UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS audit_log (
    id uuid NOT NULL CONSTRAINT audit_log_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid,
    customer_id uuid,
    entity_id uuid,
    entity_type varchar(255),
    entity_name varchar(255),
    user_id uuid,
    user_name varchar(255),
    action_type varchar(255),
    action_data varchar(1000000),
    action_status varchar(255),
    action_failure_details varchar(1000000)
);

CREATE TABLE IF NOT EXISTS attribute_kv (
  entity_type varchar(255),
  entity_id uuid,
  attribute_type varchar(255),
  attribute_key varchar(255),
  bool_v boolean,
  str_v varchar(10000000),
  long_v bigint,
  dbl_v double precision,
  json_v varchar(10000000),
  last_update_ts bigint,
  CONSTRAINT attribute_kv_pkey PRIMARY KEY (entity_type, entity_id, attribute_type, attribute_key)
);

CREATE TABLE IF NOT EXISTS component_descriptor (
    id uuid NOT NULL CONSTRAINT component_descriptor_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    actions varchar(255),
    clazz varchar UNIQUE,
    configuration_descriptor varchar,
    name varchar(255),
    scope varchar(255),
    search_text varchar(255),
    type varchar(255)
);

CREATE TABLE IF NOT EXISTS customer (
    id uuid NOT NULL CONSTRAINT customer_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    address varchar,
    address2 varchar,
    city varchar(255),
    country varchar(255),
    email varchar(255),
    phone varchar(255),
    search_text varchar(255),
    state varchar(255),
    tenant_id uuid,
    title varchar(255),
    zip varchar(255)
);

CREATE TABLE IF NOT EXISTS dashboard (
    id uuid NOT NULL CONSTRAINT dashboard_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    configuration varchar(10000000),
    assigned_customers varchar(1000000),
    search_text varchar(255),
    tenant_id uuid,
    title varchar(255)
);

CREATE TABLE IF NOT EXISTS device (
    id uuid NOT NULL CONSTRAINT device_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    customer_id uuid,
    type varchar(255),
    name varchar(255),
    label varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    CONSTRAINT device_name_unq_key UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS device_credentials (
    id uuid NOT NULL CONSTRAINT device_credentials_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    credentials_id varchar,
    credentials_type varchar(255),
    credentials_value varchar,
    device_id uuid,
    CONSTRAINT device_credentials_id_unq_key UNIQUE (credentials_id),
    CONSTRAINT device_credentials_device_id_unq_key UNIQUE (device_id)
);

CREATE TABLE IF NOT EXISTS event (
    id uuid NOT NULL CONSTRAINT event_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    body varchar(10000000),
    entity_id uuid,
    entity_type varchar(255),
    event_type varchar(255),
    event_uid varchar(255),
    tenant_id uuid,
    ts bigint NOT NULL,
    CONSTRAINT event_unq_key UNIQUE (tenant_id, entity_type, entity_id, event_type, event_uid)
);

CREATE TABLE IF NOT EXISTS relation (
    from_id uuid,
    from_type varchar(255),
    to_id uuid,
    to_type varchar(255),
    relation_type_group varchar(255),
    relation_type varchar(255),
    additional_info varchar,
    CONSTRAINT relation_pkey PRIMARY KEY (from_id, from_type, relation_type_group, relation_type, to_id, to_type)
);

CREATE TABLE IF NOT EXISTS tb_user (
    id uuid NOT NULL CONSTRAINT tb_user_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    authority varchar(255),
    customer_id uuid,
    email varchar(255) UNIQUE,
    first_name varchar(255),
    last_name varchar(255),
    search_text varchar(255),
    tenant_id uuid
);

CREATE TABLE IF NOT EXISTS tenant (
    id uuid NOT NULL CONSTRAINT tenant_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    address varchar,
    address2 varchar,
    city varchar(255),
    country varchar(255),
    email varchar(255),
    phone varchar(255),
    region varchar(255),
    search_text varchar(255),
    state varchar(255),
    title varchar(255),
    zip varchar(255),
    isolated_tb_core boolean,
    isolated_tb_rule_engine boolean
);

CREATE TABLE IF NOT EXISTS user_credentials (
    id uuid NOT NULL CONSTRAINT user_credentials_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    activate_token varchar(255) UNIQUE,
    enabled boolean,
    password varchar(255),
    reset_token varchar(255) UNIQUE,
    user_id uuid UNIQUE
);

CREATE TABLE IF NOT EXISTS widget_type (
    id uuid NOT NULL CONSTRAINT widget_type_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    alias varchar(255),
    bundle_alias varchar(255),
    descriptor varchar(1000000),
    name varchar(255),
    tenant_id uuid
);

CREATE TABLE IF NOT EXISTS widgets_bundle (
    id uuid NOT NULL CONSTRAINT widgets_bundle_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    alias varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    title varchar(255)
);

CREATE TABLE IF NOT EXISTS rule_chain (
    id uuid NOT NULL CONSTRAINT rule_chain_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    configuration varchar(10000000),
    name varchar(255),
    first_rule_node_id uuid,
    root boolean,
    debug_mode boolean,
    search_text varchar(255),
    tenant_id uuid
);

CREATE TABLE IF NOT EXISTS rule_node (
    id uuid NOT NULL CONSTRAINT rule_node_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    rule_chain_id uuid,
    additional_info varchar,
    configuration varchar(10000000),
    type varchar(255),
    name varchar(255),
    debug_mode boolean,
    search_text varchar(255)
);

CREATE TABLE IF NOT EXISTS entity_view (
    id uuid NOT NULL CONSTRAINT entity_view_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    entity_id uuid,
    entity_type varchar(255),
    tenant_id uuid,
    customer_id uuid,
    type varchar(255),
    name varchar(255),
    keys varchar(10000000),
    start_ts bigint,
    end_ts bigint,
    search_text varchar(255),
    additional_info varchar
);

