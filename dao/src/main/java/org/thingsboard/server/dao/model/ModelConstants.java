/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.thingsboard.server.common.data.mobile.app.MobileAppVersionInfo;
import org.thingsboard.server.common.data.mobile.app.StoreInfo;

import java.util.UUID;

public class ModelConstants {

    private ModelConstants() {}

    public static final UUID NULL_UUID = Uuids.startOf(0);
    public static final TenantId SYSTEM_TENANT = TenantId.fromUUID(ModelConstants.NULL_UUID);

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
    public static final String ASSIGNEE_ID_PROPERTY = "assignee_id";
    public static final String DEVICE_ID_PROPERTY = "device_id";
    public static final String TITLE_PROPERTY = "title";
    public static final String NAME_PROPERTY = "name";
    public static final String ALIAS_PROPERTY = "alias";
    public static final String SEARCH_TEXT_PROPERTY = "search_text";
    public static final String ADDITIONAL_INFO_PROPERTY = "additional_info";
    public static final String ENTITY_TYPE_PROPERTY = "entity_type";
    public static final String VERSION_PROPERTY = "version";

    public static final String ENTITY_TYPE_COLUMN = ENTITY_TYPE_PROPERTY;
    public static final String TENANT_ID_COLUMN = "tenant_id";
    public static final String ENTITY_ID_COLUMN = "entity_id";
    public static final String ATTRIBUTE_TYPE_COLUMN = "attribute_type";
    public static final String ATTRIBUTE_KEY_COLUMN = "attribute_key";
    public static final String LAST_UPDATE_TS_COLUMN = "last_update_ts";
    public static final String VERSION_COLUMN = "version";

    /**
     * User constants.
     */
    public static final String USER_PG_HIBERNATE_TABLE_NAME = "tb_user";
    public static final String USER_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String USER_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String USER_EMAIL_PROPERTY = "email";
    public static final String USER_AUTHORITY_PROPERTY = "authority";
    public static final String USER_FIRST_NAME_PROPERTY = "first_name";
    public static final String USER_LAST_NAME_PROPERTY = "last_name";
    public static final String USER_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;

    /**
     * User_credentials constants.
     */
    public static final String USER_CREDENTIALS_TABLE_NAME = "user_credentials";
    public static final String USER_CREDENTIALS_USER_ID_PROPERTY = USER_ID_PROPERTY;
    public static final String USER_CREDENTIALS_ENABLED_PROPERTY = "enabled";
    public static final String USER_CREDENTIALS_PASSWORD_PROPERTY = "password"; //NOSONAR, the constant used to identify password column name (not password value itself)
    public static final String USER_CREDENTIALS_ACTIVATE_TOKEN_PROPERTY = "activate_token";
    public static final String USER_CREDENTIALS_ACTIVATE_TOKEN_EXP_TIME_PROPERTY = "activate_token_exp_time";
    public static final String USER_CREDENTIALS_RESET_TOKEN_PROPERTY = "reset_token";
    public static final String USER_CREDENTIALS_RESET_TOKEN_EXP_TIME_PROPERTY = "reset_token_exp_time";
    public static final String USER_CREDENTIALS_LAST_LOGIN_TS_PROPERTY = "last_login_ts";
    public static final String USER_CREDENTIALS_FAILED_LOGIN_ATTEMPTS_PROPERTY = "failed_login_attempts";

    /**
     * User settings constants.
     */
    public static final String USER_SETTINGS_TABLE_NAME = "user_settings";
    public static final String USER_SETTINGS_USER_ID_PROPERTY = USER_ID_PROPERTY;
    public static final String USER_SETTINGS_TYPE_PROPERTY = "type";
    public static final String USER_SETTINGS_SETTINGS = "settings";

    /**
     * Admin_settings constants.
     */
    public static final String ADMIN_SETTINGS_TABLE_NAME = "admin_settings";

    public static final String ADMIN_SETTINGS_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String ADMIN_SETTINGS_KEY_PROPERTY = "key";
    public static final String ADMIN_SETTINGS_JSON_VALUE_PROPERTY = "json_value";

    /**
     * Contact constants.
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
     * Tenant constants.
     */
    public static final String TENANT_TABLE_NAME = "tenant";
    public static final String TENANT_TITLE_PROPERTY = TITLE_PROPERTY;
    public static final String TENANT_REGION_PROPERTY = "region";
    public static final String TENANT_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;
    public static final String TENANT_TENANT_PROFILE_ID_PROPERTY = "tenant_profile_id";

    /**
     * Tenant profile constants.
     */
    public static final String TENANT_PROFILE_TABLE_NAME = "tenant_profile";
    public static final String TENANT_PROFILE_NAME_PROPERTY = "name";
    public static final String TENANT_PROFILE_PROFILE_DATA_PROPERTY = "profile_data";
    public static final String TENANT_PROFILE_DESCRIPTION_PROPERTY = "description";
    public static final String TENANT_PROFILE_IS_DEFAULT_PROPERTY = "is_default";
    public static final String TENANT_PROFILE_ISOLATED_TB_RULE_ENGINE = "isolated_tb_rule_engine";

    /**
     * Customer constants.
     */
    public static final String CUSTOMER_TABLE_NAME = "customer";
    public static final String CUSTOMER_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String CUSTOMER_TITLE_PROPERTY = TITLE_PROPERTY;
    public static final String CUSTOMER_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;
    public static final String CUSTOMER_IS_PUBLIC_PROPERTY = "is_public";

    /**
     * Device constants.
     */
    public static final String DEVICE_TABLE_NAME = "device";
    public static final String DEVICE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String DEVICE_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String DEVICE_NAME_PROPERTY = "name";
    public static final String DEVICE_TYPE_PROPERTY = "type";
    public static final String DEVICE_LABEL_PROPERTY = "label";
    public static final String DEVICE_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;
    public static final String DEVICE_DEVICE_PROFILE_ID_PROPERTY = "device_profile_id";
    public static final String DEVICE_DEVICE_DATA_PROPERTY = "device_data";
    public static final String DEVICE_FIRMWARE_ID_PROPERTY = "firmware_id";
    public static final String DEVICE_SOFTWARE_ID_PROPERTY = "software_id";

    public static final String DEVICE_CUSTOMER_TITLE_PROPERTY = "customer_title";
    public static final String DEVICE_CUSTOMER_IS_PUBLIC_PROPERTY = "customer_is_public";
    public static final String DEVICE_DEVICE_PROFILE_NAME_PROPERTY = "device_profile_name";
    public static final String DEVICE_ACTIVE_PROPERTY = "active";

    public static final String DEVICE_INFO_VIEW_TABLE_NAME = "device_info_view";

    /**
     * Device profile constants.
     */
    public static final String DEVICE_PROFILE_TABLE_NAME = "device_profile";
    public static final String DEVICE_PROFILE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String DEVICE_PROFILE_NAME_PROPERTY = "name";
    public static final String DEVICE_PROFILE_TYPE_PROPERTY = "type";
    public static final String DEVICE_PROFILE_IMAGE_PROPERTY = "image";
    public static final String DEVICE_PROFILE_TRANSPORT_TYPE_PROPERTY = "transport_type";
    public static final String DEVICE_PROFILE_PROVISION_TYPE_PROPERTY = "provision_type";
    public static final String DEVICE_PROFILE_PROFILE_DATA_PROPERTY = "profile_data";
    public static final String DEVICE_PROFILE_DESCRIPTION_PROPERTY = "description";
    public static final String DEVICE_PROFILE_IS_DEFAULT_PROPERTY = "is_default";
    public static final String DEVICE_PROFILE_DEFAULT_RULE_CHAIN_ID_PROPERTY = "default_rule_chain_id";
    public static final String DEVICE_PROFILE_DEFAULT_DASHBOARD_ID_PROPERTY = "default_dashboard_id";
    public static final String DEVICE_PROFILE_DEFAULT_QUEUE_NAME_PROPERTY = "default_queue_name";
    public static final String DEVICE_PROFILE_PROVISION_DEVICE_KEY = "provision_device_key";
    public static final String DEVICE_PROFILE_FIRMWARE_ID_PROPERTY = "firmware_id";
    public static final String DEVICE_PROFILE_SOFTWARE_ID_PROPERTY = "software_id";
    public static final String DEVICE_PROFILE_DEFAULT_EDGE_RULE_CHAIN_ID_PROPERTY = "default_edge_rule_chain_id";

    /**
     * Asset profile constants.
     */
    public static final String ASSET_PROFILE_TABLE_NAME = "asset_profile";
    public static final String ASSET_PROFILE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String ASSET_PROFILE_NAME_PROPERTY = "name";
    public static final String ASSET_PROFILE_IMAGE_PROPERTY = "image";
    public static final String ASSET_PROFILE_DESCRIPTION_PROPERTY = "description";
    public static final String ASSET_PROFILE_IS_DEFAULT_PROPERTY = "is_default";
    public static final String ASSET_PROFILE_DEFAULT_RULE_CHAIN_ID_PROPERTY = "default_rule_chain_id";
    public static final String ASSET_PROFILE_DEFAULT_DASHBOARD_ID_PROPERTY = "default_dashboard_id";
    public static final String ASSET_PROFILE_DEFAULT_QUEUE_NAME_PROPERTY = "default_queue_name";
    public static final String ASSET_PROFILE_DEFAULT_EDGE_RULE_CHAIN_ID_PROPERTY = "default_edge_rule_chain_id";

    /**
     * Entity view constants.
     */
    public static final String ENTITY_VIEW_TABLE_NAME = "entity_view";
    public static final String ENTITY_VIEW_ENTITY_ID_PROPERTY = ENTITY_ID_COLUMN;
    public static final String ENTITY_VIEW_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String ENTITY_VIEW_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String ENTITY_VIEW_NAME_PROPERTY = DEVICE_NAME_PROPERTY;
    public static final String ENTITY_VIEW_KEYS_PROPERTY = "keys";
    public static final String ENTITY_VIEW_START_TS_PROPERTY = "start_ts";
    public static final String ENTITY_VIEW_END_TS_PROPERTY = "end_ts";
    public static final String ENTITY_VIEW_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;

    /**
     * Audit log constants.
     */
    public static final String AUDIT_LOG_TABLE_NAME = "audit_log";
    public static final String AUDIT_LOG_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String AUDIT_LOG_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String AUDIT_LOG_ENTITY_TYPE_PROPERTY = ENTITY_TYPE_PROPERTY;
    public static final String AUDIT_LOG_ENTITY_ID_PROPERTY = ENTITY_ID_COLUMN;
    public static final String AUDIT_LOG_ENTITY_NAME_PROPERTY = "entity_name";
    public static final String AUDIT_LOG_USER_ID_PROPERTY = USER_ID_PROPERTY;
    public static final String AUDIT_LOG_USER_NAME_PROPERTY = "user_name";
    public static final String AUDIT_LOG_ACTION_TYPE_PROPERTY = "action_type";
    public static final String AUDIT_LOG_ACTION_DATA_PROPERTY = "action_data";
    public static final String AUDIT_LOG_ACTION_STATUS_PROPERTY = "action_status";
    public static final String AUDIT_LOG_ACTION_FAILURE_DETAILS_PROPERTY = "action_failure_details";

    /**
     * Asset constants.
     */
    public static final String ASSET_TABLE_NAME = "asset";
    public static final String ASSET_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String ASSET_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String ASSET_NAME_PROPERTY = "name";
    public static final String ASSET_TYPE_PROPERTY = "type";
    public static final String ASSET_LABEL_PROPERTY = "label";
    public static final String ASSET_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;

    public static final String ASSET_ASSET_PROFILE_ID_PROPERTY = "asset_profile_id";

    /**
     * Alarm constants.
     */
    public static final String ENTITY_ALARM_TABLE_NAME = "entity_alarm";
    public static final String ALARM_TABLE_NAME = "alarm";
    public static final String ALARM_VIEW_NAME = "alarm_info";
    public static final String ALARM_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String ALARM_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String ALARM_TYPE_PROPERTY = "type";
    public static final String ALARM_DETAILS_PROPERTY = ADDITIONAL_INFO_PROPERTY;
    public static final String ALARM_STATUS_PROPERTY = "status";
    public static final String ALARM_ORIGINATOR_ID_PROPERTY = "originator_id";
    public static final String ALARM_ORIGINATOR_NAME_PROPERTY = "originator_name";
    public static final String ALARM_ORIGINATOR_LABEL_PROPERTY = "originator_label";
    public static final String ALARM_ORIGINATOR_TYPE_PROPERTY = "originator_type";
    public static final String ALARM_SEVERITY_PROPERTY = "severity";
    public static final String ALARM_ASSIGNEE_ID_PROPERTY = "assignee_id";
    public static final String ALARM_ASSIGNEE_FIRST_NAME_PROPERTY = "assignee_first_name";
    public static final String ALARM_ASSIGNEE_LAST_NAME_PROPERTY = "assignee_last_name";
    public static final String ALARM_ASSIGNEE_EMAIL_PROPERTY = "assignee_email";
    public static final String ALARM_START_TS_PROPERTY = "start_ts";
    public static final String ALARM_END_TS_PROPERTY = "end_ts";
    public static final String ALARM_ACKNOWLEDGED_PROPERTY = "acknowledged";
    public static final String ALARM_ACK_TS_PROPERTY = "ack_ts";
    public static final String ALARM_CLEARED_PROPERTY = "cleared";
    public static final String ALARM_CLEAR_TS_PROPERTY = "clear_ts";
    public static final String ALARM_ASSIGN_TS_PROPERTY = "assign_ts";
    public static final String ALARM_PROPAGATE_PROPERTY = "propagate";
    public static final String ALARM_PROPAGATE_TO_OWNER_PROPERTY = "propagate_to_owner";
    public static final String ALARM_PROPAGATE_TO_TENANT_PROPERTY = "propagate_to_tenant";
    public static final String ALARM_PROPAGATE_RELATION_TYPES = "propagate_relation_types";

    public static final String ALARM_COMMENT_TABLE_NAME = "alarm_comment";
    public static final String ALARM_COMMENT_ALARM_ID = "alarm_id";
    public static final String ALARM_COMMENT_USER_ID = USER_ID_PROPERTY;
    public static final String ALARM_COMMENT_TYPE = "type";
    public static final String ALARM_COMMENT_COMMENT = "comment";

    /**
     * Entity relation constants.
     */
    public static final String RELATION_TABLE_NAME = "relation";
    public static final String RELATION_FROM_ID_PROPERTY = "from_id";
    public static final String RELATION_FROM_TYPE_PROPERTY = "from_type";
    public static final String RELATION_TO_ID_PROPERTY = "to_id";
    public static final String RELATION_TO_TYPE_PROPERTY = "to_type";
    public static final String RELATION_TYPE_PROPERTY = "relation_type";
    public static final String RELATION_TYPE_GROUP_PROPERTY = "relation_type_group";

    /**
     * Device_credentials constants.
     */
    public static final String DEVICE_CREDENTIALS_TABLE_NAME = "device_credentials";
    public static final String DEVICE_CREDENTIALS_DEVICE_ID_PROPERTY = DEVICE_ID_PROPERTY;
    public static final String DEVICE_CREDENTIALS_CREDENTIALS_TYPE_PROPERTY = "credentials_type";
    public static final String DEVICE_CREDENTIALS_CREDENTIALS_ID_PROPERTY = "credentials_id";
    public static final String DEVICE_CREDENTIALS_CREDENTIALS_VALUE_PROPERTY = "credentials_value";

    /**
     * Widgets_bundle constants.
     */
    public static final String WIDGETS_BUNDLE_TABLE_NAME = "widgets_bundle";
    public static final String WIDGETS_BUNDLE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String WIDGETS_BUNDLE_ALIAS_PROPERTY = ALIAS_PROPERTY;
    public static final String WIDGETS_BUNDLE_TITLE_PROPERTY = TITLE_PROPERTY;
    public static final String WIDGETS_BUNDLE_IMAGE_PROPERTY = "image";
    public static final String WIDGETS_BUNDLE_SCADA_PROPERTY = "scada";
    public static final String WIDGETS_BUNDLE_DESCRIPTION = "description";
    public static final String WIDGETS_BUNDLE_ORDER = "widgets_bundle_order";
    public static final String WIDGET_BUNDLES_PROPERTY = "bundles";

    /**
     * Widget_type constants.
     */
    public static final String WIDGET_TYPE_TABLE_NAME = "widget_type";
    public static final String WIDGET_TYPE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;

    public static final String WIDGET_TYPE_FQN_PROPERTY = "fqn";
    public static final String WIDGET_TYPE_NAME_PROPERTY = "name";
    public static final String WIDGET_TYPE_IMAGE_PROPERTY = "image";
    public static final String WIDGET_TYPE_DESCRIPTION_PROPERTY = "description";
    public static final String WIDGET_TYPE_TAGS_PROPERTY = "tags";
    public static final String WIDGET_TYPE_DESCRIPTOR_PROPERTY = "descriptor";

    public static final String WIDGET_TYPE_DEPRECATED_PROPERTY = "deprecated";

    public static final String WIDGET_TYPE_SCADA_PROPERTY = "scada";

    public static final String WIDGET_TYPE_WIDGET_TYPE_PROPERTY = "widget_type";

    public static final String WIDGET_TYPE_INFO_VIEW_TABLE_NAME = "widget_type_info_view";

    /**
     * Widgets bundle widget constants.
     */
    public static final String WIDGETS_BUNDLE_WIDGET_TABLE_NAME = "widgets_bundle_widget";

    public static final String WIDGET_TYPE_ORDER_PROPERTY = "widget_type_order";

    /**
     * Dashboard constants.
     */
    public static final String DASHBOARD_TABLE_NAME = "dashboard";
    public static final String DASHBOARD_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String DASHBOARD_TITLE_PROPERTY = TITLE_PROPERTY;
    public static final String DASHBOARD_IMAGE_PROPERTY = "image";
    public static final String DASHBOARD_CONFIGURATION_PROPERTY = "configuration";
    public static final String DASHBOARD_ASSIGNED_CUSTOMERS_PROPERTY = "assigned_customers";
    public static final String DASHBOARD_MOBILE_HIDE_PROPERTY = "mobile_hide";
    public static final String DASHBOARD_MOBILE_ORDER_PROPERTY = "mobile_order";

    /**
     * Plugin component metadata constants.
     */
    public static final String COMPONENT_DESCRIPTOR_TABLE_NAME = "component_descriptor";
    public static final String COMPONENT_DESCRIPTOR_TYPE_PROPERTY = "type";
    public static final String COMPONENT_DESCRIPTOR_SCOPE_PROPERTY = "scope";
    public static final String COMPONENT_DESCRIPTOR_CLUSTERING_MODE_PROPERTY = "clustering_mode";
    public static final String COMPONENT_DESCRIPTOR_NAME_PROPERTY = "name";
    public static final String COMPONENT_DESCRIPTOR_CLASS_PROPERTY = "clazz";
    public static final String COMPONENT_DESCRIPTOR_CONFIGURATION_DESCRIPTOR_PROPERTY = "configuration_descriptor";
    public static final String COMPONENT_DESCRIPTOR_CONFIGURATION_VERSION_PROPERTY = "configuration_version";
    public static final String COMPONENT_DESCRIPTOR_ACTIONS_PROPERTY = "actions";
    public static final String COMPONENT_DESCRIPTOR_HAS_QUEUE_NAME_PROPERTY = "has_queue_name";

    /**
     * Event constants.
     */
    public static final String ERROR_EVENT_TABLE_NAME = "error_event";
    public static final String LC_EVENT_TABLE_NAME = "lc_event";
    public static final String STATS_EVENT_TABLE_NAME = "stats_event";
    public static final String RULE_NODE_DEBUG_EVENT_TABLE_NAME = "rule_node_debug_event";
    public static final String RULE_CHAIN_DEBUG_EVENT_TABLE_NAME = "rule_chain_debug_event";
    public static final String CALCULATED_FIELD_DEBUG_EVENT_TABLE_NAME = "cf_debug_event";

    public static final String EVENT_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String EVENT_SERVICE_ID_PROPERTY = "service_id";
    public static final String EVENT_ENTITY_ID_PROPERTY = "entity_id";

    public static final String EVENT_MESSAGES_PROCESSED_COLUMN_NAME = "e_messages_processed";
    public static final String EVENT_ERRORS_OCCURRED_COLUMN_NAME = "e_errors_occurred";

    public static final String EVENT_METHOD_COLUMN_NAME = "e_method";

    public static final String EVENT_TYPE_COLUMN_NAME = "e_type";
    public static final String EVENT_ERROR_COLUMN_NAME = "e_error";
    public static final String EVENT_SUCCESS_COLUMN_NAME = "e_success";

    public static final String EVENT_ENTITY_ID_COLUMN_NAME = "e_entity_id";
    public static final String EVENT_ENTITY_TYPE_COLUMN_NAME = "e_entity_type";
    public static final String EVENT_MSG_ID_COLUMN_NAME = "e_msg_id";
    public static final String EVENT_MSG_TYPE_COLUMN_NAME = "e_msg_type";
    public static final String EVENT_DATA_TYPE_COLUMN_NAME = "e_data_type";
    public static final String EVENT_RELATION_TYPE_COLUMN_NAME = "e_relation_type";
    public static final String EVENT_DATA_COLUMN_NAME = "e_data";
    public static final String EVENT_METADATA_COLUMN_NAME = "e_metadata";
    public static final String EVENT_MESSAGE_COLUMN_NAME = "e_message";

    public static final String EVENT_CALCULATED_FIELD_ID_COLUMN_NAME = "cf_id";
    public static final String EVENT_CALCULATED_FIELD_ARGUMENTS_COLUMN_NAME = "e_args";
    public static final String EVENT_CALCULATED_FIELD_RESULT_COLUMN_NAME = "e_result";

    public static final String DEBUG_MODE = "debug_mode";
    public static final String DEBUG_SETTINGS = "debug_settings";
    public static final String SINGLETON_MODE = "singleton_mode";
    public static final String QUEUE_NAME = "queue_name";

    /**
     * Rule chain constants.
     */
    public static final String RULE_CHAIN_TABLE_NAME = "rule_chain";
    public static final String RULE_CHAIN_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String RULE_CHAIN_NAME_PROPERTY = "name";
    public static final String RULE_CHAIN_TYPE_PROPERTY = "type";
    public static final String RULE_CHAIN_FIRST_RULE_NODE_ID_PROPERTY = "first_rule_node_id";
    public static final String RULE_CHAIN_ROOT_PROPERTY = "root";
    public static final String RULE_CHAIN_CONFIGURATION_PROPERTY = "configuration";

    /**
     * Rule node constants.
     */
    public static final String RULE_NODE_TABLE_NAME = "rule_node";
    public static final String RULE_NODE_CHAIN_ID_PROPERTY = "rule_chain_id";
    public static final String RULE_NODE_TYPE_PROPERTY = "type";
    public static final String RULE_NODE_NAME_PROPERTY = "name";
    public static final String RULE_NODE_VERSION_PROPERTY = "configuration_version";
    public static final String RULE_NODE_CONFIGURATION_PROPERTY = "configuration";

    /**
     * Node state constants.
     */
    public static final String RULE_NODE_STATE_TABLE_NAME = "rule_node_state";
    public static final String RULE_NODE_STATE_NODE_ID_PROPERTY = "rule_node_id";
    public static final String RULE_NODE_STATE_ENTITY_TYPE_PROPERTY = "entity_type";
    public static final String RULE_NODE_STATE_ENTITY_ID_PROPERTY = "entity_id";
    public static final String RULE_NODE_STATE_DATA_PROPERTY = "state_data";

    /**
     * Domain constants.
     */
    public static final String DOMAIN_TABLE_NAME = "domain";
    public static final String DOMAIN_NAME_PROPERTY = "name";
    public static final String DOMAIN_OAUTH2_ENABLED_PROPERTY = "oauth2_enabled";
    public static final String DOMAIN_PROPAGATE_TO_EDGE_PROPERTY = "edge_enabled";

    public static final String DOMAIN_OAUTH2_CLIENT_TABLE_NAME = "domain_oauth2_client";
    public static final String DOMAIN_OAUTH2_CLIENT_CLIENT_ID_PROPERTY = "oauth2_client_id";
    public static final String DOMAIN_OAUTH2_CLIENT_DOMAIN_ID_PROPERTY = "domain_id";

    /**
     * Mobile application constants.
     */
    public static final String MOBILE_APP_TABLE_NAME = "mobile_app";
    public static final String MOBILE_APP_PKG_NAME_PROPERTY = "pkg_name";
    public static final String MOBILE_APP_TITLE_PROPERTY = "title";
    public static final String MOBILE_APP_APP_SECRET_PROPERTY = "app_secret";
    public static final String MOBILE_APP_PLATFORM_TYPE_PROPERTY = "platform_type";
    public static final String MOBILE_APP_STATUS_PROPERTY = "status";
    public static final String MOBILE_APP_VERSION_INFO_PROPERTY = "version_info";
    public static final String MOBILE_APP_STORE_INFO_PROPERTY = "store_info";
    public static final MobileAppVersionInfo MOBILE_APP_VERSION_INFO_EMPTY_OBJECT = new MobileAppVersionInfo();
    public static final StoreInfo MOBILE_APP_STORE_INFO_EMPTY_OBJECT = new StoreInfo();

    /**
     * Mobile application bundle constants.
     */
    public static final String MOBILE_APP_BUNDLE_TABLE_NAME = "mobile_app_bundle";
    public static final String MOBILE_APP_BUNDLE_TITLE_PROPERTY = "title";
    public static final String MOBILE_APP_BUNDLE_DESCRIPTION_PROPERTY = "description";
    public static final String MOBILE_APP_BUNDLE_ANDROID_APP_ID_PROPERTY = "android_app_id";
    public static final String MOBILE_APP_BUNDLE_IOS_APP_ID_PROPERTY = "ios_app_id";
    public static final String MOBILE_APP_BUNDLE_LAYOUT_CONFIG_PROPERTY = "layout_config";
    public static final String MOBILE_APP_BUNDLE_OAUTH2_ENABLED_PROPERTY = "oauth2_enabled";

    public static final String MOBILE_APP_BUNDLE_OAUTH2_CLIENT_TABLE_NAME = "mobile_app_bundle_oauth2_client";
    public static final String MOBILE_APP_BUNDLE_OAUTH2_CLIENT_CLIENT_ID_PROPERTY = "oauth2_client_id";
    public static final String MOBILE_APP_BUNDLE_OAUTH2_CLIENT_MOBILE_APP_BUNDLE_ID_PROPERTY = "mobile_app_bundle_id";


    /**
     * OAuth2 client constants.
     */
    public static final String OAUTH2_CLIENT_TABLE_NAME = "oauth2_client";
    public static final String OAUTH2_CLIENT_REGISTRATION_TEMPLATE_TABLE_NAME = "oauth2_client_registration_template";
    public static final String OAUTH2_TEMPLATE_PROVIDER_ID_PROPERTY = "provider_id";
    public static final String OAUTH2_CLIENT_TITLE_PROPERTY = "title";
    public static final String OAUTH2_CLIENT_ID_PROPERTY = "client_id";
    public static final String OAUTH2_CLIENT_SECRET_PROPERTY = "client_secret";
    public static final String OAUTH2_AUTHORIZATION_URI_PROPERTY = "authorization_uri";
    public static final String OAUTH2_TOKEN_URI_PROPERTY = "token_uri";
    public static final String OAUTH2_SCOPE_PROPERTY = "scope";
    public static final String OAUTH2_PLATFORMS_PROPERTY = "platforms";
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
    public static final String API_USAGE_STATE_TBEL_EXEC_COLUMN = "tbel_exec";
    public static final String API_USAGE_STATE_EMAIL_EXEC_COLUMN = "email_exec";
    public static final String API_USAGE_STATE_SMS_EXEC_COLUMN = "sms_exec";
    public static final String API_USAGE_STATE_ALARM_EXEC_COLUMN = "alarm_exec";

    /**
     * Resource constants.
     */
    public static final String RESOURCE_TABLE_NAME = "resource";
    public static final String RESOURCE_TENANT_ID_COLUMN = TENANT_ID_COLUMN;
    public static final String RESOURCE_TYPE_COLUMN = "resource_type";
    public static final String RESOURCE_SUB_TYPE_COLUMN = "resource_sub_type";
    public static final String RESOURCE_KEY_COLUMN = "resource_key";
    public static final String RESOURCE_TITLE_COLUMN = TITLE_PROPERTY;
    public static final String RESOURCE_FILE_NAME_COLUMN = "file_name";
    public static final String RESOURCE_DATA_COLUMN = "data";
    public static final String RESOURCE_ETAG_COLUMN = "etag";
    public static final String RESOURCE_DESCRIPTOR_COLUMN = "descriptor";
    public static final String RESOURCE_PREVIEW_COLUMN = "preview";
    public static final String RESOURCE_IS_PUBLIC_COLUMN = "is_public";
    public static final String PUBLIC_RESOURCE_KEY_COLUMN = "public_resource_key";

    /**
     * Ota Package constants.
     */
    public static final String OTA_PACKAGE_TABLE_NAME = "ota_package";
    public static final String OTA_PACKAGE_TENANT_ID_COLUMN = TENANT_ID_COLUMN;
    public static final String OTA_PACKAGE_DEVICE_PROFILE_ID_COLUMN = "device_profile_id";
    public static final String OTA_PACKAGE_TYPE_COLUMN = "type";
    public static final String OTA_PACKAGE_TILE_COLUMN = TITLE_PROPERTY;
    public static final String OTA_PACKAGE_VERSION_COLUMN = "version";
    public static final String OTA_PACKAGE_TAG_COLUMN = "tag";
    public static final String OTA_PACKAGE_URL_COLUMN = "url";
    public static final String OTA_PACKAGE_FILE_NAME_COLUMN = "file_name";
    public static final String OTA_PACKAGE_CONTENT_TYPE_COLUMN = "content_type";
    public static final String OTA_PACKAGE_CHECKSUM_ALGORITHM_COLUMN = "checksum_algorithm";
    public static final String OTA_PACKAGE_CHECKSUM_COLUMN = "checksum";
    public static final String OTA_PACKAGE_DATA_COLUMN = "data";
    public static final String OTA_PACKAGE_DATA_SIZE_COLUMN = "data_size";
    public static final String OTA_PACKAGE_ADDITIONAL_INFO_COLUMN = ADDITIONAL_INFO_PROPERTY;

    /**
     * Persisted RPC constants.
     */
    public static final String RPC_TABLE_NAME = "rpc";
    public static final String RPC_TENANT_ID_COLUMN = TENANT_ID_COLUMN;
    public static final String RPC_DEVICE_ID = "device_id";
    public static final String RPC_EXPIRATION_TIME = "expiration_time";
    public static final String RPC_REQUEST = "request";
    public static final String RPC_RESPONSE = "response";
    public static final String RPC_STATUS = "status";
    public static final String RPC_ADDITIONAL_INFO = ADDITIONAL_INFO_PROPERTY;

    /**
     * Edge constants.
     */
    public static final String EDGE_TABLE_NAME = "edge";
    public static final String EDGE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String EDGE_CUSTOMER_ID_PROPERTY = CUSTOMER_ID_PROPERTY;
    public static final String EDGE_ROOT_RULE_CHAIN_ID_PROPERTY = "root_rule_chain_id";
    public static final String EDGE_NAME_PROPERTY = "name";
    public static final String EDGE_LABEL_PROPERTY = "label";
    public static final String EDGE_TYPE_PROPERTY = "type";
    public static final String EDGE_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;

    public static final String EDGE_ROUTING_KEY_PROPERTY = "routing_key";
    public static final String EDGE_SECRET_PROPERTY = "secret";

    /**
     * Edge queue constants.
     */
    public static final String EDGE_EVENT_TABLE_NAME = "edge_event";
    public static final String EDGE_EVENT_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String EDGE_EVENT_SEQUENTIAL_ID_PROPERTY = "seq_id";
    public static final String EDGE_EVENT_EDGE_ID_PROPERTY = "edge_id";
    public static final String EDGE_EVENT_TYPE_PROPERTY = "edge_event_type";
    public static final String EDGE_EVENT_ACTION_PROPERTY = "edge_event_action";
    public static final String EDGE_EVENT_UID_PROPERTY = "edge_event_uid";
    public static final String EDGE_EVENT_ENTITY_ID_PROPERTY = "entity_id";
    public static final String EDGE_EVENT_BODY_PROPERTY = "body";

    public static final String EXTERNAL_ID_PROPERTY = "external_id";

    /**
     * User auth settings constants.
     */
    public static final String USER_AUTH_SETTINGS_TABLE_NAME = "user_auth_settings";
    public static final String USER_AUTH_SETTINGS_USER_ID_PROPERTY = USER_ID_PROPERTY;
    public static final String USER_AUTH_SETTINGS_TWO_FA_SETTINGS = "two_fa_settings";

    /**
     * Cassandra attributes and timeseries constants.
     */
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

    /**
     * Queue constants.
     */
    public static final String QUEUE_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String QUEUE_NAME_PROPERTY = "name";
    public static final String QUEUE_TOPIC_PROPERTY = "topic";
    public static final String QUEUE_POLL_INTERVAL_PROPERTY = "poll_interval";
    public static final String QUEUE_PARTITIONS_PROPERTY = "partitions";
    public static final String QUEUE_CONSUMER_PER_PARTITION = "consumer_per_partition";
    public static final String QUEUE_PACK_PROCESSING_TIMEOUT_PROPERTY = "pack_processing_timeout";
    public static final String QUEUE_SUBMIT_STRATEGY_PROPERTY = "submit_strategy";
    public static final String QUEUE_PROCESSING_STRATEGY_PROPERTY = "processing_strategy";
    public static final String QUEUE_TABLE_NAME = "queue";
    public static final String QUEUE_ADDITIONAL_INFO_PROPERTY = ADDITIONAL_INFO_PROPERTY;

    /**
     * Tenant queue stats constants.
     */
    public static final String QUEUE_STATS_TABLE_NAME = "queue_stats";
    public static final String QUEUE_STATS_TENANT_ID_PROPERTY = TENANT_ID_PROPERTY;
    public static final String QUEUE_STATS_QUEUE_NAME_PROPERTY = "queue_name";
    public static final String QUEUE_STATS_SERVICE_ID_PROPERTY = "service_id";

    /**
     * Notification constants
     */
    public static final String NOTIFICATION_TARGET_TABLE_NAME = "notification_target";
    public static final String NOTIFICATION_TARGET_CONFIGURATION_PROPERTY = "configuration";

    public static final String NOTIFICATION_TABLE_NAME = "notification";
    public static final String NOTIFICATION_REQUEST_ID_PROPERTY = "request_id";
    public static final String NOTIFICATION_RECIPIENT_ID_PROPERTY = "recipient_id";
    public static final String NOTIFICATION_TYPE_PROPERTY = "type";
    public static final String NOTIFICATION_DELIVERY_METHOD_PROPERTY = "delivery_method";
    public static final String NOTIFICATION_SUBJECT_PROPERTY = "subject";
    public static final String NOTIFICATION_TEXT_PROPERTY = "body";
    public static final String NOTIFICATION_ADDITIONAL_CONFIG_PROPERTY = "additional_config";
    public static final String NOTIFICATION_STATUS_PROPERTY = "status";

    public static final String NOTIFICATION_REQUEST_TABLE_NAME = "notification_request";
    public static final String NOTIFICATION_REQUEST_TARGETS_PROPERTY = "targets";
    public static final String NOTIFICATION_REQUEST_TEMPLATE_ID_PROPERTY = "template_id";
    public static final String NOTIFICATION_REQUEST_TEMPLATE_PROPERTY = "template";
    public static final String NOTIFICATION_REQUEST_INFO_PROPERTY = "info";
    public static final String NOTIFICATION_REQUEST_ORIGINATOR_ENTITY_ID_PROPERTY = "originator_entity_id";
    public static final String NOTIFICATION_REQUEST_ORIGINATOR_ENTITY_TYPE_PROPERTY = "originator_entity_type";
    public static final String NOTIFICATION_REQUEST_ADDITIONAL_CONFIG_PROPERTY = "additional_config";
    public static final String NOTIFICATION_REQUEST_STATUS_PROPERTY = "status";
    public static final String NOTIFICATION_REQUEST_RULE_ID_PROPERTY = "rule_id";
    public static final String NOTIFICATION_REQUEST_STATS_PROPERTY = "stats";

    public static final String NOTIFICATION_RULE_TABLE_NAME = "notification_rule";
    public static final String NOTIFICATION_RULE_ENABLED_PROPERTY = "enabled";
    public static final String NOTIFICATION_RULE_TEMPLATE_ID_PROPERTY = "template_id";
    public static final String NOTIFICATION_RULE_TRIGGER_TYPE_PROPERTY = "trigger_type";
    public static final String NOTIFICATION_RULE_TRIGGER_CONFIG_PROPERTY = "trigger_config";
    public static final String NOTIFICATION_RULE_RECIPIENTS_CONFIG_PROPERTY = "recipients_config";
    public static final String NOTIFICATION_RULE_ADDITIONAL_CONFIG_PROPERTY = "additional_config";

    public static final String NOTIFICATION_TEMPLATE_TABLE_NAME = "notification_template";
    public static final String NOTIFICATION_TEMPLATE_NOTIFICATION_TYPE_PROPERTY = "notification_type";
    public static final String NOTIFICATION_TEMPLATE_CONFIGURATION_PROPERTY = "configuration";

    /**
     * Mobile application settings constants.
     */
    public static final String QR_CODE_SETTINGS_TABLE_NAME = "qr_code_settings";
    public static final String QR_CODE_SETTINGS_USE_DEFAULT_APP_PROPERTY = "use_default_app";
    public static final String QR_CODE_SETTINGS_ANDROID_ENABLED_PROPERTY = "android_enabled";
    public static final String QR_CODE_SETTINGS_IOS_ENABLED_PROPERTY = "ios_enabled";
    public static final String QR_CODE_SETTINGS_BUNDLE_ID_PROPERTY = "mobile_app_bundle_id";
    public static final String QR_CODE_SETTINGS_CONFIG_PROPERTY = "qr_code_config";

    /**
     * Calculated fields constants.
     */
    public static final String CALCULATED_FIELD_TABLE_NAME = "calculated_field";
    public static final String CALCULATED_FIELD_TENANT_ID_COLUMN = TENANT_ID_COLUMN;
    public static final String CALCULATED_FIELD_ENTITY_TYPE = ENTITY_TYPE_COLUMN;
    public static final String CALCULATED_FIELD_ENTITY_ID = ENTITY_ID_COLUMN;
    public static final String CALCULATED_FIELD_TYPE = "type";
    public static final String CALCULATED_FIELD_NAME = "name";
    public static final String CALCULATED_FIELD_CONFIGURATION_VERSION = "configuration_version";
    public static final String CALCULATED_FIELD_CONFIGURATION = "configuration";
    public static final String CALCULATED_FIELD_VERSION = "version";

    /**
     * Tasks constants.
     */
    public static final String JOB_TABLE_NAME = "job";
    public static final String JOB_TYPE_PROPERTY = "type";
    public static final String JOB_KEY_PROPERTY = "key";
    public static final String JOB_ENTITY_ID_PROPERTY = "entity_id";
    public static final String JOB_ENTITY_TYPE_PROPERTY = "entity_type";
    public static final String JOB_STATUS_PROPERTY = "status";
    public static final String JOB_CONFIGURATION_PROPERTY = "configuration";
    public static final String JOB_RESULT_PROPERTY = "result";

    /**
     * AI model constants.
     */
    public static final String AI_MODEL_TABLE_NAME = "ai_model";
    public static final String AI_MODEL_TENANT_ID_COLUMN_NAME = TENANT_ID_COLUMN;
    public static final String AI_MODEL_NAME_COLUMN_NAME = NAME_PROPERTY;
    public static final String AI_MODEL_CONFIGURATION_COLUMN_NAME = "configuration";

    /**
     * Api Key constants.
     */
    public static final String API_KEY_TABLE_NAME = "api_key";
    public static final String API_KEY_TENANT_ID_COLUMN_NAME = TENANT_ID_COLUMN;
    public static final String API_KEY_USER_ID_COLUMN_NAME = USER_ID_PROPERTY;
    public static final String API_KEY_VALUE_COLUMN_NAME = "value";
    public static final String API_KEY_EXPIRATION_TIME_COLUMN_NAME = "expiration_time";
    public static final String API_KEY_ENABLED_COLUMN_NAME = "enabled";
    public static final String API_KEY_DESCRIPTION_COLUMN_NAME = "description";

    protected static final String[] NONE_AGGREGATION_COLUMNS = new String[]{LONG_VALUE_COLUMN, DOUBLE_VALUE_COLUMN, BOOLEAN_VALUE_COLUMN, STRING_VALUE_COLUMN, JSON_VALUE_COLUMN, KEY_COLUMN, TS_COLUMN};

    protected static final String[] COUNT_AGGREGATION_COLUMNS = new String[]{count(LONG_VALUE_COLUMN), count(DOUBLE_VALUE_COLUMN), count(BOOLEAN_VALUE_COLUMN), count(STRING_VALUE_COLUMN), count(JSON_VALUE_COLUMN), max(TS_COLUMN)};

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
