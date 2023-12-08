--
-- Copyright © 2016-2023 The Thingsboard Authors
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

CREATE TABLE IF NOT EXISTS edge (
    id uuid NOT NULL CONSTRAINT edge_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    customer_id uuid,
    root_rule_chain_id uuid,
    type varchar(255),
    name varchar(255),
    label varchar(255),
    routing_key varchar(255),
    secret varchar(255),
    edge_license_key varchar(30),
    cloud_endpoint varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    CONSTRAINT edge_name_unq_key UNIQUE (tenant_id, name),
    CONSTRAINT edge_routing_key_unq_key UNIQUE (routing_key)
    );

CREATE TABLE IF NOT EXISTS edge_event (
    id uuid NOT NULL CONSTRAINT edge_event_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    edge_id uuid,
    edge_event_type varchar(255),
    edge_event_uid varchar(255),
    entity_id uuid,
    edge_event_action varchar(255),
    body varchar(10000000),
    tenant_id uuid,
    ts bigint NOT NULL
    );

CREATE TABLE IF NOT EXISTS resource (
    id uuid NOT NULL CONSTRAINT resource_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    title varchar(255) NOT NULL,
    resource_type varchar(32) NOT NULL,
    resource_key varchar(255) NOT NULL,
    search_text varchar(255),
    file_name varchar(255) NOT NULL,
    data varchar,
    CONSTRAINT resource_unq_key UNIQUE (tenant_id, resource_type, resource_key)
);

CREATE TABLE IF NOT EXISTS ota_package (
    id uuid NOT NULL CONSTRAINT ota_package_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    device_profile_id uuid,
    type varchar(32) NOT NULL,
    title varchar(255) NOT NULL,
    version varchar(255) NOT NULL,
    tag varchar(255),
    url varchar(255),
    file_name varchar(255),
    content_type varchar(255),
    checksum_algorithm varchar(32),
    checksum varchar(1020),
    data oid,
    data_size bigint,
    additional_info varchar,
    search_text varchar(255),
    CONSTRAINT ota_package_tenant_title_version_unq_key UNIQUE (tenant_id, title, version)
);

CREATE TABLE IF NOT EXISTS oauth2_params (
    id uuid NOT NULL CONSTRAINT oauth2_params_pkey PRIMARY KEY,
    enabled boolean,
    tenant_id uuid,
    created_time bigint NOT NULL
);

CREATE TABLE IF NOT EXISTS oauth2_registration (
    id uuid NOT NULL CONSTRAINT oauth2_registration_pkey PRIMARY KEY,
    oauth2_params_id uuid NOT NULL,
    created_time bigint NOT NULL,
    additional_info varchar,
    client_id varchar(255),
    client_secret varchar(2048),
    authorization_uri varchar(255),
    token_uri varchar(255),
    scope varchar(255),
    platforms varchar(255),
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
    custom_send_token boolean,
    CONSTRAINT fk_registration_oauth2_params FOREIGN KEY (oauth2_params_id) REFERENCES oauth2_params(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS oauth2_domain (
    id uuid NOT NULL CONSTRAINT oauth2_domain_pkey PRIMARY KEY,
    oauth2_params_id uuid NOT NULL,
    created_time bigint NOT NULL,
    domain_name varchar(255),
    domain_scheme varchar(31),
    CONSTRAINT fk_domain_oauth2_params FOREIGN KEY (oauth2_params_id) REFERENCES oauth2_params(id) ON DELETE CASCADE,
    CONSTRAINT oauth2_domain_unq_key UNIQUE (oauth2_params_id, domain_name, domain_scheme)
);

CREATE TABLE IF NOT EXISTS oauth2_mobile (
    id uuid NOT NULL CONSTRAINT oauth2_mobile_pkey PRIMARY KEY,
    oauth2_params_id uuid NOT NULL,
    created_time bigint NOT NULL,
    pkg_name varchar(255),
    app_secret varchar(2048),
    CONSTRAINT fk_mobile_oauth2_params FOREIGN KEY (oauth2_params_id) REFERENCES oauth2_params(id) ON DELETE CASCADE,
    CONSTRAINT oauth2_mobile_unq_key UNIQUE (oauth2_params_id, pkg_name)
);

ALTER TABLE dashboard
    ADD COLUMN IF NOT EXISTS image varchar(1000000),
    ADD COLUMN IF NOT EXISTS mobile_hide boolean DEFAULT false,
    ADD COLUMN IF NOT EXISTS mobile_order int;

ALTER TABLE device_profile
    ADD COLUMN IF NOT EXISTS image varchar(1000000),
    ADD COLUMN IF NOT EXISTS firmware_id uuid,
    ADD COLUMN IF NOT EXISTS software_id uuid,
    ADD COLUMN IF NOT EXISTS default_dashboard_id uuid;

ALTER TABLE device
    ADD COLUMN IF NOT EXISTS firmware_id uuid,
    ADD COLUMN IF NOT EXISTS software_id uuid;

ALTER TABLE alarm
    ADD COLUMN IF NOT EXISTS customer_id uuid;

DELETE FROM relation WHERE from_type = 'TENANT' AND relation_type_group = 'RULE_CHAIN';

DO $$
    BEGIN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_firmware_device_profile') THEN
            ALTER TABLE device_profile
                ADD CONSTRAINT fk_firmware_device_profile
                    FOREIGN KEY (firmware_id) REFERENCES ota_package(id);
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_software_device_profile') THEN
            ALTER TABLE device_profile
                ADD CONSTRAINT fk_software_device_profile
                    FOREIGN KEY (firmware_id) REFERENCES ota_package(id);
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_default_dashboard_device_profile') THEN
            ALTER TABLE device_profile
                ADD CONSTRAINT fk_default_dashboard_device_profile
                    FOREIGN KEY (default_dashboard_id) REFERENCES dashboard(id);
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_firmware_device') THEN
            ALTER TABLE device
                ADD CONSTRAINT fk_firmware_device
                    FOREIGN KEY (firmware_id) REFERENCES ota_package(id);
        END IF;

        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_software_device') THEN
            ALTER TABLE device
                ADD CONSTRAINT fk_software_device
                    FOREIGN KEY (firmware_id) REFERENCES ota_package(id);
        END IF;
    END;
$$;


ALTER TABLE api_usage_state
    ADD COLUMN IF NOT EXISTS alarm_exec VARCHAR(32);
UPDATE api_usage_state SET alarm_exec = 'ENABLED' WHERE alarm_exec IS NULL;

CREATE TABLE IF NOT EXISTS rpc (
    id uuid NOT NULL CONSTRAINT rpc_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    device_id uuid NOT NULL,
    expiration_time bigint NOT NULL,
    request varchar(10000000) NOT NULL,
    response varchar(10000000),
    additional_info varchar(10000000),
    status varchar(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_rpc_tenant_id_device_id ON rpc(tenant_id, device_id);
