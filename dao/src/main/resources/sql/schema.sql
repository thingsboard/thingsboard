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


CREATE TABLE IF NOT EXISTS admin_settings (
    id character varying(31) NOT NULL CONSTRAINT admin_settings_pkey PRIMARY KEY,
    json_value varchar,
    key character varying(255)
);

CREATE TABLE IF NOT EXISTS alarm (
    id character varying(31) NOT NULL CONSTRAINT alarm_pkey PRIMARY KEY,
    ack_ts bigint,
    clear_ts bigint,
    additional_info varchar,
    end_ts bigint,
    originator_id character varying(31),
    originator_type integer,
    propagate boolean,
    severity character varying(255),
    start_ts bigint,
    status character varying(255),
    tenant_id character varying(31),
    type character varying(255)
);

CREATE TABLE IF NOT EXISTS asset (
    id character varying(31) NOT NULL CONSTRAINT asset_pkey PRIMARY KEY,
    additional_info varchar,
    customer_id character varying(31),
    name character varying(255),
    search_text character varying(255),
    tenant_id character varying(31),
    type character varying(255)
);

CREATE TABLE IF NOT EXISTS attribute_kv (
  entity_type character varying(255),
  entity_id character varying(31),
  attribute_type character varying(255),
  attribute_key character varying(255),
  bool_v boolean,
  str_v character varying(255),
  long_v bigint,
  dbl_v double precision,
  last_update_ts bigint,
  CONSTRAINT attribute_kv_unq_key UNIQUE (entity_type, entity_id, attribute_type, attribute_key)
);

CREATE TABLE IF NOT EXISTS component_descriptor (
    id character varying(31) NOT NULL CONSTRAINT component_descriptor_pkey PRIMARY KEY,
    actions character varying(255),
    clazz character varying(255),
    configuration_descriptor varchar,
    name character varying(255),
    scope character varying(255),
    search_text character varying(255),
    type character varying(255)
);

CREATE TABLE IF NOT EXISTS customer (
    id character varying(31) NOT NULL CONSTRAINT customer_pkey PRIMARY KEY,
    additional_info varchar,
    address character varying(255),
    address2 character varying(255),
    city character varying(255),
    country character varying(255),
    email character varying(255),
    phone character varying(255),
    search_text character varying(255),
    state character varying(255),
    tenant_id character varying(31),
    title character varying(255),
    zip character varying(255)
);

CREATE TABLE IF NOT EXISTS dashboard (
    id character varying(31) NOT NULL CONSTRAINT dashboard_pkey PRIMARY KEY,
    configuration varchar,
    customer_id character varying(31),
    search_text character varying(255),
    tenant_id character varying(31),
    title character varying(255)
);

CREATE TABLE IF NOT EXISTS device (
    id character varying(31) NOT NULL CONSTRAINT device_pkey PRIMARY KEY,
    additional_info varchar,
    customer_id character varying(31),
    type character varying(255),
    name character varying(255),
    search_text character varying(255),
    tenant_id character varying(31)
);

CREATE TABLE IF NOT EXISTS device_credentials (
    id character varying(31) NOT NULL CONSTRAINT device_credentials_pkey PRIMARY KEY,
    credentials_id character varying(255),
    credentials_type character varying(255),
    credentials_value character varying(255),
    device_id character varying(31)
);

CREATE TABLE IF NOT EXISTS event (
    id character varying(31) NOT NULL CONSTRAINT event_pkey PRIMARY KEY,
    body varchar,
    entity_id character varying(31),
    entity_type character varying(255),
    event_type character varying(255),
    event_uid character varying(255),
    tenant_id character varying(31),
    CONSTRAINT event_unq_key UNIQUE (tenant_id, entity_type, entity_id, event_type, event_uid)
);

CREATE TABLE IF NOT EXISTS plugin (
    id character varying(31) NOT NULL CONSTRAINT plugin_pkey PRIMARY KEY,
    additional_info varchar,
    api_token character varying(255),
    plugin_class character varying(255),
    configuration varchar,
    name character varying(255),
    public_access boolean,
    search_text character varying(255),
    state character varying(255),
    tenant_id character varying(31)
);

CREATE TABLE IF NOT EXISTS relation (
    from_id character varying(31),
    from_type character varying(255),
    to_id character varying(31),
    to_type character varying(255),
    relation_type_group character varying(255),
    relation_type character varying(255),
    additional_info varchar,
    CONSTRAINT relation_unq_key UNIQUE (from_id, from_type, relation_type_group, relation_type, to_id, to_type)
);

CREATE TABLE IF NOT EXISTS rule (
    id character varying(31) NOT NULL CONSTRAINT rule_pkey PRIMARY KEY,
    action varchar,
    additional_info varchar,
    filters varchar,
    name character varying(255),
    plugin_token character varying(255),
    processor varchar,
    search_text character varying(255),
    state character varying(255),
    tenant_id character varying(31),
    weight integer
);

CREATE TABLE IF NOT EXISTS tb_user (
    id character varying(31) NOT NULL CONSTRAINT tb_user_pkey PRIMARY KEY,
    additional_info varchar,
    authority character varying(255),
    customer_id character varying(31),
    email character varying(255) UNIQUE,
    first_name character varying(255),
    last_name character varying(255),
    search_text character varying(255),
    tenant_id character varying(31)
);

CREATE TABLE IF NOT EXISTS tenant (
    id character varying(31) NOT NULL CONSTRAINT tenant_pkey PRIMARY KEY,
    additional_info varchar,
    address character varying(255),
    address2 character varying(255),
    city character varying(255),
    country character varying(255),
    email character varying(255),
    phone character varying(255),
    region character varying(255),
    search_text character varying(255),
    state character varying(255),
    title character varying(255),
    zip character varying(255)
);

CREATE TABLE IF NOT EXISTS ts_kv (
    entity_type character varying(255) NOT NULL,
    entity_id character varying(31) NOT NULL,
    key character varying(255) NOT NULL,
    ts bigint NOT NULL,
    bool_v boolean,
    str_v character varying(255),
    long_v bigint,
    dbl_v double precision,
    CONSTRAINT ts_kv_unq_key UNIQUE (entity_type, entity_id, key, ts)
);

CREATE TABLE IF NOT EXISTS ts_kv_latest (
    entity_type character varying(255) NOT NULL,
    entity_id character varying(31) NOT NULL,
    key character varying(255) NOT NULL,
    ts bigint NOT NULL,
    bool_v boolean,
    str_v character varying(255),
    long_v bigint,
    dbl_v double precision,
    CONSTRAINT ts_kv_latest_unq_key UNIQUE (entity_type, entity_id, key)
);

CREATE TABLE IF NOT EXISTS user_credentials (
    id character varying(31) NOT NULL CONSTRAINT user_credentials_pkey PRIMARY KEY,
    activate_token character varying(255) UNIQUE,
    enabled boolean,
    password character varying(255),
    reset_token character varying(255) UNIQUE,
    user_id character varying(31) UNIQUE
);

CREATE TABLE IF NOT EXISTS widget_type (
    id character varying(31) NOT NULL CONSTRAINT widget_type_pkey PRIMARY KEY,
    alias character varying(255),
    bundle_alias character varying(255),
    descriptor varchar(1000000),
    name character varying(255),
    tenant_id character varying(31)
);

CREATE TABLE IF NOT EXISTS widgets_bundle (
    id character varying(31) NOT NULL CONSTRAINT widgets_bundle_pkey PRIMARY KEY,
    alias character varying(255),
    search_text character varying(255),
    tenant_id character varying(31),
    title character varying(255)
);