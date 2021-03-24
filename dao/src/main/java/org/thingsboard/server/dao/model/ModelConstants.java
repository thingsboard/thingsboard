/**
 * Copyright © 2016-2021 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.model;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.apache.commons.lang3.ArrayUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;

import java.util.UUID;

public class ModelConstants {

    private ModelConstants() {
    }

    public static final UUID NULL_UUID = Uuids.startOf(0);
    public static final TenantId SYSTEM_TENANT = new TenantId(ModelConstants.NULL_UUID);

    // this is the difference between midnight October 15, 1582 UTC and midnight January 1, 1970 UTC as 100 nanosecond units
    public static final long EPOCH_DIFF = 122192928000000000L;

    /**
     * Generic constants.
     */
    public static final String ID_PROPERTY = "id";
    public static final String CREATED_TIME_PROPERTY = "created_time";
    public static final String USER_ID_PROPERTY = "user_id";
    public static final String TENANT_ID_PROPERTY = "tenant_id";
    public static final String CUSTOMER_ID_PROPERTY = "customer_id";
    public static final String DEVICE_ID_PROPERTY = "device_id";
    public static final String TITLE_PROPERTY = "title";
    public static final String ALIAS_PROPERTY = "alias";
    public static final String SEARCH_TEXT_PROPERTY = "search_text";
    public static final String ADDITIONAL_INFO_PROPERTY = "additional_info";
    public static final String ENTITY_TYPE_PROPERTY = "entity_type";

    public static final String ENTITY_TYPE_COLUMN = ENTITY_TYPE_PROPERTY;
    public static final String TENANT_ID_COLUMN = "tenant_id";
    public static final String ENTITY_ID_COLUMN = "entity_id";
    public static final String ATTRIBUTE_TYPE_COLUMN = "attribute_type";
    public static final String ATTRIBUTE_KEY_COLUMN = "attribute_key";
    public static final String LAST_UPDATE_TS_COLUMN = "last_update_ts";

    /**
     * Cassandra user constants.
     */
    public static final String USER_COLUMN_FAMILY_NAME = "user";
    public static final String USER_PG_HIBERNATE_COLUMN_FAMILY_NAME = "tb_user";
    public static final String USER_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String USER_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String USER_EMAIL_PROPERTY = "email";
    public static final String USER_AUTHORITY_PROPERTY = "authority";
    public static final String USER_FIRST_NAME_PROPERTY = "first_name";
    public static final String USER_LAST_NAME_PROPERTY = "last_name";
    public static final String USER_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;

    public static final String USER_BY_EMAIL_COLUMN_FAMILY_NAME = "user_by_email";
    public static final String USER_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME = "user_by_tenant_and_search_text";
    public static final String USER_BY_CUSTOMER_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME = "user_by_customer_and_search_text";

    /**
     * Cassandra user_credentials constants.
     */
    public static final String USER_CREDENTIALS_COLUMN_FAMILY_NAME = "user_credentials";
    public static final String USER_CREDENTIALS_USER_ID_PROPERTY = USER_ID_PROPERTY;
    public static final String USER_CREDENTIALS_ENABLED_PROPERTY = "enabled";
    public static final String USER_CREDENTIALS_PASSWORD_PROPERTY = "password"; //NOSONAR, the constant used to identify password column name (not password value itself)
    public static final String USER_CREDENTIALS_ACTIVATE_TOKEN_PROPERTY = "activate_token";
    public static final String USER_CREDENTIALS_RESET_TOKEN_PROPERTY = "reset_token";

    public static final String USER_CREDENTIALS_BY_USER_COLUMN_FAMILY_NAME = "user_credentials_by_user";
    public static final String USER_CREDENTIALS_BY_ACTIVATE_TOKEN_COLUMN_FAMILY_NAME = "user_credentials_by_activate_token";
    public static final String USER_CREDENTIALS_BY_RESET_TOKEN_COLUMN_FAMILY_NAME = "user_credentials_by_reset_token";

    /**
     * Cassandra admin_settings constants.
     */
    public static final String ADMIN_SETTINGS_COLUMN_FAMILY_NAME = "admin_settings";
    public static final String ADMIN_SETTINGS_KEY_PROPERTY = "key";
    public static final String ADMIN_SETTINGS_JSON_VALUE_PROPERTY = "json_value";

    public static final String ADMIN_SETTINGS_BY_KEY_COLUMN_FAMILY_NAME = "admin_settings_by_key";

    /**
     * Cassandra contact constants.
     */
    public static final String COUNTRY_PROPERTY = "country";
    public static final String STATE_PROPERTY = "state";
    public static final String CITY_PROPERTY = "city";
    public static final String ADDRESS_PROPERTY = "address";
    public static final String ADDRESS2_PROPERTY = "address2";
    public static final String ZIP_PROPERTY = "zip";
    public static final String PHONE_PROPERTY = "phone";
    public static final String EMAIL_PROPERTY = "email";

    /**
     * Cassandra tenant constants.
     */
    public static final String TENANT_COLUMN_FAMILY_NAME = "tenant";
    public static final String TENANT_TITLE_PROPERTY = TITLE_PROPERTY;
    public static final String TENANT_REGION_PROPERTY = "region";
    public static final String TENANT_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;
    public static final String TENANT_TENANT_PROFILE_ID_PROPERTY = "tenant_profile_id";

    public static final String TENANT_BY_REGION_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME = "tenant_by_region_and_search_text";

    /**
     * Tenant profile constants.
     */
    public static final String TENANT_PROFILE_COLUMN_FAMILY_NAME = "tenant_profile";
    public static final String TENANT_PROFILE_NAME_PROPERTY = "name";
    public static final String TENANT_PROFILE_PROFILE_DATA_PROPERTY = "profile_data";
    public static final String TENANT_PROFILE_DESCRIPTION_PROPERTY = "description";
    public static final String TENANT_PROFILE_IS_DEFAULT_PROPERTY = "is_default";
    public static final String TENANT_PROFILE_ISOLATED_TB_CORE = "isolated_tb_core";
    public static final String TENANT_PROFILE_ISOLATED_TB_RULE_ENGINE = "isolated_tb_rule_engine";

    /**
     * Cassandra customer constants.
     */
    public static final String CUSTOMER_COLUMN_FAMILY_NAME = "customer";
    public static final String CUSTOMER_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String CUSTOMER_TITLE_PROPERTY = TITLE_PROPERTY;
    public static final String CUSTOMER_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;

    public static final String CUSTOMER_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME = "customer_by_tenant_and_search_text";
    public static final String CUSTOMER_BY_TENANT_AND_TITLE_VIEW_NAME = "customer_by_tenant_and_title";

    /**
     * Cassandra device constants.
     */
    public static final String DEVICE_COLUMN_FAMILY_NAME = "device";
    public static final String DEVICE_FAMILY_NAME = "device";
    public static final String DEVICE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String DEVICE_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String DEVICE_NAME_PROPERTY = "name";
    public static final String DEVICE_TYPE_PROPERTY = "type";
    public static final String DEVICE_LABEL_PROPERTY = "label";
    public static final String DEVICE_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;
    public static final String DEVICE_DEVICE_PROFILE_ID_PROPERTY = "device_profile_id";
    public static final String DEVICE_DEVICE_DATA_PROPERTY = "device_data";

    public static final String DEVICE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME = "device_by_tenant_and_search_text";
    public static final String DEVICE_BY_TENANT_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME = "device_by_tenant_by_type_and_search_text";
    public static final String DEVICE_BY_CUSTOMER_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME = "device_by_customer_and_search_text";
    public static final String DEVICE_BY_CUSTOMER_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME = "device_by_customer_by_type_and_search_text";
    public static final String DEVICE_BY_TENANT_AND_NAME_VIEW_NAME = "device_by_tenant_and_name";
    public static final String DEVICE_TYPES_BY_TENANT_VIEW_NAME = "device_types_by_tenant";

    /**
     * Device profile constants.
     */
    public static final String DEVICE_PROFILE_COLUMN_FAMILY_NAME = "device_profile";
    public static final String DEVICE_PROFILE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String DEVICE_PROFILE_NAME_PROPERTY = "name";
    public static final String DEVICE_PROFILE_TYPE_PROPERTY = "type";
    public static final String DEVICE_PROFILE_TRANSPORT_TYPE_PROPERTY = "transport_type";
    public static final String DEVICE_PROFILE_PROVISION_TYPE_PROPERTY = "provision_type";
    public static final String DEVICE_PROFILE_PROFILE_DATA_PROPERTY = "profile_data";
    public static final String DEVICE_PROFILE_DESCRIPTION_PROPERTY = "description";
    public static final String DEVICE_PROFILE_IS_DEFAULT_PROPERTY = "is_default";
    public static final String DEVICE_PROFILE_DEFAULT_RULE_CHAIN_ID_PROPERTY = "default_rule_chain_id";
    public static final String DEVICE_PROFILE_DEFAULT_QUEUE_NAME_PROPERTY = "default_queue_name";
    public static final String DEVICE_PROFILE_PROVISION_DEVICE_KEY = "provision_device_key";

    /**
     * Cassandra entityView constants.
     */
    public static final String ENTITY_VIEW_TABLE_FAMILY_NAME = "entity_view";
    public static final String ENTITY_VIEW_ENTITY_ID_PROPERTY = ENTITY_ID_COLUMN;
    public static final String ENTITY_VIEW_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String ENTITY_VIEW_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String ENTITY_VIEW_NAME_PROPERTY = DEVICE_NAME_PROPERTY;
    public static final String ENTITY_VIEW_BY_TENANT_AND_CUSTOMER_CF = "entity_view_by_tenant_and_customer";
    public static final String ENTITY_VIEW_BY_TENANT_AND_CUSTOMER_AND_TYPE_CF = "entity_view_by_tenant_and_customer_and_type";
    public static final String ENTITY_VIEW_BY_TENANT_AND_ENTITY_ID_CF = "entity_view_by_tenant_and_entity_id";
    public static final String ENTITY_VIEW_KEYS_PROPERTY = "keys";
    public static final String ENTITY_VIEW_TYPE_PROPERTY = "type";
    public static final String ENTITY_VIEW_START_TS_PROPERTY = "start_ts";
    public static final String ENTITY_VIEW_END_TS_PROPERTY = "end_ts";
    public static final String ENTITY_VIEW_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;
    public static final String ENTITY_VIEW_BY_TENANT_AND_SEARCH_TEXT_CF = "entity_view_by_tenant_and_search_text";
    public static final String ENTITY_VIEW_BY_TENANT_BY_TYPE_AND_SEARCH_TEXT_CF = "entity_view_by_tenant_by_type_and_search_text";
    public static final String ENTITY_VIEW_BY_TENANT_AND_NAME = "entity_view_by_tenant_and_name";

    /**
     * Cassandra audit log constants.
     */
    public static final String AUDIT_LOG_COLUMN_FAMILY_NAME = "audit_log";

    public static final String AUDIT_LOG_BY_ENTITY_ID_CF = "audit_log_by_entity_id";
    public static final String AUDIT_LOG_BY_CUSTOMER_ID_CF = "audit_log_by_customer_id";
    public static final String AUDIT_LOG_BY_USER_ID_CF = "audit_log_by_user_id";
    public static final String AUDIT_LOG_BY_TENANT_ID_CF = "audit_log_by_tenant_id";
    public static final String AUDIT_LOG_BY_TENANT_ID_PARTITIONS_CF = "audit_log_by_tenant_id_partitions";

    public static final String AUDIT_LOG_ID_PROPERTY = ID_PROPERTY;
    public static final String AUDIT_LOG_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String AUDIT_LOG_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String AUDIT_LOG_ENTITY_TYPE_PROPERTY = ENTITY_TYPE_PROPERTY;
    public static final String AUDIT_LOG_ENTITY_ID_PROPERTY = ENTITY_ID_COLUMN;
    public static final String AUDIT_LOG_ENTITY_NAME_PROPERTY = "entity_name";
    public static final String AUDIT_LOG_USER_ID_PROPERTY = USER_ID_PROPERTY;
    public static final String AUDIT_LOG_PARTITION_PROPERTY = "partition";
    public static final String AUDIT_LOG_USER_NAME_PROPERTY = "user_name";
    public static final String AUDIT_LOG_ACTION_TYPE_PROPERTY = "action_type";
    public static final String AUDIT_LOG_ACTION_DATA_PROPERTY = "action_data";
    public static final String AUDIT_LOG_ACTION_STATUS_PROPERTY = "action_status";
    public static final String AUDIT_LOG_ACTION_FAILURE_DETAILS_PROPERTY = "action_failure_details";

    /**
     * Cassandra asset constants.
     */
    public static final String ASSET_COLUMN_FAMILY_NAME = "asset";
    public static final String ASSET_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String ASSET_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String ASSET_NAME_PROPERTY = "name";
    public static final String ASSET_TYPE_PROPERTY = "type";
    public static final String ASSET_LABEL_PROPERTY = "label";
    public static final String ASSET_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;

    public static final String ASSET_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME = "asset_by_tenant_and_search_text";
    public static final String ASSET_BY_TENANT_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME = "asset_by_tenant_by_type_and_search_text";
    public static final String ASSET_BY_CUSTOMER_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME = "asset_by_customer_and_search_text";
    public static final String ASSET_BY_CUSTOMER_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME = "asset_by_customer_by_type_and_search_text";
    public static final String ASSET_BY_TENANT_AND_NAME_VIEW_NAME = "asset_by_tenant_and_name";
    public static final String ASSET_TYPES_BY_TENANT_VIEW_NAME = "asset_types_by_tenant";

    /**
     * Cassandra entity_subtype constants.
     */
    public static final String ENTITY_SUBTYPE_COLUMN_FAMILY_NAME = "entity_subtype";
    public static final String ENTITY_SUBTYPE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String ENTITY_SUBTYPE_ENTITY_TYPE_PROPERTY = ENTITY_TYPE_PROPERTY;
    public static final String ENTITY_SUBTYPE_TYPE_PROPERTY = "type";

    /**
     * Cassandra alarm constants.
     */
    public static final String ALARM_COLUMN_FAMILY_NAME = "alarm";
    public static final String ALARM_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String ALARM_TYPE_PROPERTY = "type";
    public static final String ALARM_DETAILS_PROPERTY = "details";
    public static final String ALARM_ORIGINATOR_ID_PROPERTY = "originator_id";
    public static final String ALARM_ORIGINATOR_NAME_PROPERTY = "originator_name";
    public static final String ALARM_ORIGINATOR_TYPE_PROPERTY = "originator_type";
    public static final String ALARM_SEVERITY_PROPERTY = "severity";
    public static final String ALARM_STATUS_PROPERTY = "status";
    public static final String ALARM_START_TS_PROPERTY = "start_ts";
    public static final String ALARM_END_TS_PROPERTY = "end_ts";
    public static final String ALARM_ACK_TS_PROPERTY = "ack_ts";
    public static final String ALARM_CLEAR_TS_PROPERTY = "clear_ts";
    public static final String ALARM_PROPAGATE_PROPERTY = "propagate";
    public static final String ALARM_PROPAGATE_RELATION_TYPES = "propagate_relation_types";

    public static final String ALARM_BY_ID_VIEW_NAME = "alarm_by_id";

    /**
     * Cassandra entity relation constants.
     */
    public static final String RELATION_COLUMN_FAMILY_NAME = "relation";
    public static final String RELATION_FROM_ID_PROPERTY = "from_id";
    public static final String RELATION_FROM_TYPE_PROPERTY = "from_type";
    public static final String RELATION_TO_ID_PROPERTY = "to_id";
    public static final String RELATION_TO_TYPE_PROPERTY = "to_type";
    public static final String RELATION_TYPE_PROPERTY = "relation_type";
    public static final String RELATION_TYPE_GROUP_PROPERTY = "relation_type_group";

    public static final String RELATION_BY_TYPE_AND_CHILD_TYPE_VIEW_NAME = "relation_by_type_and_child_type";
    public static final String RELATION_REVERSE_VIEW_NAME = "reverse_relation";


    /**
     * Cassandra device_credentials constants.
     */
    public static final String DEVICE_CREDENTIALS_COLUMN_FAMILY_NAME = "device_credentials";
    public static final String DEVICE_CREDENTIALS_DEVICE_ID_PROPERTY = DEVICE_ID_PROPERTY;
    public static final String DEVICE_CREDENTIALS_CREDENTIALS_TYPE_PROPERTY = "credentials_type";
    public static final String DEVICE_CREDENTIALS_CREDENTIALS_ID_PROPERTY = "credentials_id";
    public static final String DEVICE_CREDENTIALS_CREDENTIALS_VALUE_PROPERTY = "credentials_value";

    public static final String DEVICE_CREDENTIALS_BY_DEVICE_COLUMN_FAMILY_NAME = "device_credentials_by_device";
    public static final String DEVICE_CREDENTIALS_BY_CREDENTIALS_ID_COLUMN_FAMILY_NAME = "device_credentials_by_credentials_id";

    /**
     * Cassandra widgets_bundle constants.
     */
    public static final String WIDGETS_BUNDLE_COLUMN_FAMILY_NAME = "widgets_bundle";
    public static final String WIDGETS_BUNDLE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String WIDGETS_BUNDLE_ALIAS_PROPERTY = ALIAS_PROPERTY;
    public static final String WIDGETS_BUNDLE_TITLE_PROPERTY = TITLE_PROPERTY;
    public static final String WIDGETS_BUNDLE_IMAGE_PROPERTY = "image";
    public static final String WIDGETS_BUNDLE_DESCRIPTION = "description";

    public static final String WIDGETS_BUNDLE_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME = "widgets_bundle_by_tenant_and_search_text";
    public static final String WIDGETS_BUNDLE_BY_TENANT_AND_ALIAS_COLUMN_FAMILY_NAME = "widgets_bundle_by_tenant_and_alias";

    /**
     * Cassandra widget_type constants.
     */
    public static final String WIDGET_TYPE_COLUMN_FAMILY_NAME = "widget_type";
    public static final String WIDGET_TYPE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String WIDGET_TYPE_BUNDLE_ALIAS_PROPERTY = "bundle_alias";
    public static final String WIDGET_TYPE_ALIAS_PROPERTY = ALIAS_PROPERTY;
    public static final String WIDGET_TYPE_NAME_PROPERTY = "name";
    public static final String WIDGET_TYPE_IMAGE_PROPERTY = "image";
    public static final String WIDGET_TYPE_DESCRIPTION_PROPERTY = "description";
    public static final String WIDGET_TYPE_DESCRIPTOR_PROPERTY = "descriptor";

    public static final String WIDGET_TYPE_BY_TENANT_AND_ALIASES_COLUMN_FAMILY_NAME = "widget_type_by_tenant_and_aliases";

    /**
     * Cassandra dashboard constants.
     */
    public static final String DASHBOARD_COLUMN_FAMILY_NAME = "dashboard";
    public static final String DASHBOARD_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String DASHBOARD_TITLE_PROPERTY = TITLE_PROPERTY;
    public static final String DASHBOARD_CONFIGURATION_PROPERTY = "configuration";
    public static final String DASHBOARD_ASSIGNED_CUSTOMERS_PROPERTY = "assigned_customers";

    public static final String DASHBOARD_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME = "dashboard_by_tenant_and_search_text";

    /**
     * Cassandra plugin component metadata constants.
     */
    public static final String COMPONENT_DESCRIPTOR_COLUMN_FAMILY_NAME = "component_descriptor";
    public static final String COMPONENT_DESCRIPTOR_TYPE_PROPERTY = "type";
    public static final String COMPONENT_DESCRIPTOR_SCOPE_PROPERTY = "scope";
    public static final String COMPONENT_DESCRIPTOR_NAME_PROPERTY = "name";
    public static final String COMPONENT_DESCRIPTOR_CLASS_PROPERTY = "clazz";
    public static final String COMPONENT_DESCRIPTOR_CONFIGURATION_DESCRIPTOR_PROPERTY = "configuration_descriptor";
    public static final String COMPONENT_DESCRIPTOR_ACTIONS_PROPERTY = "actions";

    public static final String COMPONENT_DESCRIPTOR_BY_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME = "component_desc_by_type_search_text";
    public static final String COMPONENT_DESCRIPTOR_BY_SCOPE_TYPE_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME = "component_desc_by_scope_type_search_text";
    public static final String COMPONENT_DESCRIPTOR_BY_ID = "component_desc_by_id";

    /**
     * Cassandra event constants.
     */
    public static final String EVENT_COLUMN_FAMILY_NAME = "event";
    public static final String EVENT_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String EVENT_TYPE_PROPERTY = "event_type";
    public static final String EVENT_UID_PROPERTY = "event_uid";
    public static final String EVENT_ENTITY_TYPE_PROPERTY = ENTITY_TYPE_PROPERTY;
    public static final String EVENT_ENTITY_ID_PROPERTY = "entity_id";
    public static final String EVENT_BODY_PROPERTY = "body";

    public static final String EVENT_BY_TYPE_AND_ID_VIEW_NAME = "event_by_type_and_id";
    public static final String EVENT_BY_ID_VIEW_NAME = "event_by_id";

    public static final String DEBUG_MODE = "debug_mode";

    /**
     * Cassandra rule chain constants.
     */
    public static final String RULE_CHAIN_COLUMN_FAMILY_NAME = "rule_chain";
    public static final String RULE_CHAIN_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String RULE_CHAIN_NAME_PROPERTY = "name";
    public static final String RULE_CHAIN_FIRST_RULE_NODE_ID_PROPERTY = "first_rule_node_id";
    public static final String RULE_CHAIN_ROOT_PROPERTY = "root";
    public static final String RULE_CHAIN_CONFIGURATION_PROPERTY = "configuration";

    public static final String RULE_CHAIN_BY_TENANT_AND_SEARCH_TEXT_COLUMN_FAMILY_NAME = "rule_chain_by_tenant_and_search_text";

    /**
     * Cassandra rule node constants.
     */
    public static final String RULE_NODE_COLUMN_FAMILY_NAME = "rule_node";
    public static final String RULE_NODE_CHAIN_ID_PROPERTY = "rule_chain_id";
    public static final String RULE_NODE_TYPE_PROPERTY = "type";
    public static final String RULE_NODE_NAME_PROPERTY = "name";
    public static final String RULE_NODE_CONFIGURATION_PROPERTY = "configuration";

    /**
     * Rule node state constants.
     */
    public static final String RULE_NODE_STATE_TABLE_NAME = "rule_node_state";
    public static final String RULE_NODE_STATE_NODE_ID_PROPERTY = "rule_node_id";
    public static final String RULE_NODE_STATE_ENTITY_TYPE_PROPERTY = "entity_type";
    public static final String RULE_NODE_STATE_ENTITY_ID_PROPERTY = "entity_id";
    public static final String RULE_NODE_STATE_DATA_PROPERTY = "state_data";

    /**
     * OAuth2 client registration constants.
     */
    public static final String OAUTH2_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String OAUTH2_CLIENT_REGISTRATION_INFO_COLUMN_FAMILY_NAME = "oauth2_client_registration_info";
    public static final String OAUTH2_CLIENT_REGISTRATION_COLUMN_FAMILY_NAME = "oauth2_client_registration";
    public static final String OAUTH2_CLIENT_REGISTRATION_TO_DOMAIN_COLUMN_FAMILY_NAME = "oauth2_client_registration_to_domain";
    public static final String OAUTH2_CLIENT_REGISTRATION_TEMPLATE_COLUMN_FAMILY_NAME = "oauth2_client_registration_template";
    public static final String OAUTH2_ENABLED_PROPERTY = "enabled";
    public static final String OAUTH2_TEMPLATE_PROVIDER_ID_PROPERTY = "provider_id";
    public static final String OAUTH2_CLIENT_REGISTRATION_INFO_ID_PROPERTY = "client_registration_info_id";
    public static final String OAUTH2_DOMAIN_NAME_PROPERTY = "domain_name";
    public static final String OAUTH2_DOMAIN_SCHEME_PROPERTY = "domain_scheme";
    public static final String OAUTH2_CLIENT_ID_PROPERTY = "client_id";
    public static final String OAUTH2_CLIENT_SECRET_PROPERTY = "client_secret";
    public static final String OAUTH2_AUTHORIZATION_URI_PROPERTY = "authorization_uri";
    public static final String OAUTH2_TOKEN_URI_PROPERTY = "token_uri";
    public static final String OAUTH2_REDIRECT_URI_TEMPLATE_PROPERTY = "redirect_uri_template";
    public static final String OAUTH2_SCOPE_PROPERTY = "scope";
    public static final String OAUTH2_USER_INFO_URI_PROPERTY = "user_info_uri";
    public static final String OAUTH2_USER_NAME_ATTRIBUTE_NAME_PROPERTY = "user_name_attribute_name";
    public static final String OAUTH2_JWK_SET_URI_PROPERTY = "jwk_set_uri";
    public static final String OAUTH2_CLIENT_AUTHENTICATION_METHOD_PROPERTY = "client_authentication_method";
    public static final String OAUTH2_LOGIN_BUTTON_LABEL_PROPERTY = "login_button_label";
    public static final String OAUTH2_LOGIN_BUTTON_ICON_PROPERTY = "login_button_icon";
    public static final String OAUTH2_ALLOW_USER_CREATION_PROPERTY = "allow_user_creation";
    public static final String OAUTH2_ACTIVATE_USER_PROPERTY = "activate_user";
    public static final String OAUTH2_MAPPER_TYPE_PROPERTY = "type";
    public static final String OAUTH2_EMAIL_ATTRIBUTE_KEY_PROPERTY = "basic_email_attribute_key";
    public static final String OAUTH2_FIRST_NAME_ATTRIBUTE_KEY_PROPERTY = "basic_first_name_attribute_key";
    public static final String OAUTH2_LAST_NAME_ATTRIBUTE_KEY_PROPERTY = "basic_last_name_attribute_key";
    public static final String OAUTH2_TENANT_NAME_STRATEGY_PROPERTY = "basic_tenant_name_strategy";
    public static final String OAUTH2_TENANT_NAME_PATTERN_PROPERTY = "basic_tenant_name_pattern";
    public static final String OAUTH2_CUSTOMER_NAME_PATTERN_PROPERTY = "basic_customer_name_pattern";
    public static final String OAUTH2_DEFAULT_DASHBOARD_NAME_PROPERTY = "basic_default_dashboard_name";
    public static final String OAUTH2_ALWAYS_FULL_SCREEN_PROPERTY = "basic_always_full_screen";
    public static final String OAUTH2_MAPPER_URL_PROPERTY = "custom_url";
    public static final String OAUTH2_MAPPER_USERNAME_PROPERTY = "custom_username";
    public static final String OAUTH2_MAPPER_PASSWORD_PROPERTY = "custom_password";
    public static final String OAUTH2_MAPPER_SEND_TOKEN_PROPERTY = "custom_send_token";
    public static final String OAUTH2_TEMPLATE_COMMENT_PROPERTY = "comment";
    public static final String OAUTH2_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;
    public static final String OAUTH2_TEMPLATE_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;
    public static final String OAUTH2_TEMPLATE_LOGIN_BUTTON_ICON_PROPERTY = OAUTH2_LOGIN_BUTTON_ICON_PROPERTY;
    public static final String OAUTH2_TEMPLATE_LOGIN_BUTTON_LABEL_PROPERTY = OAUTH2_LOGIN_BUTTON_LABEL_PROPERTY;
    public static final String OAUTH2_TEMPLATE_HELP_LINK_PROPERTY = "help_link";

    /**
     * Usage Record constants.
     */
    public static final String API_USAGE_STATE_TABLE_NAME = "api_usage_state";
    public static final String API_USAGE_STATE_TENANT_ID_COLUMN = TENANT_ID_PROPERTY;
    public static final String API_USAGE_STATE_ENTITY_TYPE_COLUMN = ENTITY_TYPE_COLUMN;
    public static final String API_USAGE_STATE_ENTITY_ID_COLUMN = ENTITY_ID_COLUMN;
    public static final String API_USAGE_STATE_TRANSPORT_COLUMN = "transport";
    public static final String API_USAGE_STATE_DB_STORAGE_COLUMN = "db_storage";
    public static final String API_USAGE_STATE_RE_EXEC_COLUMN = "re_exec";
    public static final String API_USAGE_STATE_JS_EXEC_COLUMN = "js_exec";
    public static final String API_USAGE_STATE_EMAIL_EXEC_COLUMN = "email_exec";
    public static final String API_USAGE_STATE_SMS_EXEC_COLUMN = "sms_exec";

    /**
     * Cassandra attributes and timeseries constants.
     */
    public static final String ATTRIBUTES_KV_CF = "attributes_kv_cf";
    public static final String TS_KV_CF = "ts_kv_cf";
    public static final String TS_KV_PARTITIONS_CF = "ts_kv_partitions_cf";
    public static final String TS_KV_LATEST_CF = "ts_kv_latest_cf";

    public static final String PARTITION_COLUMN = "partition";
    public static final String KEY_COLUMN = "key";
    public static final String KEY_ID_COLUMN = "key_id";
    public static final String TS_COLUMN = "ts";

    /**
     * Main names of cassandra key-value columns storage.
     */
    public static final String BOOLEAN_VALUE_COLUMN = "bool_v";
    public static final String STRING_VALUE_COLUMN = "str_v";
    public static final String LONG_VALUE_COLUMN = "long_v";
    public static final String DOUBLE_VALUE_COLUMN = "dbl_v";
    public static final String JSON_VALUE_COLUMN = "json_v";

    protected static final String[] NONE_AGGREGATION_COLUMNS = new String[]{LONG_VALUE_COLUMN, DOUBLE_VALUE_COLUMN, BOOLEAN_VALUE_COLUMN, STRING_VALUE_COLUMN, JSON_VALUE_COLUMN, KEY_COLUMN, TS_COLUMN};

    protected static final String[] COUNT_AGGREGATION_COLUMNS = new String[]{count(LONG_VALUE_COLUMN), count(DOUBLE_VALUE_COLUMN), count(BOOLEAN_VALUE_COLUMN), count(STRING_VALUE_COLUMN), count(JSON_VALUE_COLUMN)};

    protected static final String[] MIN_AGGREGATION_COLUMNS =
            ArrayUtils.addAll(COUNT_AGGREGATION_COLUMNS, new String[]{min(LONG_VALUE_COLUMN), min(DOUBLE_VALUE_COLUMN), min(BOOLEAN_VALUE_COLUMN), min(STRING_VALUE_COLUMN), min(JSON_VALUE_COLUMN)});
    protected static final String[] MAX_AGGREGATION_COLUMNS =
            ArrayUtils.addAll(COUNT_AGGREGATION_COLUMNS, new String[]{max(LONG_VALUE_COLUMN), max(DOUBLE_VALUE_COLUMN), max(BOOLEAN_VALUE_COLUMN), max(STRING_VALUE_COLUMN), max(JSON_VALUE_COLUMN)});
    protected static final String[] SUM_AGGREGATION_COLUMNS =
            ArrayUtils.addAll(COUNT_AGGREGATION_COLUMNS, new String[]{sum(LONG_VALUE_COLUMN), sum(DOUBLE_VALUE_COLUMN)});
    protected static final String[] AVG_AGGREGATION_COLUMNS = SUM_AGGREGATION_COLUMNS;

    public static String min(String s) {
        return "min(" + s + ")";
    }

    public static String max(String s) {
        return "max(" + s + ")";
    }

    public static String sum(String s) {
        return "sum(" + s + ")";
    }

    public static String count(String s) {
        return "count(" + s + ")";
    }

    public static String[] getFetchColumnNames(Aggregation aggregation) {
        switch (aggregation) {
            case NONE:
                return NONE_AGGREGATION_COLUMNS;
            case MIN:
                return MIN_AGGREGATION_COLUMNS;
            case MAX:
                return MAX_AGGREGATION_COLUMNS;
            case SUM:
                return SUM_AGGREGATION_COLUMNS;
            case COUNT:
                return COUNT_AGGREGATION_COLUMNS;
            case AVG:
                return AVG_AGGREGATION_COLUMNS;
            default:
                throw new RuntimeException("Aggregation type: " + aggregation + " is not supported!");
        }
    }
}
