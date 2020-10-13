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

CREATE TABLE IF NOT EXISTS oauth2_client_registration_info (
    id uuid NOT NULL CONSTRAINT oauth2_client_registration_info_pkey PRIMARY KEY,
    enabled boolean,
    created_time bigint NOT NULL,
    additional_info varchar,
    client_id varchar(255),
    client_secret varchar(255),
    authorization_uri varchar(255),
    token_uri varchar(255),
    scope varchar(255),
    user_info_uri varchar(255),
    user_name_attribute_name varchar(255),
    jwk_set_uri varchar(255),
    client_authentication_method varchar(255),
    login_button_label varchar(255),
    login_button_icon varchar(255),
    allow_user_creation boolean,
    activate_user boolean,
    type varchar(31),
    basic_email_attribute_key varchar(31),
    basic_first_name_attribute_key varchar(31),
    basic_last_name_attribute_key varchar(31),
    basic_tenant_name_strategy varchar(31),
    basic_tenant_name_pattern varchar(255),
    basic_customer_name_pattern varchar(255),
    basic_default_dashboard_name varchar(255),
    basic_always_full_screen boolean,
    custom_url varchar(255),
    custom_username varchar(255),
    custom_password varchar(255),
    custom_send_token boolean
);

CREATE TABLE IF NOT EXISTS oauth2_client_registration (
    id uuid NOT NULL CONSTRAINT oauth2_client_registration_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    domain_name varchar(255),
    domain_scheme varchar(31),
    client_registration_info_id uuid
);

CREATE TABLE IF NOT EXISTS oauth2_client_registration_template (
    id uuid NOT NULL CONSTRAINT oauth2_client_registration_template_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    provider_id varchar(255),
    authorization_uri varchar(255),
    token_uri varchar(255),
    scope varchar(255),
    user_info_uri varchar(255),
    user_name_attribute_name varchar(255),
    jwk_set_uri varchar(255),
    client_authentication_method varchar(255),
    type varchar(31),
    basic_email_attribute_key varchar(31),
    basic_first_name_attribute_key varchar(31),
    basic_last_name_attribute_key varchar(31),
    basic_tenant_name_strategy varchar(31),
    basic_tenant_name_pattern varchar(255),
    basic_customer_name_pattern varchar(255),
    basic_default_dashboard_name varchar(255),
    basic_always_full_screen boolean,
    comment varchar,
    login_button_icon varchar(255),
    login_button_label varchar(255),
    help_link varchar(255),
    CONSTRAINT oauth2_template_provider_id_unq_key UNIQUE (provider_id)
);

CREATE TABLE IF NOT EXISTS device_profile (
    id uuid NOT NULL CONSTRAINT device_profile_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    name varchar(255),
    type varchar(255),
    transport_type varchar(255),
    provision_type varchar(255),
    profile_data jsonb,
    description varchar,
    search_text varchar(255),
    is_default boolean,
    tenant_id uuid,
    default_rule_chain_id uuid,
    provision_device_key varchar,
    CONSTRAINT device_profile_name_unq_key UNIQUE (tenant_id, name),
    CONSTRAINT device_provision_key_unq_key UNIQUE (provision_device_key),
    CONSTRAINT fk_default_rule_chain_device_profile FOREIGN KEY (default_rule_chain_id) REFERENCES rule_chain(id)
);

CREATE TABLE IF NOT EXISTS tenant_profile (
    id uuid NOT NULL CONSTRAINT tenant_profile_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    name varchar(255),
    profile_data jsonb,
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
    UPDATE device as d SET device_profile_id = p.id, device_data = '{"configuration":{"type":"DEFAULT"}, "transportConfiguration":{"type":"DEFAULT"}}'
        FROM
           (SELECT id, tenant_id, name from device_profile) as p
                WHERE d.device_profile_id IS NULL AND p.tenant_id = d.tenant_id AND d.type = p.name;
END;
$$;
