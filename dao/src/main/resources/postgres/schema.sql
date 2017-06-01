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

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;

SET search_path = public, pg_catalog;
SET default_tablespace = '';
SET default_with_oids = false;

CREATE TABLE IF NOT EXISTS admin_settings (
    id uuid NOT NULL CONSTRAINT admin_settings_pkey PRIMARY KEY,
    json_value jsonb,
    key character varying(255)
);
ALTER TABLE admin_settings OWNER TO postgres;

CREATE TABLE IF NOT EXISTS alarm (
    id uuid NOT NULL CONSTRAINT alarm_pkey PRIMARY KEY,
    ack_ts bigint,
    clear_ts bigint,
    additional_info jsonb,
    end_ts bigint,
    originator_id uuid,
    originator_type integer,
    propagate boolean,
    severity character varying(255),
    start_ts bigint,
    status character varying(255),
    tenant_id uuid,
    type character varying(255)
);
ALTER TABLE alarm OWNER TO postgres;

CREATE TABLE IF NOT EXISTS asset (
    id uuid NOT NULL CONSTRAINT asset_pkey PRIMARY KEY,
    additional_info jsonb,
    customer_id uuid,
    name character varying(255),
    search_text character varying(255),
    tenant_id uuid,
    type character varying(255)
);
ALTER TABLE asset OWNER TO postgres;

CREATE TABLE IF NOT EXISTS component_descriptor (
    id uuid NOT NULL CONSTRAINT component_descriptor_pkey PRIMARY KEY,
    actions character varying(255),
    clazz character varying(255),
    configuration_descriptor jsonb,
    name character varying(255),
    scope character varying(255),
    search_text character varying(255),
    type character varying(255)
);
ALTER TABLE component_descriptor OWNER TO postgres;

CREATE TABLE IF NOT EXISTS customer (
    id uuid NOT NULL CONSTRAINT customer_pkey PRIMARY KEY,
    additional_info jsonb,
    address character varying(255),
    address2 character varying(255),
    city character varying(255),
    country character varying(255),
    email character varying(255),
    phone character varying(255),
    search_text character varying(255),
    state character varying(255),
    tenant_id uuid,
    title character varying(255),
    zip character varying(255)
);
ALTER TABLE customer OWNER TO postgres;

CREATE TABLE IF NOT EXISTS dashboard (
    id uuid NOT NULL CONSTRAINT dashboard_pkey PRIMARY KEY,
    configuration jsonb,
    customer_id uuid,
    search_text character varying(255),
    tenant_id uuid,
    title character varying(255)
);
ALTER TABLE dashboard OWNER TO postgres;

CREATE TABLE IF NOT EXISTS device (
    id uuid NOT NULL CONSTRAINT device_pkey PRIMARY KEY,
    additional_info jsonb,
    customer_id uuid,
    type character varying(255),
    name character varying(255),
    search_text character varying(255),
    tenant_id uuid
);
ALTER TABLE device OWNER TO postgres;

CREATE TABLE IF NOT EXISTS device_credentials (
    id uuid NOT NULL CONSTRAINT device_credentials_pkey PRIMARY KEY,
    credentials_id character varying(255),
    credentials_type character varying(255),
    credentials_value character varying(255),
    device_id uuid
);
ALTER TABLE device_credentials OWNER TO postgres;

CREATE TABLE IF NOT EXISTS event (
    id uuid NOT NULL CONSTRAINT event_pkey PRIMARY KEY,
    body jsonb,
    entity_id uuid,
    entity_type character varying(255),
    event_type character varying(255),
    event_uid character varying(255),
    tenant_id uuid
);
ALTER TABLE event OWNER TO postgres;

CREATE TABLE IF NOT EXISTS plugin (
    id uuid NOT NULL CONSTRAINT plugin_pkey PRIMARY KEY,
    additional_info jsonb,
    api_token character varying(255),
    plugin_class character varying(255),
    configuration jsonb,
    name character varying(255),
    public_access boolean,
    search_text character varying(255),
    state character varying(255),
    tenant_id uuid
);
ALTER TABLE plugin OWNER TO postgres;

CREATE TABLE IF NOT EXISTS rule (
    id uuid NOT NULL CONSTRAINT rule_pkey PRIMARY KEY,
    action jsonb,
    additional_info jsonb,
    filters jsonb,
    name character varying(255),
    plugin_token character varying(255),
    processor jsonb,
    search_text character varying(255),
    state character varying(255),
    tenant_id uuid,
    weight integer
);
ALTER TABLE rule OWNER TO postgres;

CREATE TABLE IF NOT EXISTS tb_user (
    id uuid NOT NULL CONSTRAINT tb_user_pkey PRIMARY KEY,
    additional_info jsonb,
    authority character varying(255),
    customer_id uuid,
    email character varying(255) UNIQUE,
    first_name character varying(255),
    last_name character varying(255),
    search_text character varying(255),
    tenant_id uuid
);
ALTER TABLE tb_user OWNER TO postgres;

CREATE TABLE IF NOT EXISTS tenant (
    id uuid NOT NULL CONSTRAINT tenant_pkey PRIMARY KEY,
    additional_info jsonb,
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
ALTER TABLE tenant OWNER TO postgres;

CREATE TABLE IF NOT EXISTS user_credentials (
    id uuid NOT NULL CONSTRAINT user_credentials_pkey PRIMARY KEY,
    activate_token character varying(255) UNIQUE,
    enabled boolean,
    password character varying(255),
    reset_token character varying(255) UNIQUE,
    user_id uuid UNIQUE
);
ALTER TABLE user_credentials OWNER TO postgres;

CREATE TABLE IF NOT EXISTS widget_type (
    id uuid NOT NULL CONSTRAINT widget_type_pkey PRIMARY KEY,
    alias character varying(255),
    bundle_alias character varying(255),
    descriptor jsonb,
    name character varying(255),
    tenant_id uuid
);
ALTER TABLE widget_type OWNER TO postgres;

CREATE TABLE IF NOT EXISTS widgets_bundle (
    id uuid NOT NULL CONSTRAINT widgets_bundle_pkey PRIMARY KEY,
    alias character varying(255),
    image bytea,
    search_text character varying(255),
    tenant_id uuid,
    title character varying(255)
);
ALTER TABLE widgets_bundle OWNER TO postgres;
