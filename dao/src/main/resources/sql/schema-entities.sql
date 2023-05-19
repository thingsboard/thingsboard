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

CREATE TABLE IF NOT EXISTS tb_schema_settings
(
    schema_version bigint NOT NULL,
    CONSTRAINT tb_schema_settings_pkey PRIMARY KEY (schema_version)
);

CREATE OR REPLACE PROCEDURE insert_tb_schema_settings()
    LANGUAGE plpgsql AS
$$
BEGIN
    IF (SELECT COUNT(*) FROM tb_schema_settings) = 0 THEN
        INSERT INTO tb_schema_settings (schema_version) VALUES (3003000);
    END IF;
END;
$$;

call insert_tb_schema_settings();

CREATE TABLE IF NOT EXISTS admin_settings (
    id uuid NOT NULL CONSTRAINT admin_settings_pkey PRIMARY KEY,
    tenant_id uuid NOT NULL,
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
    assign_ts bigint DEFAULT 0,
    assignee_id uuid,
    tenant_id uuid,
    customer_id uuid,
    propagate_relation_types varchar,
    type varchar(255),
    propagate_to_owner boolean,
    propagate_to_tenant boolean,
    acknowledged boolean,
    cleared boolean
);

CREATE TABLE IF NOT EXISTS alarm_comment (
    id uuid NOT NULL,
    created_time bigint NOT NULL,
    alarm_id uuid NOT NULL,
    user_id uuid,
    type varchar(255) NOT NULL,
    comment varchar(10000),
    CONSTRAINT fk_alarm_comment_alarm_id FOREIGN KEY (alarm_id) REFERENCES alarm(id) ON DELETE CASCADE
) PARTITION BY RANGE (created_time);

CREATE TABLE IF NOT EXISTS entity_alarm (
    tenant_id uuid NOT NULL,
    entity_type varchar(32),
    entity_id uuid NOT NULL,
    created_time bigint NOT NULL,
    alarm_type varchar(255) NOT NULL,
    customer_id uuid,
    alarm_id uuid,
    CONSTRAINT entity_alarm_pkey PRIMARY KEY (entity_id, alarm_id),
    CONSTRAINT fk_entity_alarm_id FOREIGN KEY (alarm_id) REFERENCES alarm(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_log (
    id uuid NOT NULL,
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
) PARTITION BY RANGE (created_time);

CREATE TABLE IF NOT EXISTS attribute_kv (
  entity_type varchar(255),
  entity_id uuid,
  attribute_type varchar(255),
  attribute_key varchar(255),
  bool_v boolean,
  str_v varchar(10000000),
  long_v bigint,
  dbl_v double precision,
  json_v json,
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
    type varchar(255),
    clustering_mode varchar(255)
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
    zip varchar(255),
    external_id uuid,
    CONSTRAINT customer_external_id_unq_key UNIQUE (tenant_id, external_id)
);

CREATE TABLE IF NOT EXISTS dashboard (
    id uuid NOT NULL CONSTRAINT dashboard_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    configuration varchar,
    assigned_customers varchar(1000000),
    search_text varchar(255),
    tenant_id uuid,
    title varchar(255),
    mobile_hide boolean DEFAULT false,
    mobile_order int,
    image varchar(1000000),
    external_id uuid,
    CONSTRAINT dashboard_external_id_unq_key UNIQUE (tenant_id, external_id)
);

CREATE TABLE IF NOT EXISTS rule_chain (
    id uuid NOT NULL CONSTRAINT rule_chain_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    configuration varchar(10000000),
    name varchar(255),
    type varchar(255),
    first_rule_node_id uuid,
    root boolean,
    debug_mode boolean,
    search_text varchar(255),
    tenant_id uuid,
    external_id uuid,
    CONSTRAINT rule_chain_external_id_unq_key UNIQUE (tenant_id, external_id)
);

CREATE TABLE IF NOT EXISTS rule_node (
    id uuid NOT NULL CONSTRAINT rule_node_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    rule_chain_id uuid,
    additional_info varchar,
    configuration_version int DEFAULT 0,
    configuration varchar(10000000),
    type varchar(255),
    name varchar(255),
    debug_mode boolean,
    singleton_mode boolean,
    search_text varchar(255),
    external_id uuid
);

CREATE TABLE IF NOT EXISTS rule_node_state (
    id uuid NOT NULL CONSTRAINT rule_node_state_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    rule_node_id uuid NOT NULL,
    entity_type varchar(32) NOT NULL,
    entity_id uuid NOT NULL,
    state_data varchar(16384) NOT NULL,
    CONSTRAINT rule_node_state_unq_key UNIQUE (rule_node_id, entity_id),
    CONSTRAINT fk_rule_node_state_node_id FOREIGN KEY (rule_node_id) REFERENCES rule_node(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ota_package (
    id uuid NOT NULL CONSTRAINT ota_package_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    device_profile_id uuid ,
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

CREATE TABLE IF NOT EXISTS queue (
    id uuid NOT NULL CONSTRAINT queue_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid,
    name varchar(255),
    topic varchar(255),
    poll_interval int,
    partitions int,
    consumer_per_partition boolean,
    pack_processing_timeout bigint,
    submit_strategy varchar(255),
    processing_strategy varchar(255),
    additional_info varchar
);

CREATE TABLE IF NOT EXISTS asset_profile (
    id uuid NOT NULL CONSTRAINT asset_profile_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    name varchar(255),
    image varchar(1000000),
    description varchar,
    search_text varchar(255),
    is_default boolean,
    tenant_id uuid,
    default_rule_chain_id uuid,
    default_dashboard_id uuid,
    default_queue_name varchar(255),
    default_edge_rule_chain_id uuid,
    external_id uuid,
    CONSTRAINT asset_profile_name_unq_key UNIQUE (tenant_id, name),
    CONSTRAINT asset_profile_external_id_unq_key UNIQUE (tenant_id, external_id),
    CONSTRAINT fk_default_rule_chain_asset_profile FOREIGN KEY (default_rule_chain_id) REFERENCES rule_chain(id),
    CONSTRAINT fk_default_dashboard_asset_profile FOREIGN KEY (default_dashboard_id) REFERENCES dashboard(id),
    CONSTRAINT fk_default_edge_rule_chain_asset_profile FOREIGN KEY (default_edge_rule_chain_id) REFERENCES rule_chain(id)
    );

CREATE TABLE IF NOT EXISTS asset (
    id uuid NOT NULL CONSTRAINT asset_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    customer_id uuid,
    asset_profile_id uuid NOT NULL,
    name varchar(255),
    label varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    type varchar(255),
    external_id uuid,
    CONSTRAINT asset_name_unq_key UNIQUE (tenant_id, name),
    CONSTRAINT asset_external_id_unq_key UNIQUE (tenant_id, external_id),
    CONSTRAINT fk_asset_profile FOREIGN KEY (asset_profile_id) REFERENCES asset_profile(id)
);

CREATE TABLE IF NOT EXISTS device_profile (
    id uuid NOT NULL CONSTRAINT device_profile_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    name varchar(255),
    type varchar(255),
    image varchar(1000000),
    transport_type varchar(255),
    provision_type varchar(255),
    profile_data jsonb,
    description varchar,
    search_text varchar(255),
    is_default boolean,
    tenant_id uuid,
    firmware_id uuid,
    software_id uuid,
    default_rule_chain_id uuid,
    default_dashboard_id uuid,
    default_queue_name varchar(255),
    provision_device_key varchar,
    default_edge_rule_chain_id uuid,
    external_id uuid,
    CONSTRAINT device_profile_name_unq_key UNIQUE (tenant_id, name),
    CONSTRAINT device_provision_key_unq_key UNIQUE (provision_device_key),
    CONSTRAINT device_profile_external_id_unq_key UNIQUE (tenant_id, external_id),
    CONSTRAINT fk_default_rule_chain_device_profile FOREIGN KEY (default_rule_chain_id) REFERENCES rule_chain(id),
    CONSTRAINT fk_default_dashboard_device_profile FOREIGN KEY (default_dashboard_id) REFERENCES dashboard(id),
    CONSTRAINT fk_firmware_device_profile FOREIGN KEY (firmware_id) REFERENCES ota_package(id),
    CONSTRAINT fk_software_device_profile FOREIGN KEY (software_id) REFERENCES ota_package(id),
    CONSTRAINT fk_default_edge_rule_chain_device_profile FOREIGN KEY (default_edge_rule_chain_id) REFERENCES rule_chain(id)
);

DO
$$
    BEGIN
        IF NOT EXISTS(SELECT 1 FROM pg_constraint WHERE conname = 'fk_device_profile_ota_package') THEN
            ALTER TABLE ota_package
                ADD CONSTRAINT fk_device_profile_ota_package
                    FOREIGN KEY (device_profile_id) REFERENCES device_profile (id)
                        ON DELETE CASCADE;
        END IF;
    END;
$$;

-- We will use one-to-many relation in the first release and extend this feature in case of user requests
-- CREATE TABLE IF NOT EXISTS device_profile_firmware (
--     device_profile_id uuid NOT NULL,
--     firmware_id uuid NOT NULL,
--     CONSTRAINT device_profile_firmware_unq_key UNIQUE (device_profile_id, firmware_id),
--     CONSTRAINT fk_device_profile_firmware_device_profile FOREIGN KEY (device_profile_id) REFERENCES device_profile(id) ON DELETE CASCADE,
--     CONSTRAINT fk_device_profile_firmware_firmware FOREIGN KEY (firmware_id) REFERENCES firmware(id) ON DELETE CASCADE,
-- );

CREATE TABLE IF NOT EXISTS device (
    id uuid NOT NULL CONSTRAINT device_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    customer_id uuid,
    device_profile_id uuid NOT NULL,
    device_data jsonb,
    type varchar(255),
    name varchar(255),
    label varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    firmware_id uuid,
    software_id uuid,
    external_id uuid,
    CONSTRAINT device_name_unq_key UNIQUE (tenant_id, name),
    CONSTRAINT device_external_id_unq_key UNIQUE (tenant_id, external_id),
    CONSTRAINT fk_device_profile FOREIGN KEY (device_profile_id) REFERENCES device_profile(id),
    CONSTRAINT fk_firmware_device FOREIGN KEY (firmware_id) REFERENCES ota_package(id),
    CONSTRAINT fk_software_device FOREIGN KEY (software_id) REFERENCES ota_package(id)
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

CREATE TABLE IF NOT EXISTS rule_node_debug_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL ,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar,
    e_type varchar,
    e_entity_id uuid,
    e_entity_type varchar,
    e_msg_id uuid,
    e_msg_type varchar,
    e_data_type varchar,
    e_relation_type varchar,
    e_data varchar,
    e_metadata varchar,
    e_error varchar
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS rule_chain_debug_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_message varchar,
    e_error varchar
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS stats_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_messages_processed bigint NOT NULL,
    e_errors_occurred bigint NOT NULL
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS lc_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_type varchar NOT NULL,
    e_success boolean NOT NULL,
    e_error varchar
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS error_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL,
    service_id varchar NOT NULL,
    e_method varchar NOT NULL,
    e_error varchar
) PARTITION BY RANGE (ts);

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
    phone varchar(255),
    search_text varchar(255),
    tenant_id uuid
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

CREATE TABLE IF NOT EXISTS tenant (
    id uuid NOT NULL CONSTRAINT tenant_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    additional_info varchar,
    tenant_profile_id uuid NOT NULL,
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
    CONSTRAINT fk_tenant_profile FOREIGN KEY (tenant_profile_id) REFERENCES tenant_profile(id)
);

CREATE TABLE IF NOT EXISTS user_credentials (
    id uuid NOT NULL CONSTRAINT user_credentials_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    activate_token varchar(255) UNIQUE,
    enabled boolean,
    password varchar(255),
    reset_token varchar(255) UNIQUE,
    user_id uuid UNIQUE,
    additional_info varchar DEFAULT '{}'
);

CREATE TABLE IF NOT EXISTS widget_type (
    id uuid NOT NULL CONSTRAINT widget_type_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    alias varchar(255),
    bundle_alias varchar(255),
    descriptor varchar(1000000),
    name varchar(255),
    tenant_id uuid,
    image varchar(1000000),
    description varchar(255)
);

CREATE TABLE IF NOT EXISTS widgets_bundle (
    id uuid NOT NULL CONSTRAINT widgets_bundle_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    alias varchar(255),
    search_text varchar(255),
    tenant_id uuid,
    title varchar(255),
    image varchar(1000000),
    description varchar(255),
    external_id uuid,
    CONSTRAINT widgets_bundle_external_id_unq_key UNIQUE (tenant_id, external_id)
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
    additional_info varchar,
    external_id uuid,
    CONSTRAINT entity_view_external_id_unq_key UNIQUE (tenant_id, external_id)
);

CREATE TABLE IF NOT EXISTS ts_kv_latest
(
    entity_id uuid   NOT NULL,
    key       int    NOT NULL,
    ts        bigint NOT NULL,
    bool_v    boolean,
    str_v     varchar(10000000),
    long_v    bigint,
    dbl_v     double precision,
    json_v    json,
    CONSTRAINT ts_kv_latest_pkey PRIMARY KEY (entity_id, key)
);

CREATE TABLE IF NOT EXISTS ts_kv_dictionary
(
    key    varchar(255) NOT NULL,
    key_id serial UNIQUE,
    CONSTRAINT ts_key_id_pkey PRIMARY KEY (key)
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

-- Deprecated
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

-- Deprecated
CREATE TABLE IF NOT EXISTS oauth2_client_registration (
    id uuid NOT NULL CONSTRAINT oauth2_client_registration_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    domain_name varchar(255),
    domain_scheme varchar(31),
    client_registration_info_id uuid
);

CREATE TABLE IF NOT EXISTS api_usage_state (
    id uuid NOT NULL CONSTRAINT usage_record_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid,
    entity_type varchar(32),
    entity_id uuid,
    transport varchar(32),
    db_storage varchar(32),
    re_exec varchar(32),
    js_exec varchar(32),
    email_exec varchar(32),
    sms_exec varchar(32),
    alarm_exec varchar(32),
    CONSTRAINT api_usage_state_unq_key UNIQUE (tenant_id, entity_id)
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
    search_text varchar(255),
    tenant_id uuid,
    CONSTRAINT edge_name_unq_key UNIQUE (tenant_id, name),
    CONSTRAINT edge_routing_key_unq_key UNIQUE (routing_key)
);

CREATE TABLE IF NOT EXISTS edge_event (
    id uuid NOT NULL,
    created_time bigint NOT NULL,
    edge_id uuid,
    edge_event_type varchar(255),
    edge_event_uid varchar(255),
    entity_id uuid,
    edge_event_action varchar(255),
    body varchar(10000000),
    tenant_id uuid,
    ts bigint NOT NULL
) PARTITION BY RANGE(created_time);

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

CREATE OR REPLACE FUNCTION to_uuid(IN entity_id varchar, OUT uuid_id uuid) AS
$$
BEGIN
    uuid_id := substring(entity_id, 8, 8) || '-' || substring(entity_id, 4, 4) || '-1' || substring(entity_id, 1, 3) ||
               '-' || substring(entity_id, 16, 4) || '-' || substring(entity_id, 20, 12);
END;
$$ LANGUAGE plpgsql;


CREATE OR REPLACE PROCEDURE cleanup_edge_events_by_ttl(IN ttl bigint, INOUT deleted bigint)
    LANGUAGE plpgsql AS
$$
DECLARE
    ttl_ts bigint;
    ttl_deleted_count bigint DEFAULT 0;
BEGIN
    IF ttl > 0 THEN
        ttl_ts := (EXTRACT(EPOCH FROM current_timestamp) * 1000 - ttl::bigint * 1000)::bigint;
        EXECUTE format(
                'WITH deleted AS (DELETE FROM edge_event WHERE ts < %L::bigint RETURNING *) SELECT count(*) FROM deleted', ttl_ts) into ttl_deleted_count;
    END IF;
    RAISE NOTICE 'Edge events removed by ttl: %', ttl_deleted_count;
    deleted := ttl_deleted_count;
END
$$;


CREATE TABLE IF NOT EXISTS user_auth_settings (
    id uuid NOT NULL CONSTRAINT user_auth_settings_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    user_id uuid UNIQUE NOT NULL CONSTRAINT fk_user_auth_settings_user_id REFERENCES tb_user(id),
    two_fa_settings varchar
);

CREATE TABLE IF NOT EXISTS notification_target (
    id UUID NOT NULL CONSTRAINT notification_target_pkey PRIMARY KEY,
    created_time BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    configuration VARCHAR(10000) NOT NULL,
    CONSTRAINT uq_notification_target_name UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS notification_template (
    id UUID NOT NULL CONSTRAINT notification_template_pkey PRIMARY KEY,
    created_time BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    configuration VARCHAR(10000000) NOT NULL,
    CONSTRAINT uq_notification_template_name UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS notification_rule (
    id UUID NOT NULL CONSTRAINT notification_rule_pkey PRIMARY KEY,
    created_time BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    template_id UUID NOT NULL CONSTRAINT fk_notification_rule_template_id REFERENCES notification_template(id),
    trigger_type VARCHAR(50) NOT NULL,
    trigger_config VARCHAR(1000) NOT NULL,
    recipients_config VARCHAR(10000) NOT NULL,
    additional_config VARCHAR(255),
    CONSTRAINT uq_notification_rule_name UNIQUE (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS notification_request (
    id UUID NOT NULL CONSTRAINT notification_request_pkey PRIMARY KEY,
    created_time BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    targets VARCHAR(10000) NOT NULL,
    template_id UUID,
    template VARCHAR(10000000),
    info VARCHAR(1000),
    additional_config VARCHAR(1000),
    originator_entity_id UUID,
    originator_entity_type VARCHAR(32),
    rule_id UUID NULL,
    status VARCHAR(32),
    stats VARCHAR(10000)
);

CREATE TABLE IF NOT EXISTS notification (
    id UUID NOT NULL,
    created_time BIGINT NOT NULL,
    request_id UUID NULL CONSTRAINT fk_notification_request_id REFERENCES notification_request(id) ON DELETE CASCADE,
    recipient_id UUID NOT NULL CONSTRAINT fk_notification_recipient_id REFERENCES tb_user(id) ON DELETE CASCADE,
    type VARCHAR(50) NOT NULL,
    subject VARCHAR(255),
    body VARCHAR(1000) NOT NULL,
    additional_config VARCHAR(1000),
    status VARCHAR(32)
) PARTITION BY RANGE (created_time);

CREATE TABLE IF NOT EXISTS user_settings (
    user_id uuid NOT NULL,
    type VARCHAR(50) NOT NULL,
    settings varchar(10000),
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE,
    CONSTRAINT user_settings_pkey PRIMARY KEY (user_id, type)
);
