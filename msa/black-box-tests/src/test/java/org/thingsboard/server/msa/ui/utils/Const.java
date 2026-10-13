// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.msa.ui.utils;

import static org.thingsboard.server.msa.TestProperties.getBaseUiUrl;

public class Const {

    public static final String URL = getBaseUiUrl();
    public static final String TENANT_EMAIL = "tenant@thingsboard.org";
    public static final String TENANT_PASSWORD = "tenant";
    public static final String ENTITY_NAME = "Aaz!@#$%^&*()_-+=~`";
    public static final String ROOT_RULE_CHAIN_NAME = "Root Rule Chain";
    public static final String IMPORT_RULE_CHAIN_NAME = "Rule Chain For Import";
    public static final String IMPORT_DEVICE_PROFILE_NAME = "Device Profile For Import";
    public static final String IMPORT_ASSET_PROFILE_NAME = "Asset Profile For Import";
    public static final String IMPORT_RULE_CHAIN_FILE_NAME = "ruleChainForImport.json";
    public static final String IMPORT_DEVICE_PROFILE_FILE_NAME = "deviceProfileForImport.json";
    public static final String IMPORT_ASSET_PROFILE_FILE_NAME = "assetProfileForImport.json";
    public static final String IMPORT_TXT_FILE_NAME = "forImport.txt";
    public static final String DEVICE_ACTIVE_STATE = "Active";
    public static final String DEVICE_INACTIVE_STATE = "Inactive";
    public static final String PUBLIC_CUSTOMER_NAME = "Public";

    //Massages
    public static final String EMPTY_IMPORT_MESSAGE = "No file selected";
    public static final String EMPTY_RULE_CHAIN_MESSAGE = "Rule chain name should be specified!";
    public static final String EMPTY_CUSTOMER_MESSAGE = "Customer title should be specified!";
    public static final String EMPTY_DEVICE_PROFILE_MESSAGE = "Device profile name should be specified!";
    public static final String EMPTY_ASSET_PROFILE_MESSAGE = "Asset profile name should be specified!";
    public static final String EMPTY_GROUP_NAME_MESSAGE = "Entity group name should be specified!";
    public static final String EMPTY_DEVICE_MESSAGE = "Device name should be specified!";
    public static final String DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE = "The rule chain referenced by the device profiles cannot be deleted!";
    public static final String SAME_NAME_WARNING_CUSTOMER_MESSAGE = "Customer with such title already exists!";
    public static final String SAME_NAME_WARNING_DEVICE_PROFILE_MESSAGE = "Device profile with such name already exists!";
    public static final String SAME_NAME_WARNING_ASSET_PROFILE_MESSAGE = "Asset profile with such name already exists!";
    public static final String SAME_NAME_WARNING_ENTITY_GROUP_MESSAGE = "Entity Group with such name, type and owner already exists!";
    public static final String SAME_NAME_WARNING_DEVICE_MESSAGE = "Device with such name already exists!";
    public static final String PHONE_NUMBER_ERROR_MESSAGE = "Phone number is invalid or not possible";
    public static final String NAME_IS_REQUIRED_MESSAGE = "Name is required.";
    public static final String COPY_ENTITY_GROUP_ID_MESSAGE = "Entity group Id has been copied to clipboard";
    public static final String OWNER_NOT_SELECTED_ERROR = "Target owner is required.";
    public static final String DEVICE_PROFILE_IS_REQUIRED_MESSAGE = "Device profile is required";

    //Docs URLs
    public static final String DASHBOARD_GIDE_DOCS_URL = "https://thingsboard.io/docs/pe/user-guide/dashboards/";
    public static final String HTTP_API_DOCS_URL = "https://thingsboard.io/docs/pe/reference/http-api/#telemetry-upload-api";
    public static final String CONNECTIVITY_DOCS_URL = "https://thingsboard.io/docs/pe/getting-started-guides/connectivity/";
    public static final String ALARM_RULES_DOCS_URL = "https://thingsboard.io/docs/pe/user-guide/alarm-rules/";
    public static final String THINGSBOARD_IOT_GATEWAY_DOCS_URL = "https://thingsboard.io/docs/iot-gateway/what-is-iot-gateway/";
    public static final String THINGSBOARD_MQTT_GATEWAY_DOCS_URL = "https://thingsboard.io/docs/paas/reference/gateway-mqtt-api/";
    public static final String THINGSBOARD_INTEGRATION_DOCS_URL = "https://thingsboard.io/docs/user-guide/integrations/";
}
