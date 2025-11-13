--
-- Copyright © 2016-2025 The Thingsboard Authors
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
    product varchar(2) NOT NULL,
    CONSTRAINT tb_schema_settings_pkey PRIMARY KEY (schema_version)
);

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

CREATE SEQUENCE IF NOT EXISTS attribute_kv_version_seq cache 1;

CREATE TABLE IF NOT EXISTS attribute_kv (
  entity_id uuid,
  attribute_type int,
  attribute_key int,
  bool_v boolean,
  str_v varchar(10000000),
  long_v bigint,
  dbl_v double precision,
  json_v json,
  last_update_ts bigint,
  version bigint default 0,
  CONSTRAINT attribute_kv_pkey PRIMARY KEY (entity_id, attribute_type, attribute_key)
);

CREATE TABLE IF NOT EXISTS component_descriptor (
    id uuid NOT NULL CONSTRAINT component_descriptor_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    actions varchar(255),
    clazz varchar UNIQUE,
    configuration_descriptor varchar,
    configuration_version int DEFAULT 0,
    name varchar(255),
    scope varchar(255),
    type varchar(255),
    clustering_mode varchar(255),
    has_queue_name boolean DEFAULT false
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
    state varchar(255),
    tenant_id uuid,
    title varchar(255),
    zip varchar(255),
    external_id uuid,
    is_public boolean,
    version BIGINT DEFAULT 1,
    CONSTRAINT customer_title_unq_key UNIQUE (tenant_id, title),
    CONSTRAINT customer_external_id_unq_key UNIQUE (tenant_id, external_id)
);

CREATE TABLE IF NOT EXISTS dashboard (
    id uuid NOT NULL CONSTRAINT dashboard_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    configuration varchar,
    assigned_customers varchar(1000000),
    tenant_id uuid,
    title varchar(255),
    mobile_hide boolean DEFAULT false,
    mobile_order int,
    image varchar(1000000),
    external_id uuid,
    version BIGINT DEFAULT 1,
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
    tenant_id uuid,
    external_id uuid,
    version BIGINT DEFAULT 1,
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
    debug_settings varchar(1024),
    singleton_mode boolean,
    queue_name varchar(255),
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
    external_id uuid,
    CONSTRAINT ota_package_tenant_title_version_unq_key UNIQUE (tenant_id, title, version),
    CONSTRAINT ota_package_external_id_unq_key UNIQUE (tenant_id, external_id)
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
    is_default boolean,
    tenant_id uuid,
    default_rule_chain_id uuid,
    default_dashboard_id uuid,
    default_queue_name varchar(255),
    default_edge_rule_chain_id uuid,
    external_id uuid,
    version BIGINT DEFAULT 1,
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
    tenant_id uuid,
    type varchar(255),
    external_id uuid,
    version BIGINT DEFAULT 1,
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
    version BIGINT DEFAULT 1,
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
    tenant_id uuid,
    firmware_id uuid,
    software_id uuid,
    external_id uuid,
    version BIGINT DEFAULT 1,
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
    version BIGINT DEFAULT 1,
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

CREATE SEQUENCE IF NOT EXISTS relation_version_seq cache 1;

CREATE TABLE IF NOT EXISTS relation (
    from_id uuid,
    from_type varchar(255),
    to_id uuid,
    to_type varchar(255),
    relation_type_group varchar(255),
    relation_type varchar(255),
    additional_info varchar,
    version bigint default 0,
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
    tenant_id uuid,
    version BIGINT DEFAULT 1
);

CREATE TABLE IF NOT EXISTS tenant_profile (
    id uuid NOT NULL CONSTRAINT tenant_profile_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    name varchar(255),
    profile_data jsonb,
    description varchar,
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
    state varchar(255),
    title varchar(255),
    zip varchar(255),
    version BIGINT DEFAULT 1,
    CONSTRAINT fk_tenant_profile FOREIGN KEY (tenant_profile_id) REFERENCES tenant_profile(id)
);

CREATE TABLE IF NOT EXISTS user_credentials (
    id uuid NOT NULL CONSTRAINT user_credentials_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    activate_token varchar(255) UNIQUE,
    activate_token_exp_time BIGINT,
    enabled boolean,
    password varchar(255),
    reset_token varchar(255) UNIQUE,
    reset_token_exp_time BIGINT,
    user_id uuid UNIQUE,
    additional_info varchar DEFAULT '{}',
    last_login_ts BIGINT,
    failed_login_attempts INT
);

CREATE TABLE IF NOT EXISTS widget_type (
    id uuid NOT NULL CONSTRAINT widget_type_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    fqn varchar(512),
    descriptor varchar(1000000),
    name varchar(255),
    tenant_id uuid,
    image varchar(1000000),
    scada boolean NOT NULL DEFAULT false,
    deprecated boolean NOT NULL DEFAULT false,
    description varchar(1024),
    tags text[],
    external_id uuid,
    version BIGINT DEFAULT 1,
    CONSTRAINT uq_widget_type_fqn UNIQUE (tenant_id, fqn),
    CONSTRAINT widget_type_external_id_unq_key UNIQUE (tenant_id, external_id)
);

CREATE TABLE IF NOT EXISTS widgets_bundle (
    id uuid NOT NULL CONSTRAINT widgets_bundle_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    alias varchar(255),
    tenant_id uuid,
    title varchar(255),
    image varchar(1000000),
    scada boolean NOT NULL DEFAULT false,
    description varchar(1024),
    widgets_bundle_order int,
    external_id uuid,
    version BIGINT DEFAULT 1,
    CONSTRAINT uq_widgets_bundle_alias UNIQUE (tenant_id, alias),
    CONSTRAINT widgets_bundle_external_id_unq_key UNIQUE (tenant_id, external_id)
);

CREATE TABLE IF NOT EXISTS widgets_bundle_widget (
    widgets_bundle_id uuid NOT NULL,
    widget_type_id uuid NOT NULL,
    widget_type_order int NOT NULL DEFAULT 0,
    CONSTRAINT widgets_bundle_widget_pkey PRIMARY KEY (widgets_bundle_id, widget_type_id),
    CONSTRAINT fk_widgets_bundle FOREIGN KEY (widgets_bundle_id) REFERENCES widgets_bundle(id) ON DELETE CASCADE,
    CONSTRAINT fk_widget_type FOREIGN KEY (widget_type_id) REFERENCES widget_type(id) ON DELETE CASCADE
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
    additional_info varchar,
    external_id uuid,
    version BIGINT DEFAULT 1,
    CONSTRAINT entity_view_external_id_unq_key UNIQUE (tenant_id, external_id)
);

CREATE SEQUENCE IF NOT EXISTS ts_kv_latest_version_seq cache 1;

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
    version bigint default 0,
    CONSTRAINT ts_kv_latest_pkey PRIMARY KEY (entity_id, key)
);

CREATE TABLE IF NOT EXISTS key_dictionary
(
    key    varchar(255) NOT NULL,
    key_id serial UNIQUE,
    CONSTRAINT key_dictionary_id_pkey PRIMARY KEY (key)
);

CREATE TABLE IF NOT EXISTS oauth2_client (
    id uuid NOT NULL CONSTRAINT oauth2_client_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    title varchar(100) NOT NULL,
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
    custom_send_token boolean
);

CREATE TABLE IF NOT EXISTS domain (
    id uuid NOT NULL CONSTRAINT domain_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    name varchar(255) UNIQUE,
    oauth2_enabled boolean,
    edge_enabled boolean
);

CREATE TABLE IF NOT EXISTS mobile_app (
    id uuid NOT NULL CONSTRAINT mobile_app_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid,
    pkg_name varchar(255),
    title varchar(255),
    app_secret varchar(2048),
    platform_type varchar(32),
    status varchar(32),
    version_info varchar(100000),
    store_info varchar(16384),
    CONSTRAINT mobile_app_pkg_name_platform_unq_key UNIQUE (pkg_name, platform_type)
);

CREATE TABLE IF NOT EXISTS mobile_app_bundle (
    id uuid NOT NULL CONSTRAINT mobile_app_bundle_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid,
    title varchar(255),
    description varchar(1024),
    android_app_id uuid UNIQUE,
    ios_app_id uuid UNIQUE,
    layout_config varchar(16384),
    oauth2_enabled boolean,
    CONSTRAINT fk_android_app_id FOREIGN KEY (android_app_id) REFERENCES mobile_app(id) ON DELETE SET NULL,
    CONSTRAINT fk_ios_app_id FOREIGN KEY (ios_app_id) REFERENCES mobile_app(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS domain_oauth2_client (
    domain_id uuid NOT NULL,
    oauth2_client_id uuid NOT NULL,
    CONSTRAINT fk_domain FOREIGN KEY (domain_id) REFERENCES domain(id) ON DELETE CASCADE,
    CONSTRAINT fk_oauth2_client FOREIGN KEY (oauth2_client_id) REFERENCES oauth2_client(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS mobile_app_bundle_oauth2_client (
    mobile_app_bundle_id uuid NOT NULL,
    oauth2_client_id uuid NOT NULL,
    CONSTRAINT fk_domain FOREIGN KEY (mobile_app_bundle_id) REFERENCES mobile_app_bundle(id) ON DELETE CASCADE,
    CONSTRAINT fk_oauth2_client FOREIGN KEY (oauth2_client_id) REFERENCES oauth2_client(id) ON DELETE CASCADE
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
    tbel_exec varchar(32),
    email_exec varchar(32),
    sms_exec varchar(32),
    alarm_exec varchar(32),
    version BIGINT DEFAULT 1,
    CONSTRAINT api_usage_state_unq_key UNIQUE (tenant_id, entity_id)
);

CREATE TABLE IF NOT EXISTS api_key (
    id uuid NOT NULL CONSTRAINT api_key_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid,
    user_id uuid,
    value varchar(512),
    enabled boolean NOT NULL DEFAULT TRUE,
    expiration_time bigint DEFAULT 0,
    description varchar(255),
    CONSTRAINT api_key_value_unq_key UNIQUE (value)
);

CREATE TABLE IF NOT EXISTS resource (
    id uuid NOT NULL CONSTRAINT resource_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    title varchar(255) NOT NULL,
    resource_type varchar(32) NOT NULL,
    resource_sub_type varchar(32),
    resource_key varchar(255) NOT NULL,
    search_text varchar(255),
    file_name varchar(255) NOT NULL,
    data bytea,
    etag varchar,
    descriptor varchar,
    preview bytea,
    is_public boolean default true,
    public_resource_key varchar(32) unique,
    external_id uuid,
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
    tenant_id uuid,
    version BIGINT DEFAULT 1,
    CONSTRAINT edge_name_unq_key UNIQUE (tenant_id, name),
    CONSTRAINT edge_routing_key_unq_key UNIQUE (routing_key)
);

CREATE TABLE IF NOT EXISTS edge_event (
    seq_id INT GENERATED ALWAYS AS IDENTITY,
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
ALTER TABLE IF EXISTS edge_event ALTER COLUMN seq_id SET CYCLE;

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
    external_id UUID,
    CONSTRAINT uq_notification_target_name UNIQUE (tenant_id, name),
    CONSTRAINT uq_notification_target_external_id UNIQUE (tenant_id, external_id)
);

CREATE TABLE IF NOT EXISTS notification_template (
    id UUID NOT NULL CONSTRAINT notification_template_pkey PRIMARY KEY,
    created_time BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    notification_type VARCHAR(50) NOT NULL,
    configuration VARCHAR(10000000) NOT NULL,
    external_id UUID,
    CONSTRAINT uq_notification_template_name UNIQUE (tenant_id, name),
    CONSTRAINT uq_notification_template_external_id UNIQUE (tenant_id, external_id)
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
    external_id UUID,
    CONSTRAINT uq_notification_rule_name UNIQUE (tenant_id, name),
    CONSTRAINT uq_notification_rule_external_id UNIQUE (tenant_id, external_id)
);

CREATE TABLE IF NOT EXISTS notification_request (
    id UUID NOT NULL CONSTRAINT notification_request_pkey PRIMARY KEY,
    created_time BIGINT NOT NULL,
    tenant_id UUID NOT NULL,
    targets VARCHAR(10000) NOT NULL,
    template_id UUID,
    template VARCHAR(10000000),
    info VARCHAR(1000000),
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
    request_id UUID,
    recipient_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    delivery_method VARCHAR(50) NOT NULL,
    subject VARCHAR(255),
    body VARCHAR(1000) NOT NULL,
    additional_config VARCHAR(1000),
    status VARCHAR(32)
) PARTITION BY RANGE (created_time);

CREATE TABLE IF NOT EXISTS user_settings (
    user_id uuid NOT NULL,
    type VARCHAR(50) NOT NULL,
    settings jsonb,
    CONSTRAINT fk_user_id FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE,
    CONSTRAINT user_settings_pkey PRIMARY KEY (user_id, type)
);

CREATE TABLE IF NOT EXISTS alarm_types (
    tenant_id uuid NOT NULL,
    type varchar(255) NOT NULL,
    CONSTRAINT tenant_id_type_unq_key UNIQUE (tenant_id, type),
    CONSTRAINT fk_entity_tenant_id FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS queue_stats (
    id uuid NOT NULL CONSTRAINT queue_stats_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    queue_name varchar(255) NOT NULL,
    service_id varchar(255) NOT NULL,
    CONSTRAINT queue_stats_name_unq_key UNIQUE (tenant_id, queue_name, service_id)
);

CREATE TABLE IF NOT EXISTS qr_code_settings (
    id uuid NOT NULL CONSTRAINT qr_code_settings_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    use_default_app boolean,
    android_enabled boolean,
    ios_enabled boolean,
    mobile_app_bundle_id uuid,
    qr_code_config VARCHAR(100000),
    CONSTRAINT qr_code_settings_tenant_id_unq_key UNIQUE (tenant_id)
);

CREATE TABLE IF NOT EXISTS calculated_field (
    id uuid NOT NULL CONSTRAINT calculated_field_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    entity_type VARCHAR(32),
    entity_id uuid NOT NULL,
    type varchar(32) NOT NULL,
    name varchar(255) NOT NULL,
    configuration_version int DEFAULT 0,
    configuration varchar(1000000),
    version BIGINT DEFAULT 1,
    debug_settings varchar(1024),
    CONSTRAINT calculated_field_unq_key UNIQUE (entity_id, type, name)
);

CREATE TABLE IF NOT EXISTS cf_debug_event (
    id uuid NOT NULL,
    tenant_id uuid NOT NULL ,
    ts bigint NOT NULL,
    entity_id uuid NOT NULL, -- calculated field id
    service_id varchar,
    cf_id uuid NOT NULL,
    e_entity_id uuid, -- target entity id
    e_entity_type varchar,
    e_msg_id uuid,
    e_msg_type varchar,
    e_args varchar,
    e_result varchar,
    e_error varchar
) PARTITION BY RANGE (ts);

CREATE TABLE IF NOT EXISTS job (
    id uuid NOT NULL CONSTRAINT job_pkey PRIMARY KEY,
    created_time bigint NOT NULL,
    tenant_id uuid NOT NULL,
    type varchar NOT NULL,
    key varchar NOT NULL,
    entity_id uuid NOT NULL,
    entity_type varchar NOT NULL,
    status varchar NOT NULL,
    configuration varchar NOT NULL,
    result varchar
);

CREATE TABLE IF NOT EXISTS ai_model (
    id              UUID          NOT NULL PRIMARY KEY,
    external_id     UUID,
    created_time    BIGINT        NOT NULL,
    tenant_id       UUID          NOT NULL,
    version         BIGINT        NOT NULL DEFAULT 1,
    name            VARCHAR(255)  NOT NULL,
    configuration   JSONB         NOT NULL,
    CONSTRAINT ai_model_name_unq_key        UNIQUE (tenant_id, name),
    CONSTRAINT ai_model_external_id_unq_key UNIQUE (tenant_id, external_id)
);
