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
package org.thingsboard.server.controller;

public class ControllerConstants {

    protected static final String NEW_LINE = "\n\n";
    protected static final String UUID_WIKI_LINK = "[time-based UUID](https://en.wikipedia.org/wiki/Universally_unique_identifier#Version_1_(date-time_and_MAC_address)). ";
    protected static final int DEFAULT_PAGE_SIZE = 1000;
    protected static final String ENTITY_TYPE = "entityType";
    protected static final String CUSTOMER_ID = "customerId";
    protected static final String TENANT_ID = "tenantId";
    protected static final String DEVICE_ID = "deviceId";
    protected static final String PROTOCOL = "protocol";
    protected static final String EDGE_ID = "edgeId";
    protected static final String RPC_ID = "rpcId";
    protected static final String ENTITY_ID = "entityId";
    protected static final String ASSIGNEE_ID = "assigneeId";
    protected static final String PAGE_DATA_PARAMETERS = "You can specify parameters to filter the results. " +
            "The result is wrapped with PageData object that allows you to iterate over result set using pagination. " +
            "See response schema for more details. ";

    protected static final String INLINE_IMAGES = "inlineImages";
    protected static final String INLINE_IMAGES_DESCRIPTION = "Inline images as a data URL (Base64)";
    protected static final String INCLUDE_RESOURCES = "includeResources";
    protected static final String INCLUDE_RESOURCES_DESCRIPTION = "Export used resources and replace resource links with resource metadata";
    protected static final String DASHBOARD_ID_PARAM_DESCRIPTION = "A string value representing the dashboard id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String RPC_ID_PARAM_DESCRIPTION = "A string value representing the rpc id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String DEVICE_ID_PARAM_DESCRIPTION = "A string value representing the device id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String PROTOCOL_PARAM_DESCRIPTION = "A string value representing the device connectivity protocol. Possible values: 'mqtt', 'mqtts', 'http', 'https', 'coap', 'coaps'";
    protected static final String ENTITY_VIEW_ID_PARAM_DESCRIPTION = "A string value representing the entity view id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String DEVICE_PROFILE_ID_PARAM_DESCRIPTION = "A string value representing the device profile id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";

    protected static final String ASSET_PROFILE_ID_PARAM_DESCRIPTION = "A string value representing the asset profile id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String TENANT_PROFILE_ID_PARAM_DESCRIPTION = "A string value representing the tenant profile id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String TENANT_ID_PARAM_DESCRIPTION = "A string value representing the tenant id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String EDGE_ID_PARAM_DESCRIPTION = "A string value representing the edge id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String CUSTOMER_ID_PARAM_DESCRIPTION = "A string value representing the customer id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String USER_ID_PARAM_DESCRIPTION = "A string value representing the user id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String ASSET_ID_PARAM_DESCRIPTION = "A string value representing the asset id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String ALARM_ID_PARAM_DESCRIPTION = "A string value representing the alarm id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String ASSIGN_ID_PARAM_DESCRIPTION = "A string value representing the user id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";

    protected static final String ALARM_COMMENT_ID_PARAM_DESCRIPTION = "A string value representing the alarm comment id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String ENTITY_ID_PARAM_DESCRIPTION = "A string value representing the entity id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String OTA_PACKAGE_ID_PARAM_DESCRIPTION = "A string value representing the ota package id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String ENTITY_TYPE_PARAM_DESCRIPTION = "A string value representing the entity type. For example, 'DEVICE'";
    protected static final String RULE_CHAIN_ID_PARAM_DESCRIPTION = "A string value representing the rule chain id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String RULE_NODE_ID_PARAM_DESCRIPTION = "A string value representing the rule node id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String WIDGET_BUNDLE_ID_PARAM_DESCRIPTION = "A string value representing the widget bundle id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String WIDGET_TYPE_ID_PARAM_DESCRIPTION = "A string value representing the widget type id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String VC_REQUEST_ID_PARAM_DESCRIPTION = "A string value representing the version control request id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String RESOURCE_ID_PARAM_DESCRIPTION = "A string value representing the resource id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String SYSTEM_AUTHORITY_PARAGRAPH = "\n\nAvailable for users with 'SYS_ADMIN' authority.";
    protected static final String SYSTEM_OR_TENANT_AUTHORITY_PARAGRAPH = "\n\nAvailable for users with 'SYS_ADMIN' or 'TENANT_ADMIN' authority.";
    protected static final String TENANT_AUTHORITY_PARAGRAPH = "\n\nAvailable for users with 'TENANT_ADMIN' authority.";
    protected static final String TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH = "\n\nAvailable for users with 'TENANT_ADMIN' or 'CUSTOMER_USER' authority.";
    protected static final String CUSTOMER_AUTHORITY_PARAGRAPH = "\n\nAvailable for users with 'CUSTOMER_USER' authority.";
    protected static final String AVAILABLE_FOR_ANY_AUTHORIZED_USER = "\n\nAvailable for any authorized user. ";
    protected static final String PAGE_SIZE_DESCRIPTION = "Maximum amount of entities in a one page";
    protected static final String PAGE_NUMBER_DESCRIPTION = "Sequence number of page starting from 0";
    protected static final String DEVICE_TYPE_DESCRIPTION = "Device type as the name of the device profile";
    protected static final String DEVICE_ACTIVE_PARAM_DESCRIPTION = "A boolean value representing the device active flag.";
    protected static final String ENTITY_VIEW_TYPE_DESCRIPTION = "Entity View type";
    protected static final String ASSET_TYPE_DESCRIPTION = "Asset type";
    protected static final String EDGE_TYPE_DESCRIPTION = "A string value representing the edge type. For example, 'default'";
    protected static final String RULE_CHAIN_TYPE_DESCRIPTION = "Rule chain type (CORE or EDGE)";
    protected static final String ASSET_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the asset name.";
    protected static final String DASHBOARD_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the dashboard title.";
    protected static final String WIDGET_BUNDLE_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the widget bundle title.";
    protected static final String WIDGET_TYPE_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the widget type name.";
    protected static final String RPC_TEXT_SEARCH_DESCRIPTION = "Not implemented. Leave empty.";
    protected static final String DEVICE_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the device name.";
    protected static final String ENTITY_VIEW_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the entity view name.";
    protected static final String USER_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the user email.";
    protected static final String TENANT_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the tenant name.";
    protected static final String TENANT_PROFILE_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the tenant profile name.";
    protected static final String RULE_CHAIN_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the rule chain name.";
    protected static final String DEVICE_PROFILE_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the device profile name.";
    protected static final String AI_MODEL_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the AI model name, provider and model ID.";

    protected static final String ASSET_PROFILE_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the asset profile name.";
    protected static final String CUSTOMER_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the customer title.";
    protected static final String EDGE_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the edge name.";
    protected static final String EVENT_TEXT_SEARCH_DESCRIPTION = "The value is not used in searching.";
    protected static final String AUDIT_LOG_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on one of the next properties: entityType, entityName, userName, actionType, actionStatus.";
    protected static final String CF_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the calculated field name.";
    protected static final String SORT_PROPERTY_DESCRIPTION = "Property of entity to sort by";

    protected static final String SORT_ORDER_DESCRIPTION = "Sort order. ASC (ASCENDING) or DESC (DESCENDING)";
    protected static final String DEVICE_INFO_DESCRIPTION = "Device Info is an extension of the default Device object that contains information about the assigned customer name and device profile name. ";
    protected static final String ASSET_INFO_DESCRIPTION = "Asset Info is an extension of the default Asset object that contains information about the assigned customer name. ";
    protected static final String ALARM_INFO_DESCRIPTION = "Alarm Info is an extension of the default Alarm object that also contains name of the alarm originator.";
    protected static final String RELATION_INFO_DESCRIPTION = "Relation Info is an extension of the default Relation object that contains information about the 'from' and 'to' entity names. ";
    protected static final String EDGE_INFO_DESCRIPTION = "Edge Info is an extension of the default Edge object that contains information about the assigned customer name. ";
    protected static final String DEVICE_PROFILE_INFO_DESCRIPTION = "Device Profile Info is a lightweight object that includes main information about Device Profile excluding the heavyweight configuration object. ";

    protected static final String ASSET_PROFILE_INFO_DESCRIPTION = "Asset Profile Info is a lightweight object that includes main information about Asset Profile. ";
    protected static final String QUEUE_SERVICE_TYPE_DESCRIPTION = "Service type (implemented only for the TB-RULE-ENGINE)";
    protected static final String QUEUE_QUEUE_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the queue name.";
    protected static final String QUEUE_STATS_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the queue name or service id.";
    protected static final String QUEUE_ID_PARAM_DESCRIPTION = "A string value representing the queue id. For example, '784f394c-42b6-435a-983c-b7beff2784f9'";
    protected static final String QUEUE_STATS_ID_PARAM_DESCRIPTION = "A string value representing the queue stats id. For example, '687f294c-42b6-435a-983c-b7beff2784f9'";
    protected static final String QUEUE_NAME_PARAM_DESCRIPTION = "A string value representing the queue id. For example, 'Main'";
    protected static final String OTA_PACKAGE_INFO_DESCRIPTION = "OTA Package Info is a lightweight object that includes main information about the OTA Package excluding the heavyweight data. ";
    protected static final String OTA_PACKAGE_DESCRIPTION = "OTA Package is a heavyweight object that includes main information about the OTA Package and also data. ";
    protected static final String OTA_PACKAGE_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the ota package title.";
    protected static final String RESOURCE_INFO_DESCRIPTION = "Resource Info is a lightweight object that includes main information about the Resource excluding the heavyweight data. ";
    protected static final String RESOURCE_DESCRIPTION = "Resource is a heavyweight object that includes main information about the Resource and also data. ";

    protected static final String RESOURCE_IMAGE_SUB_TYPE_DESCRIPTION = "A string value representing resource sub-type.";

    protected static final String RESOURCE_INCLUDE_SYSTEM_IMAGES_DESCRIPTION = "Use 'true' to include system images. Disabled by default. Ignored for requests by users with system administrator authority.";

    protected static final String RESOURCE_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the resource title.";
    protected static final String RESOURCE_TYPE = "A string value representing the resource type.";
    protected static final String RESOURCE_SUB_TYPE = "A string value representing the resource sub-type.";

    protected static final String LWM2M_OBJECT_DESCRIPTION = "LwM2M Object is a object that includes information about the LwM2M model which can be used in transport configuration for the LwM2M device profile. ";

    protected static final String DEVICE_NAME_DESCRIPTION = "A string value representing the Device name.";
    protected static final String ASSET_NAME_DESCRIPTION = "A string value representing the Asset name.";

    protected static final String EVENT_START_TIME_DESCRIPTION = "Timestamp. Events with creation time before it won't be queried.";
    protected static final String EVENT_END_TIME_DESCRIPTION = "Timestamp. Events with creation time after it won't be queried.";

    protected static final String EDGE_UNASSIGN_ASYNC_FIRST_STEP_DESCRIPTION = "Unassignment works in async way - first, 'unassign' notification event pushed to edge queue on platform. ";
    protected static final String EDGE_UNASSIGN_RECEIVE_STEP_DESCRIPTION = "(Edge will receive this instantly, if it's currently connected, or once it's going to be connected to platform). ";
    protected static final String EDGE_ASSIGN_ASYNC_FIRST_STEP_DESCRIPTION = "Assignment works in async way - first, notification event pushed to edge service queue on platform. ";
    protected static final String EDGE_ASSIGN_RECEIVE_STEP_DESCRIPTION = "(Edge will receive this instantly, if it's currently connected, or once it's going to be connected to platform). ";

    protected static final String ENTITY_VERSION_TEXT_SEARCH_DESCRIPTION = "The case insensitive 'substring' filter based on the entity version name.";
    protected static final String VERSION_ID_PARAM_DESCRIPTION = "Version id, for example fd82625bdd7d6131cf8027b44ee967012ecaf990. Represents commit hash.";
    protected static final String BRANCH_PARAM_DESCRIPTION = "The name of the working branch, for example 'master'";

    protected static final String MARKDOWN_CODE_BLOCK_START = "```json\n";
    protected static final String MARKDOWN_CODE_BLOCK_END = "\n```";
    protected static final String EVENT_ERROR_FILTER_OBJ = MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "   \"eventType\":\"ERROR\",\n" +
            "   \"server\":\"ip-172-31-24-152\",\n" +
            "   \"method\":\"onClusterEventMsg\",\n" +
            "   \"errorStr\":\"Error Message\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END;
    protected static final String EVENT_LC_EVENT_FILTER_OBJ = MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "   \"eventType\":\"LC_EVENT\",\n" +
            "   \"server\":\"ip-172-31-24-152\",\n" +
            "   \"event\":\"STARTED\",\n" +
            "   \"status\":\"Success\",\n" +
            "   \"errorStr\":\"Error Message\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END;
    protected static final String EVENT_STATS_FILTER_OBJ = MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "   \"eventType\":\"STATS\",\n" +
            "   \"server\":\"ip-172-31-24-152\",\n" +
            "   \"messagesProcessed\":10,\n" +
            "   \"errorsOccurred\":5\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END;
    protected static final String DEBUG_FILTER_OBJ =
            "   \"msgDirectionType\":\"IN\",\n" +
                    "   \"server\":\"ip-172-31-24-152\",\n" +
                    "   \"dataSearch\":\"humidity\",\n" +
                    "   \"metadataSearch\":\"deviceName\",\n" +
                    "   \"entityName\":\"DEVICE\",\n" +
                    "   \"relationType\":\"Success\",\n" +
                    "   \"entityId\":\"de9d54a0-2b7a-11ec-a3cc-23386423d98f\",\n" +
                    "   \"msgType\":\"POST_TELEMETRY_REQUEST\",\n" +
                    "   \"isError\":\"false\",\n" +
                    "   \"errorStr\":\"Error Message\"\n" +
                    "}";
    protected static final String EVENT_DEBUG_RULE_NODE_FILTER_OBJ = MARKDOWN_CODE_BLOCK_START + "{\n" +
            "   \"eventType\":\"DEBUG_RULE_NODE\",\n" + DEBUG_FILTER_OBJ + MARKDOWN_CODE_BLOCK_END;
    protected static final String EVENT_DEBUG_RULE_CHAIN_FILTER_OBJ = MARKDOWN_CODE_BLOCK_START + "{\n" +
            "   \"eventType\":\"DEBUG_RULE_CHAIN\",\n" + DEBUG_FILTER_OBJ + MARKDOWN_CODE_BLOCK_END;

    protected static final String EVENT_DEBUG_CALCULATED_FIELD_FILTER_OBJ = MARKDOWN_CODE_BLOCK_START + "{\n" +
            "   \"eventType\":\"DEBUG_CALCULATED_FIELD\",\n" +
            "   \"server\":\"ip-172-31-24-152\",\n" +
            "   \"isError\":\"false\",\n" +
            "   \"errorStr\":\"Error Message\"\n" +
            "   \"entityId\":\"cf4b8741-f618-471f-ae08-d881ca7f9fe9\",\n" +
            "   \"msgId\":\"5cf7d3a0-aee7-40dd-a737-ade05528e7eb\",\n" +
            "   \"msgType\":\"POST_TELEMETRY_REQUEST\",\n" +
            "   \"arguments\":\"{\n" +
            "    \"x\": {\n" +
            "      \"ts\": 1739432016629,\n" +
            "      \"value\": 20\n" +
            "    },\n" +
            "    \"y\": {\n" +
            "      \"ts\": 1739429717656,\n" +
            "      \"value\": 12\n" +
            "    }\n" +
            "  }\",\n" +
            "   \"result\":\"{\n" +
            "    \"x + y\": 32\n" +
            "  }\",\n" +
            "}" + MARKDOWN_CODE_BLOCK_END;

    protected static final String IS_BOOTSTRAP_SERVER_PARAM_DESCRIPTION = "A Boolean value representing the Server SecurityInfo for future Bootstrap client mode settings. Values: 'true' for Bootstrap Server; 'false' for Lwm2m Server. ";

    protected static final String DEVICE_WITH_DEVICE_CREDENTIALS_ACCESS_TOKEN_PARAM_DESCRIPTION =
                    "{\n" +
                    "  \"device\": {\n" +
                    "    \"name\":\"Name_DeviceWithCredantial_AccessToken\",\n" +
                    "    \"label\":\"Label_DeviceWithCredantial_AccessToken\",\n" +
                    "    \"deviceProfileId\":{\n" +
                    "      \"id\":\"9d9588c0-06c9-11ee-b618-19be30fdeb60\",\n" +
                    "      \"entityType\":\"DEVICE_PROFILE\"\n" +
                    "     }\n" +
                    "   },\n" +
                    "  \"credentials\": {\n" +
                    "    \"credentialsType\": \"ACCESS_TOKEN\",\n" +
                    "    \"credentialsId\": \"6hmxew8pmmzng4e3une2\"\n" +
                    "   }\n" +
                    "}";

    protected static final String DEVICE_UPDATE_CREDENTIALS_ACCESS_TOKEN_PARAM_DESCRIPTION =
                    "{\n" +
                    "  \"id\": {\n" +
                    "    \"id\":\"c886a090-168d-11ee-87c9-6f157dbc816a\"\n" +
                    "   },\n" +
                    "  \"deviceId\": {\n" +
                    "    \"id\":\"c5fb3ac0-168d-11ee-87c9-6f157dbc816a\",\n" +
                    "    \"entityType\":\"DEVICE\"\n" +
                    "   },\n" +
                    "  \"credentialsType\": \"ACCESS_TOKEN\",\n" +
                    "  \"credentialsId\": \"6hmxew8pmmzng4e3une4\"\n" +
                    "}";

    protected static final String DEVICE_WITH_DEVICE_CREDENTIALS_ACCESS_TOKEN_DEFAULT_PARAM_DESCRIPTION =
                    "{\n" +
                    "  \"device\": {\n" +
                    "    \"name\":\"Name_DeviceWithCredantial_AccessToken_Default\",\n" +
                    "    \"label\":\"Label_DeviceWithCredantial_AccessToken_Default\",\n" +
                    "    \"type\": \"default\"\n" +
                    "   },\n" +
                    "  \"credentials\": {\n" +
                    "    \"credentialsType\": \"ACCESS_TOKEN\",\n" +
                    "    \"credentialsId\": \"6hmxew8pmmzng4e3une3\"\n" +
                    "   }\n" +
                    "}";

    protected static final String certificateValue = "\"-----BEGIN CERTIFICATE----- " +
        "MIICMTCCAdegAwIBAgIUI9dBuwN6pTtK6uZ03rkiCwV4wEYwCgYIKoZIzj0EAwIwbjELMAkGA1UEBhMCVVMxETAPBgNVBAgMCE5ldyBZb3JrMRowGAYDVQQKDBFUaGluZ3NCb2FyZCwgSW5jLjEwMC4GA1UEAwwnZGV2aWNlQ2VydGlmaWNhdGVAWDUwOVByb3Zpc2lvblN0cmF0ZWd5MB4XDTIzMDMyOTE0NTYxN1oXDTI0MDMyODE0NTYxN1owbjELMAkGA1UEBhMCVVMxETAPBgNVBAgMCE5ldyBZb3JrMRowGAYDVQQKDBFUaGluZ3NCb2FyZCwgSW5jLjEwMC4GA1UEAwwnZGV2aWNlQ2VydGlmaWNhdGVAWDUwOVByb3Zpc2lvblN0cmF0ZWd5MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE9Zo791qKQiGNBm11r4ZGxh+w+ossZL3xc46ufq5QckQHP7zkD2XDAcmP5GvdkM1sBFN9AWaCkQfNnWmfERsOOKNTMFEwHQYDVR0OBBYEFFFc5uyCyglQoZiKhzXzMcQ3BKORMB8GA1UdIwQYMBaAFFFc5uyCyglQoZiKhzXzMcQ3BKORMA8GA1UdEwEB/wQFMAMBAf8wCgYIKoZIzj0EAwIDSAAwRQIhANbA9CuhoOifZMMmqkpuld+65CR+ItKdXeRAhLMZuccuAiB0FSQB34zMutXrZj1g8Gl5OkE7YryFHbei1z0SveHR8g== " +
        "-----END CERTIFICATE-----\"";

    protected static final String certificateId =  "\"84f5911765abba1f96bf4165604e9e90338fc6214081a8e623b6ff9669aedb27\"";

    protected static final String certificateValueUpdate = "\"-----BEGIN CERTIFICATE----- " +
        "MIICMTCCAdegAwIBAgIUUEKxS9hTz4l+oLUMF0LV6TC/gCIwCgYIKoZIzj0EAwIwbjELMAkGA1UEBhMCVVMxETAPBgNVBAgMCE5ldyBZb3JrMRowGAYDVQQKDBFUaGluZ3NCb2FyZCwgSW5jLjEwMC4GA1UEAwwnZGV2aWNlUHJvZmlsZUNlcnRAWDUwOVByb3Zpc2lvblN0cmF0ZWd5MB4XDTIzMDMyOTE0NTczNloXDTI0MDMyODE0NTczNlowbjELMAkGA1UEBhMCVVMxETAPBgNVBAgMCE5ldyBZb3JrMRowGAYDVQQKDBFUaGluZ3NCb2FyZCwgSW5jLjEwMC4GA1UEAwwnZGV2aWNlUHJvZmlsZUNlcnRAWDUwOVByb3Zpc2lvblN0cmF0ZWd5MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAECMlWO72krDoUL9FQjUmSCetkhaEGJUfQkdSfkLSNa0GyAEIMbfmzI4zITeapunu4rGet3EMyLydQzuQanBicp6NTMFEwHQYDVR0OBBYEFHpZ78tPnztNii4Da/yCw6mhEIL3MB8GA1UdIwQYMBaAFHpZ78tPnztNii4Da/yCw6mhEIL3MA8GA1UdEwEB/wQFMAMBAf8wCgYIKoZIzj0EAwIDSAAwRQIgJ7qyMFqNcwSYkH6o+UlQXzLWfwZbNjVk+aR7foAZNGsCIQDsd7v3WQIGHiArfZeDs1DLEDuV/2h6L+ZNoGNhEKL+1A== " +
        "-----END CERTIFICATE-----\"";

    protected static final String certificateIdUpdate =  "\"6b8adb49015500e51a527acd332b51684ab9b49b4ade03a9582a44c455e2e9b6\"";

    protected static final String DEVICE_WITH_DEVICE_CREDENTIALS_X509_CERTIFICATE_PARAM_DESCRIPTION =
            "{\n" +
            "  \"device\": {\n" +
            "    \"name\":\"Name_DeviceWithCredantial_X509_Certificate\",\n" +
            "    \"label\":\"Label_DeviceWithCredantial_X509_Certificate\",\n" +
            "    \"deviceProfileId\":{\n" +
            "      \"id\":\"9d9588c0-06c9-11ee-b618-19be30fdeb60\",\n" +
            "      \"entityType\":\"DEVICE_PROFILE\"\n" +
            "     }\n" +
            "   },\n" +
            "  \"credentials\": {\n" +
            "    \"credentialsType\": \"X509_CERTIFICATE\",\n" +
            "    \"credentialsId\": " + certificateId + ",\n" +
            "    \"credentialsValue\": " + certificateValue + "\n" +
            "   }\n" +
            "}";

    protected static final String DEVICE_UPDATE_CREDENTIALS_X509_CERTIFICATE_PARAM_DESCRIPTION =
            "{\n" +
            "  \"id\": {\n" +
            "    \"id\":\"309bd9c0-14f4-11ee-9fc9-d9b7463abb63\"\n" +
            "   },\n" +
            "  \"deviceId\": {\n" +
            "    \"id\":\"3092b200-14f4-11ee-9fc9-d9b7463abb63\",\n" +
            "    \"entityType\":\"DEVICE\"\n" +
            "   },\n" +
            "  \"credentialsType\": \"X509_CERTIFICATE\",\n" +
            "  \"credentialsId\": " + certificateIdUpdate + ",\n" +
            "  \"credentialsValue\": " + certificateValueUpdate + "\n" +
            "}";

    protected static final String MQTT_BASIC_VALUE = "\"{\\\"clientId\\\":\\\"5euh5nzm34bjjh1efmlt\\\",\\\"userName\\\":\\\"onasd1lgwasmjl7v2v7h\\\",\\\"password\\\":\\\"b9xtm4ny8kt9zewaga5o\\\"}\"";

    protected static final String MQTT_BASIC_VALUE_UPDATE = "\"{\\\"clientId\\\":\\\"juy03yv4owqxcmqhqtvk\\\",\\\"userName\\\":\\\"ov19fxca0cyjn7lm7w7u\\\",\\\"password\\\":\\\"twy94he114dfi9usyk1o\\\"}\"";

    protected static final String DEVICE_WITH_DEVICE_CREDENTIALS_MQTT_BASIC_PARAM_DESCRIPTION =
            "{\n" +
            "  \"device\": {\n" +
            "    \"name\":\"Name_DeviceWithCredantial_MQTT_Basic\",\n" +
            "    \"label\":\"Label_DeviceWithCredantial_MQTT_Basic\",\n" +
            "    \"deviceProfileId\":{\n" +
            "      \"id\":\"9d9588c0-06c9-11ee-b618-19be30fdeb60\",\n" +
            "      \"entityType\":\"DEVICE_PROFILE\"\n" +
            "     }\n" +
            "   },\n" +
            "  \"credentials\": {\n" +
            "    \"credentialsType\": \"MQTT_BASIC\",\n" +
            "    \"credentialsValue\": " + MQTT_BASIC_VALUE + "\n" +
            "   }\n" +
            "}";

    protected static final String DEVICE_UPDATE_CREDENTIALS_MQTT_BASIC_PARAM_DESCRIPTION =
            "{\n" +
            "  \"id\": {\n" +
            "    \"id\":\"d877ffb0-14f5-11ee-9fc9-d9b7463abb63\"\n" +
            "   },\n" +
            "  \"deviceId\": {\n" +
            "    \"id\":\"d875dcd0-14f5-11ee-9fc9-d9b7463abb63\",\n" +
            "    \"entityType\":\"DEVICE\"\n" +
            "   },\n" +
            "  \"credentialsType\": \"MQTT_BASIC\",\n" +
            "  \"credentialsValue\": " + MQTT_BASIC_VALUE_UPDATE + "\n" +
            "}";

    protected static final String CREDENTIALS_VALUE_LVM2M_RPK_DESCRIPTION =
       "       \"{" +
                   "\\\"client\\\":{ " +
                       "\\\"endpoint\\\":\\\"LwRpk00000000\\\", " +
                       "\\\"securityConfigClientMode\\\":\\\"RPK\\\", " +
                       "\\\"key\\\":\\\"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEUEBxNl/RcYJNm8mk91CyVXoIJiROYDlXcSSqK6e5bDHwOW4ZiN2lNnXalyF0Jxw8MbAytnDMERXyAja5VEMeVQ==\\\"" +
               "   }, " +
                   "\\\"bootstrap\\\":{ " +
                        "\\\"bootstrapServer\\\":{ " +
                            "\\\"securityMode\\\":\\\"RPK\\\", " +
                            "\\\"clientPublicKeyOrId\\\":\\\"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEUEBxNl/RcYJNm8mk91CyVXoIJiROYDlXcSSqK6e5bDHwOW4ZiN2lNnXalyF0Jxw8MbAytnDMERXyAja5VEMeVQ==\\\", " +
                            "\\\"clientSecretKey\\\":\\\"MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgd9GAx7yZW37autew5KZykn4IgRpge/tZSjnudnZJnMahRANCAARQQHE2X9Fxgk2byaT3ULJVeggmJE5gOVdxJKorp7lsMfA5bhmI3aU2ddqXIXQnHDwxsDK2cMwRFfICNrlUQx5V\\\"" +
                        "}, " +
                        "\\\"lwm2mServer\\\":{ \\\"securityMode\\\":\\\"RPK\\\", " +
                            "\\\"clientPublicKeyOrId\\\":\\\"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEUEBxNl/RcYJNm8mk91CyVXoIJiROYDlXcSSqK6e5bDHwOW4ZiN2lNnXalyF0Jxw8MbAytnDMERXyAja5VEMeVQ==\\\", " +
                            "\\\"clientSecretKey\\\":\\\"MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgd9GAx7yZW37autew5KZykn4IgRpge/tZSjnudnZJnMahRANCAARQQHE2X9Fxgk2byaT3ULJVeggmJE5gOVdxJKorp7lsMfA5bhmI3aU2ddqXIXQnHDwxsDK2cMwRFfICNrlUQx5V\\\"" +
                        "}" +
                   "} " +
               "}\"";

    protected static final String CREDENTIALS_VALUE_UPDATE_LVM2M_RPK_DESCRIPTION =
       "       \"{" +
                   "\\\"client\\\":{ " +
                       "\\\"endpoint\\\":\\\"LwRpk00000000\\\", " +
                       "\\\"securityConfigClientMode\\\":\\\"RPK\\\", " +
                       "\\\"key\\\":\\\"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEdvBZZ2vQRK9wgDhctj6B1c7bxR3Z0wYg1+YdoYFnVUKWb+rIfTTyYK9tmQJx5Vlb5fxdLnVv1RJOPiwsLIQbAA==\\\"" +
               "   }, " +
                   "\\\"bootstrap\\\":{ " +
                        "\\\"bootstrapServer\\\":{ " +
                            "\\\"securityMode\\\":\\\"RPK\\\", " +
                            "\\\"clientPublicKeyOrId\\\":\\\"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEUEBxNl/RcYJNm8mk91CyVXoIJiROYDlXcSSqK6e5bDHwOW4ZiN2lNnXalyF0Jxw8MbAytnDMERXyAja5VEMeVQ==\\\", " +
                            "\\\"clientSecretKey\\\":\\\"MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgd9GAx7yZW37autew5KZykn4IgRpge/tZSjnudnZJnMahRANCAARQQHE2X9Fxgk2byaT3ULJVeggmJE5gOVdxJKorp7lsMfA5bhmI3aU2ddqXIXQnHDwxsDK2cMwRFfICNrlUQx5V\\\"" +
                        "}, " +
                        "\\\"lwm2mServer\\\":{ \\\"securityMode\\\":\\\"RPK\\\", " +
                            "\\\"clientPublicKeyOrId\\\":\\\"MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEUEBxNl/RcYJNm8mk91CyVXoIJiROYDlXcSSqK6e5bDHwOW4ZiN2lNnXalyF0Jxw8MbAytnDMERXyAja5VEMeVQ==\\\", " +
                            "\\\"clientSecretKey\\\":\\\"MIGHAgEAMBMGByqGSM49AgEGCCqGSM49AwEHBG0wawIBAQQgd9GAx7yZW37autew5KZykn4IgRpge/tZSjnudnZJnMahRANCAARQQHE2X9Fxgk2byaT3ULJVeggmJE5gOVdxJKorp7lsMfA5bhmI3aU2ddqXIXQnHDwxsDK2cMwRFfICNrlUQx5V\\\"" +
                        "}" +
                   "} " +
               "}\"";

   protected static final String DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_LVM2M_RPK_DESCRIPTION =
           "{\n" +
           "  \"device\": {\n" +
           "    \"name\":\"Name_LwRpk00000000\",\n" +
           "    \"label\":\"Label_LwRpk00000000\",\n" +
           "    \"deviceProfileId\":{\n" +
           "      \"id\":\"a660bd50-10ef-11ee-8737-b5634e73c779\",\n" +
           "      \"entityType\":\"DEVICE_PROFILE\"\n" +
           "     }\n" +
           "   },\n" +
           "  \"credentials\": {\n" +
           "    \"credentialsType\": \"LWM2M_CREDENTIALS\",\n" +
           "    \"credentialsId\": \"LwRpk00000000\",\n" +
           "    \"credentialsValue\":\n" + CREDENTIALS_VALUE_LVM2M_RPK_DESCRIPTION + "\n" +
           "   }\n" +
           "}";

   protected static final String DEVICE_UPDATE_CREDENTIALS_PARAM_LVM2M_RPK_DESCRIPTION =
           "{\n" +
           "  \"id\": {\n" +
           "    \"id\":\"e238d4d0-1689-11ee-98c6-1713c1be5a8e\"\n" +
           "   },\n" +
           "  \"deviceId\": {\n" +
           "    \"id\":\"e232e160-1689-11ee-98c6-1713c1be5a8e\",\n" +
           "    \"entityType\":\"DEVICE\"\n" +
           "   },\n" +
           "  \"credentialsType\": \"LWM2M_CREDENTIALS\",\n" +
           "  \"credentialsId\": \"LwRpk00000000\",\n" +
           "  \"credentialsValue\":\n" + CREDENTIALS_VALUE_UPDATE_LVM2M_RPK_DESCRIPTION + "\n" +
           "}";

   protected static final String DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_ACCESS_TOKEN_DESCRIPTION_MARKDOWN =
            MARKDOWN_CODE_BLOCK_START + DEVICE_WITH_DEVICE_CREDENTIALS_ACCESS_TOKEN_PARAM_DESCRIPTION + MARKDOWN_CODE_BLOCK_END;

   protected static final String DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_ACCESS_TOKEN_DEFAULT_DESCRIPTION_MARKDOWN =
            MARKDOWN_CODE_BLOCK_START + DEVICE_WITH_DEVICE_CREDENTIALS_ACCESS_TOKEN_DEFAULT_PARAM_DESCRIPTION + MARKDOWN_CODE_BLOCK_END;

  protected static final String DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_X509_CERTIFICATE_DESCRIPTION_MARKDOWN =
            MARKDOWN_CODE_BLOCK_START + DEVICE_WITH_DEVICE_CREDENTIALS_X509_CERTIFICATE_PARAM_DESCRIPTION + MARKDOWN_CODE_BLOCK_END;

  protected static final String DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_MQTT_BASIC_DESCRIPTION_MARKDOWN =
            MARKDOWN_CODE_BLOCK_START + DEVICE_WITH_DEVICE_CREDENTIALS_MQTT_BASIC_PARAM_DESCRIPTION + MARKDOWN_CODE_BLOCK_END;

   protected static final String DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_LVM2M_RPK_DESCRIPTION_MARKDOWN =
            MARKDOWN_CODE_BLOCK_START + DEVICE_WITH_DEVICE_CREDENTIALS_PARAM_LVM2M_RPK_DESCRIPTION + MARKDOWN_CODE_BLOCK_END;

    protected static final String DEVICE_UPDATE_CREDENTIALS_PARAM_ACCESS_TOKEN_DESCRIPTION_MARKDOWN =
            MARKDOWN_CODE_BLOCK_START + DEVICE_UPDATE_CREDENTIALS_ACCESS_TOKEN_PARAM_DESCRIPTION + MARKDOWN_CODE_BLOCK_END;

    protected static final String DEVICE_UPDATE_CREDENTIALS_PARAM_X509_CERTIFICATE_DESCRIPTION_MARKDOWN =
            MARKDOWN_CODE_BLOCK_START + DEVICE_UPDATE_CREDENTIALS_X509_CERTIFICATE_PARAM_DESCRIPTION + MARKDOWN_CODE_BLOCK_END;

    protected static final String DEVICE_UPDATE_CREDENTIALS_PARAM_MQTT_BASIC_DESCRIPTION_MARKDOWN =
            MARKDOWN_CODE_BLOCK_START + DEVICE_UPDATE_CREDENTIALS_MQTT_BASIC_PARAM_DESCRIPTION + MARKDOWN_CODE_BLOCK_END;

    protected static final String DEVICE_UPDATE_CREDENTIALS_PARAM_LVM2M_RPK_DESCRIPTION_MARKDOWN =
            MARKDOWN_CODE_BLOCK_START + DEVICE_UPDATE_CREDENTIALS_PARAM_LVM2M_RPK_DESCRIPTION + MARKDOWN_CODE_BLOCK_END;



    protected static final String FILTER_VALUE_TYPE = NEW_LINE + "## Value Type and Operations" + NEW_LINE +
            "Provides a hint about the data type of the entity field that is defined in the filter key. " +
            "The value type impacts the list of possible operations that you may use in the corresponding predicate. For example, you may use 'STARTS_WITH' or 'END_WITH', but you can't use 'GREATER_OR_EQUAL' for string values." +
            "The following filter value types and corresponding predicate operations are supported: " + NEW_LINE +
            " * 'STRING' - used to filter any 'String' or 'JSON' values. Operations: EQUAL, NOT_EQUAL, STARTS_WITH, ENDS_WITH, CONTAINS, NOT_CONTAINS; \n" +
            " * 'NUMERIC' - used for 'Long' and 'Double' values. Operations: EQUAL, NOT_EQUAL, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL; \n" +
            " * 'BOOLEAN' - used for boolean values. Operations: EQUAL, NOT_EQUAL;\n" +
            " * 'DATE_TIME' - similar to numeric, transforms value to milliseconds since epoch. Operations: EQUAL, NOT_EQUAL, GREATER, LESS, GREATER_OR_EQUAL, LESS_OR_EQUAL; \n";

   protected static final String DEVICE_PROFILE_ALARM_SCHEDULE_SPECIFIC_TIME_EXAMPLE = MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "   \"schedule\":{\n" +
            "      \"type\":\"SPECIFIC_TIME\",\n" +
            "      \"endsOn\":64800000,\n" +
            "      \"startsOn\":43200000,\n" +
            "      \"timezone\":\"Europe/Kiev\",\n" +
            "      \"daysOfWeek\":[\n" +
            "         1,\n" +
            "         3,\n" +
            "         5\n" +
            "      ]\n" +
            "   }\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END;
   protected static final String DEVICE_PROFILE_ALARM_SCHEDULE_CUSTOM_EXAMPLE = MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "   \"schedule\":{\n" +
            "      \"type\":\"CUSTOM\",\n" +
            "      \"items\":[\n" +
            "         {\n" +
            "            \"endsOn\":0,\n" +
            "            \"enabled\":false,\n" +
            "            \"startsOn\":0,\n" +
            "            \"dayOfWeek\":1\n" +
            "         },\n" +
            "         {\n" +
            "            \"endsOn\":64800000,\n" +
            "            \"enabled\":true,\n" +
            "            \"startsOn\":43200000,\n" +
            "            \"dayOfWeek\":2\n" +
            "         },\n" +
            "         {\n" +
            "            \"endsOn\":0,\n" +
            "            \"enabled\":false,\n" +
            "            \"startsOn\":0,\n" +
            "            \"dayOfWeek\":3\n" +
            "         },\n" +
            "         {\n" +
            "            \"endsOn\":57600000,\n" +
            "            \"enabled\":true,\n" +
            "            \"startsOn\":36000000,\n" +
            "            \"dayOfWeek\":4\n" +
            "         },\n" +
            "         {\n" +
            "            \"endsOn\":0,\n" +
            "            \"enabled\":false,\n" +
            "            \"startsOn\":0,\n" +
            "            \"dayOfWeek\":5\n" +
            "         },\n" +
            "         {\n" +
            "            \"endsOn\":0,\n" +
            "            \"enabled\":false,\n" +
            "            \"startsOn\":0,\n" +
            "            \"dayOfWeek\":6\n" +
            "         },\n" +
            "         {\n" +
            "            \"endsOn\":0,\n" +
            "            \"enabled\":false,\n" +
            "            \"startsOn\":0,\n" +
            "            \"dayOfWeek\":7\n" +
            "         }\n" +
            "      ],\n" +
            "      \"timezone\":\"Europe/Kiev\"\n" +
            "   }\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END;
   protected static final String DEVICE_PROFILE_ALARM_SCHEDULE_ALWAYS_EXAMPLE = MARKDOWN_CODE_BLOCK_START + "\"schedule\": null" + MARKDOWN_CODE_BLOCK_END;

   protected static final String DEVICE_PROFILE_ALARM_CONDITION_REPEATING_EXAMPLE = MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "   \"spec\":{\n" +
            "      \"type\":\"REPEATING\",\n" +
            "      \"predicate\":{\n" +
            "         \"userValue\":null,\n" +
            "         \"defaultValue\":5,\n" +
            "         \"dynamicValue\":{\n" +
            "            \"inherit\":true,\n" +
            "            \"sourceType\":\"CURRENT_DEVICE\",\n" +
            "            \"sourceAttribute\":\"tempAttr\"\n" +
            "         }\n" +
            "      }\n" +
            "   }\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END;

   protected static final String DEVICE_PROFILE_ALARM_CONDITION_DURATION_EXAMPLE = MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "   \"spec\":{\n" +
            "      \"type\":\"DURATION\",\n" +
            "      \"unit\":\"MINUTES\",\n" +
            "      \"predicate\":{\n" +
            "         \"userValue\":null,\n" +
            "         \"defaultValue\":30,\n" +
            "         \"dynamicValue\":null\n" +
            "      }\n" +
            "   }\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END;

    protected static final String RELATION_TYPE_PARAM_DESCRIPTION = "A string value representing relation type between entities. For example, 'Contains', 'Manages'. It can be any string value.";
    protected static final String RELATION_TYPE_GROUP_PARAM_DESCRIPTION = "A string value representing relation type group. For example, 'COMMON'";

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    protected static final String DEFAULT_DASHBOARD = "defaultDashboardId";
    protected static final String HOME_DASHBOARD = "homeDashboardId";

    protected static final String SINGLE_ENTITY = "\n\n## Single Entity\n\n" +
            "Allows to filter only one entity based on the id. For example, this entity filter selects certain device:\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"singleEntity\",\n" +
            "  \"singleEntity\": {\n" +
            "    \"id\": \"d521edb0-2a7a-11ec-94eb-213c95f54092\",\n" +
            "    \"entityType\": \"DEVICE\"\n" +
            "  }\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    protected static final String ENTITY_LIST = "\n\n## Entity List Filter\n\n" +
            "Allows to filter entities of the same type using their ids. For example, this entity filter selects two devices:\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"entityList\",\n" +
            "  \"entityType\": \"DEVICE\",\n" +
            "  \"entityList\": [\n" +
            "    \"e6501f30-2a7a-11ec-94eb-213c95f54092\",\n" +
            "    \"e6657bf0-2a7a-11ec-94eb-213c95f54092\"\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    protected static final String ENTITY_NAME = "\n\n## Entity Name Filter\n\n" +
            "Allows to filter entities of the same type using the **'starts with'** expression over entity name. " +
            "For example, this entity filter selects all devices which name starts with 'Air Quality':\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"entityName\",\n" +
            "  \"entityType\": \"DEVICE\",\n" +
            "  \"entityNameFilter\": \"Air Quality\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    protected static final String ENTITY_TYPE_FILTER = "\n\n## Entity Type Filter\n\n" +
            "Allows to filter entities based on their type (CUSTOMER, USER, DASHBOARD, ASSET, DEVICE, etc)" +
            "For example, this entity filter selects all tenant customers:\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"entityType\",\n" +
            "  \"entityType\": \"CUSTOMER\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    protected static final String ASSET_TYPE = "\n\n## Asset Type Filter\n\n" +
            "Allows to filter assets based on their type and the **'starts with'** expression over their name. " +
            "For example, this entity filter selects all 'charging station' assets which name starts with 'Tesla':\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"assetType\",\n" +
            "  \"assetType\": \"charging station\",\n" +
            "  \"assetNameFilter\": \"Tesla\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    protected static final String DEVICE_TYPE = "\n\n## Device Type Filter\n\n" +
            "Allows to filter devices based on their type and the **'starts with'** expression over their name. " +
            "For example, this entity filter selects all 'Temperature Sensor' devices which name starts with 'ABC':\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"deviceType\",\n" +
            "  \"deviceType\": \"Temperature Sensor\",\n" +
            "  \"deviceNameFilter\": \"ABC\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    protected static final String EDGE_TYPE = "\n\n## Edge Type Filter\n\n" +
            "Allows to filter edge instances based on their type and the **'starts with'** expression over their name. " +
            "For example, this entity filter selects all 'Factory' edge instances which name starts with 'Nevada':\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"edgeType\",\n" +
            "  \"edgeType\": \"Factory\",\n" +
            "  \"edgeNameFilter\": \"Nevada\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    protected static final String ENTITY_VIEW_TYPE = "\n\n## Entity View Filter\n\n" +
            "Allows to filter entity views based on their type and the **'starts with'** expression over their name. " +
            "For example, this entity filter selects all 'Concrete Mixer' entity views which name starts with 'CAT':\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"entityViewType\",\n" +
            "  \"entityViewType\": \"Concrete Mixer\",\n" +
            "  \"entityViewNameFilter\": \"CAT\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    protected static final String API_USAGE = "\n\n## Api Usage Filter\n\n" +
            "Allows to query for Api Usage based on optional customer id. If the customer id is not set, returns current tenant API usage." +
            "For example, this entity filter selects the 'Api Usage' entity for customer with id 'e6501f30-2a7a-11ec-94eb-213c95f54092':\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"apiUsageState\",\n" +
            "  \"customerId\": {\n" +
            "    \"id\": \"d521edb0-2a7a-11ec-94eb-213c95f54092\",\n" +
            "    \"entityType\": \"CUSTOMER\"\n" +
            "  }\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    protected static final String MAX_LEVEL_DESCRIPTION = "Possible direction values are 'TO' and 'FROM'. The 'maxLevel' defines how many relation levels should the query search 'recursively'. ";
    protected static final String FETCH_LAST_LEVEL_ONLY_DESCRIPTION = "Assuming the 'maxLevel' is > 1, the 'fetchLastLevelOnly' defines either to return all related entities or only entities that are on the last level of relations. ";

    protected static final String RELATIONS_QUERY_FILTER = "\n\n## Relations Query Filter\n\n" +
            "Allows to filter entities that are related to the provided root entity. " +
            MAX_LEVEL_DESCRIPTION +
            FETCH_LAST_LEVEL_ONLY_DESCRIPTION +
            "The 'filter' object allows you to define the relation type and set of acceptable entity types to search for. " +
            "The relation query calculates all related entities, even if they are filtered using different relation types, and then extracts only those who match the 'filters'.\n\n" +
            "For example, this entity filter selects all devices and assets which are related to the asset with id 'e51de0c0-2a7a-11ec-94eb-213c95f54092':\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"relationsQuery\",\n" +
            "  \"rootEntity\": {\n" +
            "    \"entityType\": \"ASSET\",\n" +
            "    \"id\": \"e51de0c0-2a7a-11ec-94eb-213c95f54092\"\n" +
            "  },\n" +
            "  \"direction\": \"FROM\",\n" +
            "  \"maxLevel\": 1,\n" +
            "  \"fetchLastLevelOnly\": false,\n" +
            "  \"filters\": [\n" +
            "    {\n" +
            "      \"relationType\": \"Contains\",\n" +
            "      \"entityTypes\": [\n" +
            "        \"DEVICE\",\n" +
            "        \"ASSET\"\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";


    protected static final String ASSET_QUERY_FILTER = "\n\n## Asset Search Query\n\n" +
            "Allows to filter assets that are related to the provided root entity. Filters related assets based on the relation type and set of asset types. " +
            MAX_LEVEL_DESCRIPTION +
            FETCH_LAST_LEVEL_ONLY_DESCRIPTION +
            "The 'relationType' defines the type of the relation to search for. " +
            "The 'assetTypes' defines the type of the asset to search for. " +
            "The relation query calculates all related entities, even if they are filtered using different relation types, and then extracts only assets that match 'relationType' and 'assetTypes' conditions.\n\n" +
            "For example, this entity filter selects 'charging station' assets which are related to the asset with id 'e51de0c0-2a7a-11ec-94eb-213c95f54092' using 'Contains' relation:\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"assetSearchQuery\",\n" +
            "  \"rootEntity\": {\n" +
            "    \"entityType\": \"ASSET\",\n" +
            "    \"id\": \"e51de0c0-2a7a-11ec-94eb-213c95f54092\"\n" +
            "  },\n" +
            "  \"direction\": \"FROM\",\n" +
            "  \"maxLevel\": 1,\n" +
            "  \"fetchLastLevelOnly\": false,\n" +
            "  \"relationType\": \"Contains\",\n" +
            "  \"assetTypes\": [\n" +
            "    \"charging station\"\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    protected static final String DEVICE_QUERY_FILTER = "\n\n## Device Search Query\n\n" +
            "Allows to filter devices that are related to the provided root entity. Filters related devices based on the relation type and set of device types. " +
            MAX_LEVEL_DESCRIPTION +
            FETCH_LAST_LEVEL_ONLY_DESCRIPTION +
            "The 'relationType' defines the type of the relation to search for. " +
            "The 'deviceTypes' defines the type of the device to search for. " +
            "The relation query calculates all related entities, even if they are filtered using different relation types, and then extracts only devices that match 'relationType' and 'deviceTypes' conditions.\n\n" +
            "For example, this entity filter selects 'Charging port' and 'Air Quality Sensor' devices which are related to the asset with id 'e52b0020-2a7a-11ec-94eb-213c95f54092' using 'Contains' relation:\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"deviceSearchQuery\",\n" +
            "  \"rootEntity\": {\n" +
            "    \"entityType\": \"ASSET\",\n" +
            "    \"id\": \"e52b0020-2a7a-11ec-94eb-213c95f54092\"\n" +
            "  },\n" +
            "  \"direction\": \"FROM\",\n" +
            "  \"maxLevel\": 2,\n" +
            "  \"fetchLastLevelOnly\": true,\n" +
            "  \"relationType\": \"Contains\",\n" +
            "  \"deviceTypes\": [\n" +
            "    \"Air Quality Sensor\",\n" +
            "    \"Charging port\"\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    protected static final String EV_QUERY_FILTER = "\n\n## Entity View Query\n\n" +
            "Allows to filter entity views that are related to the provided root entity. Filters related entity views based on the relation type and set of entity view types. " +
            MAX_LEVEL_DESCRIPTION +
            FETCH_LAST_LEVEL_ONLY_DESCRIPTION +
            "The 'relationType' defines the type of the relation to search for. " +
            "The 'entityViewTypes' defines the type of the entity view to search for. " +
            "The relation query calculates all related entities, even if they are filtered using different relation types, and then extracts only devices that match 'relationType' and 'deviceTypes' conditions.\n\n" +
            "For example, this entity filter selects 'Concrete mixer' entity views which are related to the asset with id 'e52b0020-2a7a-11ec-94eb-213c95f54092' using 'Contains' relation:\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"entityViewSearchQuery\",\n" +
            "  \"rootEntity\": {\n" +
            "    \"entityType\": \"ASSET\",\n" +
            "    \"id\": \"e52b0020-2a7a-11ec-94eb-213c95f54092\"\n" +
            "  },\n" +
            "  \"direction\": \"FROM\",\n" +
            "  \"maxLevel\": 1,\n" +
            "  \"fetchLastLevelOnly\": false,\n" +
            "  \"relationType\": \"Contains\",\n" +
            "  \"entityViewTypes\": [\n" +
            "    \"Concrete mixer\"\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    protected static final String EDGE_QUERY_FILTER = "\n\n## Edge Search Query\n\n" +
            "Allows to filter edge instances that are related to the provided root entity. Filters related edge instances based on the relation type and set of edge types. " +
            MAX_LEVEL_DESCRIPTION +
            FETCH_LAST_LEVEL_ONLY_DESCRIPTION +
            "The 'relationType' defines the type of the relation to search for. " +
            "The 'deviceTypes' defines the type of the device to search for. " +
            "The relation query calculates all related entities, even if they are filtered using different relation types, and then extracts only devices that match 'relationType' and 'deviceTypes' conditions.\n\n" +
            "For example, this entity filter selects 'Factory' edge instances which are related to the asset with id 'e52b0020-2a7a-11ec-94eb-213c95f54092' using 'Contains' relation:\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"deviceSearchQuery\",\n" +
            "  \"rootEntity\": {\n" +
            "    \"entityType\": \"ASSET\",\n" +
            "    \"id\": \"e52b0020-2a7a-11ec-94eb-213c95f54092\"\n" +
            "  },\n" +
            "  \"direction\": \"FROM\",\n" +
            "  \"maxLevel\": 2,\n" +
            "  \"fetchLastLevelOnly\": true,\n" +
            "  \"relationType\": \"Contains\",\n" +
            "  \"edgeTypes\": [\n" +
            "    \"Factory\"\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    protected static final String EMPTY = "\n\n## Entity Type Filter\n\n" +
            "Allows to filter multiple entities of the same type using the **'starts with'** expression over entity name. " +
            "For example, this entity filter selects all devices which name starts with 'Air Quality':\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    protected static final String ENTITY_FILTERS =
            "\n\n # Entity Filters" +
                    "\nEntity Filter body depends on the 'type' parameter. Let's review available entity filter types. In fact, they do correspond to available dashboard aliases." +
                    SINGLE_ENTITY + ENTITY_LIST + ENTITY_NAME + ENTITY_TYPE_FILTER + ASSET_TYPE + DEVICE_TYPE + EDGE_TYPE + ENTITY_VIEW_TYPE + API_USAGE + RELATIONS_QUERY_FILTER
                    + ASSET_QUERY_FILTER + DEVICE_QUERY_FILTER + EV_QUERY_FILTER + EDGE_QUERY_FILTER;

    protected static final String FILTER_KEY = "\n\n## Filter Key\n\n" +
            "Filter Key defines either entity field, attribute or telemetry. It is a JSON object that consists the key name and type. " +
            "The following filter key types are supported: \n\n" +
            " * 'CLIENT_ATTRIBUTE' - used for client attributes; \n" +
            " * 'SHARED_ATTRIBUTE' - used for shared attributes; \n" +
            " * 'SERVER_ATTRIBUTE' - used for server attributes; \n" +
            " * 'ATTRIBUTE' - used for any of the above; \n" +
            " * 'TIME_SERIES' - used for time series values; \n" +
            " * 'ENTITY_FIELD' - used for accessing entity fields like 'name', 'label', etc. The list of available fields depends on the entity type; \n" +
            " * 'ALARM_FIELD' - similar to entity field, but is used in alarm queries only; \n" +
            "\n\n Let's review the example:\n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"TIME_SERIES\",\n" +
            "  \"key\": \"temperature\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    protected static final String FILTER_PREDICATE = "\n\n## Filter Predicate\n\n" +
            "Filter Predicate defines the logical expression to evaluate. The list of available operations depends on the filter value type, see above. " +
            "Platform supports 4 predicate types: 'STRING', 'NUMERIC', 'BOOLEAN' and 'COMPLEX'. The last one allows to combine multiple operations over one filter key." +
            "\n\nSimple predicate example to check 'value < 100': \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"operation\": \"LESS\",\n" +
            "  \"value\": {\n" +
            "    \"defaultValue\": 100,\n" +
            "    \"dynamicValue\": null\n" +
            "  },\n" +
            "  \"type\": \"NUMERIC\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\nComplex predicate example, to check 'value < 10 or value > 20': \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"COMPLEX\",\n" +
            "  \"operation\": \"OR\",\n" +
            "  \"predicates\": [\n" +
            "    {\n" +
            "      \"operation\": \"LESS\",\n" +
            "      \"value\": {\n" +
            "        \"defaultValue\": 10,\n" +
            "        \"dynamicValue\": null\n" +
            "      },\n" +
            "      \"type\": \"NUMERIC\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"operation\": \"GREATER\",\n" +
            "      \"value\": {\n" +
            "        \"defaultValue\": 20,\n" +
            "        \"dynamicValue\": null\n" +
            "      },\n" +
            "      \"type\": \"NUMERIC\"\n" +
            "    }\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\nMore complex predicate example, to check 'value < 10 or (value > 50 && value < 60)': \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"COMPLEX\",\n" +
            "  \"operation\": \"OR\",\n" +
            "  \"predicates\": [\n" +
            "    {\n" +
            "      \"operation\": \"LESS\",\n" +
            "      \"value\": {\n" +
            "        \"defaultValue\": 10,\n" +
            "        \"dynamicValue\": null\n" +
            "      },\n" +
            "      \"type\": \"NUMERIC\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"COMPLEX\",\n" +
            "      \"operation\": \"AND\",\n" +
            "      \"predicates\": [\n" +
            "        {\n" +
            "          \"operation\": \"GREATER\",\n" +
            "          \"value\": {\n" +
            "            \"defaultValue\": 50,\n" +
            "            \"dynamicValue\": null\n" +
            "          },\n" +
            "          \"type\": \"NUMERIC\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"operation\": \"LESS\",\n" +
            "          \"value\": {\n" +
            "            \"defaultValue\": 60,\n" +
            "            \"dynamicValue\": null\n" +
            "          },\n" +
            "          \"type\": \"NUMERIC\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\n You may also want to replace hardcoded values (for example, temperature > 20) with the more dynamic " +
            "expression (for example, temperature > 'value of the tenant attribute with key 'temperatureThreshold'). " +
            "It is possible to use 'dynamicValue' to define attribute of the tenant, customer or user that is performing the API call. " +
            "See example below: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"operation\": \"GREATER\",\n" +
            "  \"value\": {\n" +
            "    \"defaultValue\": 0,\n" +
            "    \"dynamicValue\": {\n" +
            "      \"sourceType\": \"CURRENT_USER\",\n" +
            "      \"sourceAttribute\": \"temperatureThreshold\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"type\": \"NUMERIC\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\n Note that you may use 'CURRENT_USER', 'CURRENT_CUSTOMER' and 'CURRENT_TENANT' as a 'sourceType'. The 'defaultValue' is used when the attribute with such a name is not defined for the chosen source.";

    protected static final String KEY_FILTERS =
            "\n\n # Key Filters" +
                    "\nKey Filter allows you to define complex logical expressions over entity field, attribute or latest time series value. The filter is defined using 'key', 'valueType' and 'predicate' objects. " +
                    "Single Entity Query may have zero, one or multiple predicates. If multiple filters are defined, they are evaluated using logical 'AND'. " +
                    "The example below checks that temperature of the entity is above 20 degrees:" +
                    "\n\n" + MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"key\": {\n" +
                    "    \"type\": \"TIME_SERIES\",\n" +
                    "    \"key\": \"temperature\"\n" +
                    "  },\n" +
                    "  \"valueType\": \"NUMERIC\",\n" +
                    "  \"predicate\": {\n" +
                    "    \"operation\": \"GREATER\",\n" +
                    "    \"value\": {\n" +
                    "      \"defaultValue\": 20,\n" +
                    "      \"dynamicValue\": null\n" +
                    "    },\n" +
                    "    \"type\": \"NUMERIC\"\n" +
                    "  }\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
                    "\n\n Now let's review 'key', 'valueType' and 'predicate' objects in detail."
                    + FILTER_KEY + FILTER_VALUE_TYPE + FILTER_PREDICATE;

    protected static final String ENTITY_COUNT_QUERY_DESCRIPTION =
            "Allows to run complex queries to search the count of platform entities (devices, assets, customers, etc) " +
                    "based on the combination of main entity filter and multiple key filters. Returns the number of entities that match the query definition.\n\n" +
                    "# Query Definition\n\n" +
                    "\n\nMain **entity filter** is mandatory and defines generic search criteria. " +
                    "For example, \"find all devices with profile 'Moisture Sensor'\" or \"Find all devices related to asset 'Building A'\"" +
                    "\n\nOptional **key filters** allow to filter results of the entity filter by complex criteria against " +
                    "main entity fields (name, label, type, etc), attributes and telemetry. " +
                    "For example, \"temperature > 20 or temperature< 10\" or \"name starts with 'T', and attribute 'model' is 'T1000', and time series field 'batteryLevel' > 40\"." +
                    "\n\nLet's review the example:" +
                    "\n\n" + MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"entityFilter\": {\n" +
                    "    \"type\": \"entityType\",\n" +
                    "    \"entityType\": \"DEVICE\"\n" +
                    "  },\n" +
                    "  \"keyFilters\": [\n" +
                    "    {\n" +
                    "      \"key\": {\n" +
                    "        \"type\": \"ATTRIBUTE\",\n" +
                    "        \"key\": \"active\"\n" +
                    "      },\n" +
                    "      \"valueType\": \"BOOLEAN\",\n" +
                    "      \"predicate\": {\n" +
                    "        \"operation\": \"EQUAL\",\n" +
                    "        \"value\": {\n" +
                    "          \"defaultValue\": true,\n" +
                    "          \"dynamicValue\": null\n" +
                    "        },\n" +
                    "        \"type\": \"BOOLEAN\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ]\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
                    "\n\n Example mentioned above search all devices which have attribute 'active' set to 'true'. Now let's review available entity filters and key filters syntax:" +
                    ENTITY_FILTERS +
                    KEY_FILTERS +
                    ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;

    protected static final String ENTITY_DATA_QUERY_DESCRIPTION =
            "Allows to run complex queries over platform entities (devices, assets, customers, etc) " +
                    "based on the combination of main entity filter and multiple key filters. " +
                    "Returns the paginated result of the query that contains requested entity fields and latest values of requested attributes and time series data.\n\n" +
                    "# Query Definition\n\n" +
                    "\n\nMain **entity filter** is mandatory and defines generic search criteria. " +
                    "For example, \"find all devices with profile 'Moisture Sensor'\" or \"Find all devices related to asset 'Building A'\"" +
                    "\n\nOptional **key filters** allow to filter results of the **entity filter** by complex criteria against " +
                    "main entity fields (name, label, type, etc), attributes and telemetry. " +
                    "For example, \"temperature > 20 or temperature< 10\" or \"name starts with 'T', and attribute 'model' is 'T1000', and time series field 'batteryLevel' > 40\"." +
                    "\n\nThe **entity fields** and **latest values** contains list of entity fields and latest attribute/telemetry fields to fetch for each entity." +
                    "\n\nThe **page link** contains information about the page to fetch and the sort ordering." +
                    "\n\nLet's review the example:" +
                    "\n\n" + MARKDOWN_CODE_BLOCK_START +
                    "{\n" +
                    "  \"entityFilter\": {\n" +
                    "    \"type\": \"entityType\",\n" +
                    "    \"resolveMultiple\": true,\n" +
                    "    \"entityType\": \"DEVICE\"\n" +
                    "  },\n" +
                    "  \"keyFilters\": [\n" +
                    "    {\n" +
                    "      \"key\": {\n" +
                    "        \"type\": \"TIME_SERIES\",\n" +
                    "        \"key\": \"temperature\"\n" +
                    "      },\n" +
                    "      \"valueType\": \"NUMERIC\",\n" +
                    "      \"predicate\": {\n" +
                    "        \"operation\": \"GREATER\",\n" +
                    "        \"value\": {\n" +
                    "          \"defaultValue\": 0,\n" +
                    "          \"dynamicValue\": {\n" +
                    "            \"sourceType\": \"CURRENT_USER\",\n" +
                    "            \"sourceAttribute\": \"temperatureThreshold\",\n" +
                    "            \"inherit\": false\n" +
                    "          }\n" +
                    "        },\n" +
                    "        \"type\": \"NUMERIC\"\n" +
                    "      }\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"entityFields\": [\n" +
                    "    {\n" +
                    "      \"type\": \"ENTITY_FIELD\",\n" +
                    "      \"key\": \"name\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"type\": \"ENTITY_FIELD\",\n" +
                    "      \"key\": \"label\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"type\": \"ENTITY_FIELD\",\n" +
                    "      \"key\": \"additionalInfo\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"latestValues\": [\n" +
                    "    {\n" +
                    "      \"type\": \"ATTRIBUTE\",\n" +
                    "      \"key\": \"model\"\n" +
                    "    },\n" +
                    "    {\n" +
                    "      \"type\": \"TIME_SERIES\",\n" +
                    "      \"key\": \"temperature\"\n" +
                    "    }\n" +
                    "  ],\n" +
                    "  \"pageLink\": {\n" +
                    "    \"page\": 0,\n" +
                    "    \"pageSize\": 10,\n" +
                    "    \"sortOrder\": {\n" +
                    "      \"key\": {\n" +
                    "        \"key\": \"name\",\n" +
                    "        \"type\": \"ENTITY_FIELD\"\n" +
                    "      },\n" +
                    "      \"direction\": \"ASC\"\n" +
                    "    }\n" +
                    "  }\n" +
                    "}" +
                    MARKDOWN_CODE_BLOCK_END +
                    "\n\n Example mentioned above search all devices which have attribute 'active' set to 'true'. Now let's review available entity filters and key filters syntax:" +
                    ENTITY_FILTERS +
                    KEY_FILTERS +
                    ControllerConstants.TENANT_OR_CUSTOMER_AUTHORITY_PARAGRAPH;


    protected static final String ALARM_DATA_QUERY_DESCRIPTION = "This method description defines how Alarm Data Query extends the Entity Data Query. " +
            "See method 'Find Entity Data by Query' first to get the info about 'Entity Data Query'." +
            "\n\n The platform will first search the entities that match the entity and key filters. Then, the platform will use 'Alarm Page Link' to filter the alarms related to those entities. " +
            "Finally, platform fetch the properties of alarm that are defined in the **'alarmFields'** and combine them with the other entity, attribute and latest time series fields to return the result. " +
            "\n\n See example of the alarm query below. The query will search first 100 active alarms with type 'Temperature Alarm' or 'Fire Alarm' for any device with current temperature > 0. " +
            "The query will return combination of the entity fields: name of the device, device model and latest temperature reading and alarms fields: createdTime, type, severity and status: " +
            "\n\n" + MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"entityFilter\": {\n" +
            "    \"type\": \"entityType\",\n" +
            "    \"resolveMultiple\": true,\n" +
            "    \"entityType\": \"DEVICE\"\n" +
            "  },\n" +
            "  \"pageLink\": {\n" +
            "    \"page\": 0,\n" +
            "    \"pageSize\": 100,\n" +
            "    \"textSearch\": null,\n" +
            "    \"searchPropagatedAlarms\": false,\n" +
            "    \"statusList\": [\n" +
            "      \"ACTIVE\"\n" +
            "    ],\n" +
            "    \"severityList\": [\n" +
            "      \"CRITICAL\",\n" +
            "      \"MAJOR\"\n" +
            "    ],\n" +
            "    \"typeList\": [\n" +
            "      \"Temperature Alarm\",\n" +
            "      \"Fire Alarm\"\n" +
            "    ],\n" +
            "    \"sortOrder\": {\n" +
            "      \"key\": {\n" +
            "        \"key\": \"createdTime\",\n" +
            "        \"type\": \"ALARM_FIELD\"\n" +
            "      },\n" +
            "      \"direction\": \"DESC\"\n" +
            "    },\n" +
            "    \"timeWindow\": 86400000\n" +
            "  },\n" +
            "  \"keyFilters\": [\n" +
            "    {\n" +
            "      \"key\": {\n" +
            "        \"type\": \"TIME_SERIES\",\n" +
            "        \"key\": \"temperature\"\n" +
            "      },\n" +
            "      \"valueType\": \"NUMERIC\",\n" +
            "      \"predicate\": {\n" +
            "        \"operation\": \"GREATER\",\n" +
            "        \"value\": {\n" +
            "          \"defaultValue\": 0,\n" +
            "          \"dynamicValue\": null\n" +
            "        },\n" +
            "        \"type\": \"NUMERIC\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"alarmFields\": [\n" +
            "    {\n" +
            "      \"type\": \"ALARM_FIELD\",\n" +
            "      \"key\": \"createdTime\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"ALARM_FIELD\",\n" +
            "      \"key\": \"type\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"ALARM_FIELD\",\n" +
            "      \"key\": \"severity\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"ALARM_FIELD\",\n" +
            "      \"key\": \"status\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"entityFields\": [\n" +
            "    {\n" +
            "      \"type\": \"ENTITY_FIELD\",\n" +
            "      \"key\": \"name\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"latestValues\": [\n" +
            "    {\n" +
            "      \"type\": \"ATTRIBUTE\",\n" +
            "      \"key\": \"model\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"TIME_SERIES\",\n" +
            "      \"key\": \"temperature\"\n" +
            "    }\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END +
            "";

    protected static final String COAP_TRANSPORT_CONFIGURATION_EXAMPLE = MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "   \"type\":\"COAP\",\n" +
            "   \"clientSettings\":{\n" +
            "      \"edrxCycle\":null,\n" +
            "      \"powerMode\":\"DRX\",\n" +
            "      \"psmActivityTimer\":null,\n" +
            "      \"pagingTransmissionWindow\":null\n" +
            "   },\n" +
            "   \"coapDeviceTypeConfiguration\":{\n" +
            "      \"coapDeviceType\":\"DEFAULT\",\n" +
            "      \"transportPayloadTypeConfiguration\":{\n" +
            "         \"transportPayloadType\":\"JSON\"\n" +
            "      }\n" +
            "   }\n" +
            "}"
            + MARKDOWN_CODE_BLOCK_END;

    protected static final String TRANSPORT_CONFIGURATION = "# Transport Configuration" + NEW_LINE +
            "5 transport configuration types are available:\n" +
            " * 'DEFAULT';\n" +
            " * 'MQTT';\n" +
            " * 'LWM2M';\n" +
            " * 'COAP';\n" +
            " * 'SNMP'." + NEW_LINE + "Default type supports basic MQTT, HTTP, CoAP and LwM2M transports. " +
            "Please refer to the [docs](https://thingsboard.io/docs/user-guide/device-profiles/#transport-configuration) for more details about other types.\n" +
            "\nSee another example of COAP transport configuration below:" + NEW_LINE + COAP_TRANSPORT_CONFIGURATION_EXAMPLE;

    protected static final String ALARM_FILTER_KEY = "## Alarm Filter Key" + NEW_LINE +
            "Filter Key defines either entity field, attribute, telemetry or constant. It is a JSON object that consists the key name and type. The following filter key types are supported:\n" +
            " * 'ATTRIBUTE' - used for attributes values;\n" +
            " * 'TIME_SERIES' - used for time series values;\n" +
            " * 'ENTITY_FIELD' - used for accessing entity fields like 'name', 'label', etc. The list of available fields depends on the entity type;\n" +
            " * 'CONSTANT' - constant value specified." + NEW_LINE + "Let's review the example:" + NEW_LINE +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"TIME_SERIES\",\n" +
            "  \"key\": \"temperature\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END;

    protected static final String DEVICE_PROFILE_FILTER_PREDICATE = NEW_LINE + "## Filter Predicate" + NEW_LINE +
            "Filter Predicate defines the logical expression to evaluate. The list of available operations depends on the filter value type, see above. " +
            "Platform supports 4 predicate types: 'STRING', 'NUMERIC', 'BOOLEAN' and 'COMPLEX'. The last one allows to combine multiple operations over one filter key." + NEW_LINE +
            "Simple predicate example to check 'value < 100': " + NEW_LINE +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"operation\": \"LESS\",\n" +
            "  \"value\": {\n" +
            "    \"userValue\": null,\n" +
            "    \"defaultValue\": 100,\n" +
            "    \"dynamicValue\": null\n" +
            "  },\n" +
            "  \"type\": \"NUMERIC\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END + NEW_LINE +
            "Complex predicate example, to check 'value < 10 or value > 20': " + NEW_LINE +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"COMPLEX\",\n" +
            "  \"operation\": \"OR\",\n" +
            "  \"predicates\": [\n" +
            "    {\n" +
            "      \"operation\": \"LESS\",\n" +
            "      \"value\": {\n" +
            "        \"userValue\": null,\n" +
            "        \"defaultValue\": 10,\n" +
            "        \"dynamicValue\": null\n" +
            "      },\n" +
            "      \"type\": \"NUMERIC\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"operation\": \"GREATER\",\n" +
            "      \"value\": {\n" +
            "        \"userValue\": null,\n" +
            "        \"defaultValue\": 20,\n" +
            "        \"dynamicValue\": null\n" +
            "      },\n" +
            "      \"type\": \"NUMERIC\"\n" +
            "    }\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END + NEW_LINE +
            "More complex predicate example, to check 'value < 10 or (value > 50 && value < 60)': " + NEW_LINE +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"type\": \"COMPLEX\",\n" +
            "  \"operation\": \"OR\",\n" +
            "  \"predicates\": [\n" +
            "    {\n" +
            "      \"operation\": \"LESS\",\n" +
            "      \"value\": {\n" +
            "        \"userValue\": null,\n" +
            "        \"defaultValue\": 10,\n" +
            "        \"dynamicValue\": null\n" +
            "      },\n" +
            "      \"type\": \"NUMERIC\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"type\": \"COMPLEX\",\n" +
            "      \"operation\": \"AND\",\n" +
            "      \"predicates\": [\n" +
            "        {\n" +
            "          \"operation\": \"GREATER\",\n" +
            "          \"value\": {\n" +
            "            \"userValue\": null,\n" +
            "            \"defaultValue\": 50,\n" +
            "            \"dynamicValue\": null\n" +
            "          },\n" +
            "          \"type\": \"NUMERIC\"\n" +
            "        },\n" +
            "        {\n" +
            "          \"operation\": \"LESS\",\n" +
            "          \"value\": {\n" +
            "            \"userValue\": null,\n" +
            "            \"defaultValue\": 60,\n" +
            "            \"dynamicValue\": null\n" +
            "          },\n" +
            "          \"type\": \"NUMERIC\"\n" +
            "        }\n" +
            "      ]\n" +
            "    }\n" +
            "  ]\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END + NEW_LINE +
            "You may also want to replace hardcoded values (for example, temperature > 20) with the more dynamic " +
            "expression (for example, temperature > value of the tenant attribute with key 'temperatureThreshold'). " +
            "It is possible to use 'dynamicValue' to define attribute of the tenant, customer or device. " +
            "See example below:" + NEW_LINE +
            MARKDOWN_CODE_BLOCK_START +
            "{\n" +
            "  \"operation\": \"GREATER\",\n" +
            "  \"value\": {\n" +
            "    \"userValue\": null,\n" +
            "    \"defaultValue\": 0,\n" +
            "    \"dynamicValue\": {\n" +
            "      \"inherit\": false,\n" +
            "      \"sourceType\": \"CURRENT_TENANT\",\n" +
            "      \"sourceAttribute\": \"temperatureThreshold\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"type\": \"NUMERIC\"\n" +
            "}" +
            MARKDOWN_CODE_BLOCK_END + NEW_LINE +
            "Note that you may use 'CURRENT_DEVICE', 'CURRENT_CUSTOMER' and 'CURRENT_TENANT' as a 'sourceType'. The 'defaultValue' is used when the attribute with such a name is not defined for the chosen source. " +
            "The 'sourceAttribute' can be inherited from the owner of the specified 'sourceType' if 'inherit' is set to true.";

    protected static final String KEY_FILTERS_DESCRIPTION = "# Key Filters" + NEW_LINE +
            "Key filter objects are created under the **'condition'** array. They allow you to define complex logical expressions over entity field, " +
            "attribute, latest time series value or constant. The filter is defined using 'key', 'valueType', " +
            "'value' (refers to the value of the 'CONSTANT' alarm filter key type) and 'predicate' objects. Let's review each object:" + NEW_LINE +
            ALARM_FILTER_KEY + FILTER_VALUE_TYPE + NEW_LINE + DEVICE_PROFILE_FILTER_PREDICATE + NEW_LINE;

    protected static final String DEFAULT_DEVICE_PROFILE_DATA_EXAMPLE = MARKDOWN_CODE_BLOCK_START + "{\n" +
            "   \"alarms\":[\n" +
            "   ],\n" +
            "   \"configuration\":{\n" +
            "      \"type\":\"DEFAULT\"\n" +
            "   },\n" +
            "   \"provisionConfiguration\":{\n" +
            "      \"type\":\"DISABLED\",\n" +
            "      \"provisionDeviceSecret\":null\n" +
            "   },\n" +
            "   \"transportConfiguration\":{\n" +
            "      \"type\":\"DEFAULT\"\n" +
            "   }\n" +
            "}" + MARKDOWN_CODE_BLOCK_END;

    protected static final String CUSTOM_DEVICE_PROFILE_DATA_EXAMPLE = MARKDOWN_CODE_BLOCK_START + "{\n" +
            "   \"alarms\":[\n" +
            "      {\n" +
            "         \"id\":\"2492b935-1226-59e9-8615-17d8978a4f93\",\n" +
            "         \"alarmType\":\"Temperature Alarm\",\n" +
            "         \"clearRule\":{\n" +
            "            \"schedule\":null,\n" +
            "            \"condition\":{\n" +
            "               \"spec\":{\n" +
            "                  \"type\":\"SIMPLE\"\n" +
            "               },\n" +
            "               \"condition\":[\n" +
            "                  {\n" +
            "                     \"key\":{\n" +
            "                        \"key\":\"temperature\",\n" +
            "                        \"type\":\"TIME_SERIES\"\n" +
            "                     },\n" +
            "                     \"value\":null,\n" +
            "                     \"predicate\":{\n" +
            "                        \"type\":\"NUMERIC\",\n" +
            "                        \"value\":{\n" +
            "                           \"userValue\":null,\n" +
            "                           \"defaultValue\":30.0,\n" +
            "                           \"dynamicValue\":null\n" +
            "                        },\n" +
            "                        \"operation\":\"LESS\"\n" +
            "                     },\n" +
            "                     \"valueType\":\"NUMERIC\"\n" +
            "                  }\n" +
            "               ]\n" +
            "            },\n" +
            "            \"dashboardId\":null,\n" +
            "            \"alarmDetails\":null\n" +
            "         },\n" +
            "         \"propagate\":false,\n" +
            "         \"createRules\":{\n" +
            "            \"MAJOR\":{\n" +
            "               \"schedule\":{\n" +
            "                  \"type\":\"SPECIFIC_TIME\",\n" +
            "                  \"endsOn\":64800000,\n" +
            "                  \"startsOn\":43200000,\n" +
            "                  \"timezone\":\"Europe/Kiev\",\n" +
            "                  \"daysOfWeek\":[\n" +
            "                     1,\n" +
            "                     3,\n" +
            "                     5\n" +
            "                  ]\n" +
            "               },\n" +
            "               \"condition\":{\n" +
            "                  \"spec\":{\n" +
            "                     \"type\":\"DURATION\",\n" +
            "                     \"unit\":\"MINUTES\",\n" +
            "                     \"predicate\":{\n" +
            "                        \"userValue\":null,\n" +
            "                        \"defaultValue\":30,\n" +
            "                        \"dynamicValue\":null\n" +
            "                     }\n" +
            "                  },\n" +
            "                  \"condition\":[\n" +
            "                     {\n" +
            "                        \"key\":{\n" +
            "                           \"key\":\"temperature\",\n" +
            "                           \"type\":\"TIME_SERIES\"\n" +
            "                        },\n" +
            "                        \"value\":null,\n" +
            "                        \"predicate\":{\n" +
            "                           \"type\":\"COMPLEX\",\n" +
            "                           \"operation\":\"OR\",\n" +
            "                           \"predicates\":[\n" +
            "                              {\n" +
            "                                 \"type\":\"NUMERIC\",\n" +
            "                                 \"value\":{\n" +
            "                                    \"userValue\":null,\n" +
            "                                    \"defaultValue\":50.0,\n" +
            "                                    \"dynamicValue\":null\n" +
            "                                 },\n" +
            "                                 \"operation\":\"LESS_OR_EQUAL\"\n" +
            "                              },\n" +
            "                              {\n" +
            "                                 \"type\":\"NUMERIC\",\n" +
            "                                 \"value\":{\n" +
            "                                    \"userValue\":null,\n" +
            "                                    \"defaultValue\":30.0,\n" +
            "                                    \"dynamicValue\":null\n" +
            "                                 },\n" +
            "                                 \"operation\":\"GREATER\"\n" +
            "                              }\n" +
            "                           ]\n" +
            "                        },\n" +
            "                        \"valueType\":\"NUMERIC\"\n" +
            "                     }\n" +
            "                  ]\n" +
            "               },\n" +
            "               \"dashboardId\":null,\n" +
            "               \"alarmDetails\":null\n" +
            "            },\n" +
            "            \"WARNING\":{\n" +
            "               \"schedule\":{\n" +
            "                  \"type\":\"CUSTOM\",\n" +
            "                  \"items\":[\n" +
            "                     {\n" +
            "                        \"endsOn\":0,\n" +
            "                        \"enabled\":false,\n" +
            "                        \"startsOn\":0,\n" +
            "                        \"dayOfWeek\":1\n" +
            "                     },\n" +
            "                     {\n" +
            "                        \"endsOn\":64800000,\n" +
            "                        \"enabled\":true,\n" +
            "                        \"startsOn\":43200000,\n" +
            "                        \"dayOfWeek\":2\n" +
            "                     },\n" +
            "                     {\n" +
            "                        \"endsOn\":0,\n" +
            "                        \"enabled\":false,\n" +
            "                        \"startsOn\":0,\n" +
            "                        \"dayOfWeek\":3\n" +
            "                     },\n" +
            "                     {\n" +
            "                        \"endsOn\":57600000,\n" +
            "                        \"enabled\":true,\n" +
            "                        \"startsOn\":36000000,\n" +
            "                        \"dayOfWeek\":4\n" +
            "                     },\n" +
            "                     {\n" +
            "                        \"endsOn\":0,\n" +
            "                        \"enabled\":false,\n" +
            "                        \"startsOn\":0,\n" +
            "                        \"dayOfWeek\":5\n" +
            "                     },\n" +
            "                     {\n" +
            "                        \"endsOn\":0,\n" +
            "                        \"enabled\":false,\n" +
            "                        \"startsOn\":0,\n" +
            "                        \"dayOfWeek\":6\n" +
            "                     },\n" +
            "                     {\n" +
            "                        \"endsOn\":0,\n" +
            "                        \"enabled\":false,\n" +
            "                        \"startsOn\":0,\n" +
            "                        \"dayOfWeek\":7\n" +
            "                     }\n" +
            "                  ],\n" +
            "                  \"timezone\":\"Europe/Kiev\"\n" +
            "               },\n" +
            "               \"condition\":{\n" +
            "                  \"spec\":{\n" +
            "                     \"type\":\"REPEATING\",\n" +
            "                     \"predicate\":{\n" +
            "                        \"userValue\":null,\n" +
            "                        \"defaultValue\":5,\n" +
            "                        \"dynamicValue\":null\n" +
            "                     }\n" +
            "                  },\n" +
            "                  \"condition\":[\n" +
            "                     {\n" +
            "                        \"key\":{\n" +
            "                           \"key\":\"tempConstant\",\n" +
            "                           \"type\":\"CONSTANT\"\n" +
            "                        },\n" +
            "                        \"value\":30,\n" +
            "                        \"predicate\":{\n" +
            "                           \"type\":\"NUMERIC\",\n" +
            "                           \"value\":{\n" +
            "                              \"userValue\":null,\n" +
            "                              \"defaultValue\":0.0,\n" +
            "                              \"dynamicValue\":{\n" +
            "                                 \"inherit\":false,\n" +
            "                                 \"sourceType\":\"CURRENT_DEVICE\",\n" +
            "                                 \"sourceAttribute\":\"tempThreshold\"\n" +
            "                              }\n" +
            "                           },\n" +
            "                           \"operation\":\"EQUAL\"\n" +
            "                        },\n" +
            "                        \"valueType\":\"NUMERIC\"\n" +
            "                     }\n" +
            "                  ]\n" +
            "               },\n" +
            "               \"dashboardId\":null,\n" +
            "               \"alarmDetails\":null\n" +
            "            },\n" +
            "            \"CRITICAL\":{\n" +
            "               \"schedule\":null,\n" +
            "               \"condition\":{\n" +
            "                  \"spec\":{\n" +
            "                     \"type\":\"SIMPLE\"\n" +
            "                  },\n" +
            "                  \"condition\":[\n" +
            "                     {\n" +
            "                        \"key\":{\n" +
            "                           \"key\":\"temperature\",\n" +
            "                           \"type\":\"TIME_SERIES\"\n" +
            "                        },\n" +
            "                        \"value\":null,\n" +
            "                        \"predicate\":{\n" +
            "                           \"type\":\"NUMERIC\",\n" +
            "                           \"value\":{\n" +
            "                              \"userValue\":null,\n" +
            "                              \"defaultValue\":50.0,\n" +
            "                              \"dynamicValue\":null\n" +
            "                           },\n" +
            "                           \"operation\":\"GREATER\"\n" +
            "                        },\n" +
            "                        \"valueType\":\"NUMERIC\"\n" +
            "                     }\n" +
            "                  ]\n" +
            "               },\n" +
            "               \"dashboardId\":null,\n" +
            "               \"alarmDetails\":null\n" +
            "            }\n" +
            "         },\n" +
            "         \"propagateRelationTypes\":null\n" +
            "      }\n" +
            "   ],\n" +
            "   \"configuration\":{\n" +
            "      \"type\":\"DEFAULT\"\n" +
            "   },\n" +
            "   \"provisionConfiguration\":{\n" +
            "      \"type\":\"ALLOW_CREATE_NEW_DEVICES\",\n" +
            "      \"provisionDeviceSecret\":\"vaxb9hzqdbz3oqukvomg\"\n" +
            "   },\n" +
            "   \"transportConfiguration\":{\n" +
            "      \"type\":\"MQTT\",\n" +
            "      \"deviceTelemetryTopic\":\"v1/devices/me/telemetry\",\n" +
            "      \"deviceAttributesTopic\":\"v1/devices/me/attributes\",\n" +
            "      \"transportPayloadTypeConfiguration\":{\n" +
            "         \"transportPayloadType\":\"PROTOBUF\",\n" +
            "         \"deviceTelemetryProtoSchema\":\"syntax =\\\"proto3\\\";\\npackage telemetry;\\n\\nmessage SensorDataReading {\\n\\n  optional double temperature = 1;\\n  optional double humidity = 2;\\n  InnerObject innerObject = 3;\\n\\n  message InnerObject {\\n    optional string key1 = 1;\\n    optional bool key2 = 2;\\n    optional double key3 = 3;\\n    optional int32 key4 = 4;\\n    optional string key5 = 5;\\n  }\\n}\",\n" +
            "         \"deviceAttributesProtoSchema\":\"syntax =\\\"proto3\\\";\\npackage attributes;\\n\\nmessage SensorConfiguration {\\n  optional string firmwareVersion = 1;\\n  optional string serialNumber = 2;\\n}\",\n" +
            "         \"deviceRpcRequestProtoSchema\":\"syntax =\\\"proto3\\\";\\npackage rpc;\\n\\nmessage RpcRequestMsg {\\n  optional string method = 1;\\n  optional int32 requestId = 2;\\n  optional string params = 3;\\n}\",\n" +
            "         \"deviceRpcResponseProtoSchema\":\"syntax =\\\"proto3\\\";\\npackage rpc;\\n\\nmessage RpcResponseMsg {\\n  optional string payload = 1;\\n}\"\n" +
            "      }\n" +
            "   }\n" +
            "}" + MARKDOWN_CODE_BLOCK_END;
    protected static final String DEVICE_PROFILE_DATA_DEFINITION = NEW_LINE + "# Device profile data definition" + NEW_LINE +
            "Device profile data object contains alarm rules configuration, device provision strategy and transport type configuration for device connectivity. Let's review some examples. " +
            "First one is the default device profile data configuration and second one - the custom one. " +
            NEW_LINE + DEFAULT_DEVICE_PROFILE_DATA_EXAMPLE + NEW_LINE + CUSTOM_DEVICE_PROFILE_DATA_EXAMPLE +
            NEW_LINE + "Let's review some specific objects examples related to the device profile configuration:";

    protected static final String ALARM_SCHEDULE = NEW_LINE + "# Alarm Schedule" + NEW_LINE +
            "Alarm Schedule JSON object represents the time interval during which the alarm rule is active. Note, " +
            NEW_LINE + DEVICE_PROFILE_ALARM_SCHEDULE_ALWAYS_EXAMPLE + NEW_LINE + "means alarm rule is active all the time. " +
            "**'daysOfWeek'** field represents Monday as 1, Tuesday as 2 and so on. **'startsOn'** and **'endsOn'** fields represent hours in millis (e.g. 64800000 = 18:00 or 6pm). " +
            "**'enabled'** flag specifies if item in a custom rule is active for specific day of the week:" + NEW_LINE +
            "## Specific Time Schedule" + NEW_LINE +
            DEVICE_PROFILE_ALARM_SCHEDULE_SPECIFIC_TIME_EXAMPLE + NEW_LINE +
            "## Custom Schedule" +
            NEW_LINE + DEVICE_PROFILE_ALARM_SCHEDULE_CUSTOM_EXAMPLE + NEW_LINE;

    protected static final String ALARM_CONDITION_TYPE = "# Alarm condition type (**'spec'**)" + NEW_LINE +
            "Alarm condition type can be either simple, duration, or repeating. For example, 5 times in a row or during 5 minutes." + NEW_LINE +
            "Note, **'userValue'** field is not used and reserved for future usage, **'dynamicValue'** is used for condition appliance by using the value of the **'sourceAttribute'** " +
            "or else **'defaultValue'** is used (if **'sourceAttribute'** is absent).\n" +
            "\n**'sourceType'** of the **'sourceAttribute'** can be: \n" +
            " * 'CURRENT_DEVICE';\n" +
            " * 'CURRENT_CUSTOMER';\n" +
            " * 'CURRENT_TENANT'." + NEW_LINE +
            "**'sourceAttribute'** can be inherited from the owner if **'inherit'** is set to true (for CURRENT_DEVICE and CURRENT_CUSTOMER)." + NEW_LINE +
            "## Repeating alarm condition" + NEW_LINE +
            DEVICE_PROFILE_ALARM_CONDITION_REPEATING_EXAMPLE + NEW_LINE +
            "## Duration alarm condition" + NEW_LINE +
            DEVICE_PROFILE_ALARM_CONDITION_DURATION_EXAMPLE + NEW_LINE +
            "**'unit'** can be: \n" +
            " * 'SECONDS';\n" +
            " * 'MINUTES';\n" +
            " * 'HOURS';\n" +
            " * 'DAYS'." + NEW_LINE;

    protected static final String PROVISION_CONFIGURATION = "# Provision Configuration" + NEW_LINE +
            "There are 3 types of device provision configuration for the device profile: \n" +
            " * 'DISABLED';\n" +
            " * 'ALLOW_CREATE_NEW_DEVICES';\n" +
            " * 'CHECK_PRE_PROVISIONED_DEVICES'." + NEW_LINE +
            "Please refer to the [docs](https://thingsboard.io/docs/user-guide/device-provisioning/) for more details." + NEW_LINE;

    protected static final String DEVICE_PROFILE_DATA = DEVICE_PROFILE_DATA_DEFINITION + ALARM_SCHEDULE + ALARM_CONDITION_TYPE +
            KEY_FILTERS_DESCRIPTION + PROVISION_CONFIGURATION + TRANSPORT_CONFIGURATION;

    protected static final String DEVICE_PROFILE_ID = "deviceProfileId";

    protected static final String ASSET_PROFILE_ID = "assetProfileId";

    protected static final String MODEL_DESCRIPTION = "See the 'Model' tab for more details.";
    protected static final String ENTITY_VIEW_DESCRIPTION = "Entity Views limit the degree of exposure of the Device or Asset telemetry and attributes to the Customers. " +
            "Every Entity View references exactly one entity (device or asset) and defines telemetry and attribute keys that will be visible to the assigned Customer. " +
            "As a Tenant Administrator you are able to create multiple EVs per Device or Asset and assign them to different Customers. ";
    protected static final String ENTITY_VIEW_INFO_DESCRIPTION = "Entity Views Info extends the Entity View with customer title and 'is public' flag. " + ENTITY_VIEW_DESCRIPTION;

    protected static final String ATTRIBUTES_SCOPE_DESCRIPTION = "A string value representing the attributes scope. For example, 'SERVER_SCOPE'.";
    protected static final String ATTRIBUTES_KEYS_DESCRIPTION = "A string value representing the comma-separated list of attributes keys. For example, 'active,inactivityAlarmTime'. " +
            "If attribute keys contain commas duplicate 'key' parameter for each key, for example '?key=my,key&key=my,second,key";
    protected static final String ATTRIBUTES_JSON_REQUEST_DESCRIPTION = "A string value representing the json object. For example, '{\"key\":\"value\"}'. See API call description for more details.";

    protected static final String TELEMETRY_KEYS_BASE_DESCRIPTION = "A string value representing the comma-separated list of telemetry keys.";
    protected static final String TELEMETRY_KEYS_DESCRIPTION = TELEMETRY_KEYS_BASE_DESCRIPTION + " If keys are not selected, the result will return all latest time series. For example, 'temperature,humidity'. " +
            "If telemetry keys contain comma, duplicate 'key' parameter for each key, for example '?key=my,key&key=my,second,key";
    protected static final String TELEMETRY_SCOPE_DESCRIPTION = "Value is deprecated, reserved for backward compatibility and not used in the API call implementation. Specify any scope for compatibility";
    protected static final String TELEMETRY_JSON_REQUEST_DESCRIPTION = "A JSON with the telemetry values. See API call description for more details.";


    protected static final String STRICT_DATA_TYPES_DESCRIPTION = "Enables/disables conversion of telemetry values to strings. Conversion is enabled by default. Set parameter to 'true' in order to disable the conversion.";
    protected static final String INVALID_ENTITY_ID_OR_ENTITY_TYPE_DESCRIPTION = "Referencing a non-existing entity Id or invalid entity type will cause an error. ";

    protected static final String SAVE_ATTIRIBUTES_STATUS_OK = "Attribute from the request was created or updated. ";
    protected static final String INVALID_STRUCTURE_OF_THE_REQUEST = "Invalid structure of the request";
    protected static final String SAVE_ATTIRIBUTES_STATUS_BAD_REQUEST = INVALID_STRUCTURE_OF_THE_REQUEST + " or invalid attributes scope provided.";
    protected static final String SAVE_ENTITY_ATTRIBUTES_STATUS_OK = "Platform creates an audit log event about entity attributes updates with action type 'ATTRIBUTES_UPDATED', " +
            "and also sends event msg to the rule engine with msg type 'ATTRIBUTES_UPDATED'.";
    protected static final String SAVE_ENTITY_ATTRIBUTES_STATUS_UNAUTHORIZED = "User is not authorized to save entity attributes for selected entity. Most likely, User belongs to different Customer or Tenant.";
    protected static final String SAVE_ENTITY_ATTRIBUTES_STATUS_INTERNAL_SERVER_ERROR = "The exception was thrown during processing the request. " +
            "Platform creates an audit log event about entity attributes updates with action type 'ATTRIBUTES_UPDATED' that includes an error stacktrace.";
    protected static final String SAVE_ENTITY_TIMESERIES_STATUS_OK = "Time series from the request was created or updated. " +
            "Platform creates an audit log event about entity time series updates with action type 'TIMESERIES_UPDATED'.";
    protected static final String SAVE_ENTITY_TIMESERIES_STATUS_UNAUTHORIZED = "User is not authorized to save entity time series for selected entity. Most likely, User belongs to different Customer or Tenant.";
    protected static final String SAVE_ENTITY_TIMESERIES_STATUS_INTERNAL_SERVER_ERROR = "The exception was thrown during processing the request. " +
            "Platform creates an audit log event about entity time series updates with action type 'TIMESERIES_UPDATED' that includes an error stacktrace.";

    protected static final String ENTITY_ATTRIBUTE_SCOPES_TEMPLATE = " List of possible attribute scopes depends on the entity type: " +
            "\n\n * SERVER_SCOPE - supported for all entity types;" +
            "\n * SHARED_SCOPE - supported for devices";
    protected static final String ENTITY_SAVE_ATTRIBUTE_SCOPES = ENTITY_ATTRIBUTE_SCOPES_TEMPLATE + ".\n\n";
    protected static final String ENTITY_GET_ATTRIBUTE_SCOPES = ENTITY_ATTRIBUTE_SCOPES_TEMPLATE +
            ";\n * CLIENT_SCOPE - supported for devices. " + "\n\n";

    protected static final String ATTRIBUTE_DATA_EXAMPLE = "[\n" +
            "  {\"key\": \"stringAttributeKey\", \"value\": \"value\", \"lastUpdateTs\": 1609459200000},\n" +
            "  {\"key\": \"booleanAttributeKey\", \"value\": false, \"lastUpdateTs\": 1609459200001},\n" +
            "  {\"key\": \"doubleAttributeKey\", \"value\": 42.2, \"lastUpdateTs\": 1609459200002},\n" +
            "  {\"key\": \"longKeyExample\", \"value\": 73, \"lastUpdateTs\": 1609459200003},\n" +
            "  {\"key\": \"jsonKeyExample\",\n" +
            "    \"value\": {\n" +
            "      \"someNumber\": 42,\n" +
            "      \"someArray\": [1,2,3],\n" +
            "      \"someNestedObject\": {\"key\": \"value\"}\n" +
            "    },\n" +
            "    \"lastUpdateTs\": 1609459200004\n" +
            "  }\n" +
            "]";

    protected static final String LATEST_TS_STRICT_DATA_EXAMPLE = "{\n" +
            "  \"stringTsKey\": [{ \"value\": \"value\", \"ts\": 1609459200000}],\n" +
            "  \"booleanTsKey\": [{ \"value\": false, \"ts\": 1609459200000}],\n" +
            "  \"doubleTsKey\": [{ \"value\": 42.2, \"ts\": 1609459200000}],\n" +
            "  \"longTsKey\": [{ \"value\": 73, \"ts\": 1609459200000}],\n" +
            "  \"jsonTsKey\": [{ \n" +
            "    \"value\": {\n" +
            "      \"someNumber\": 42,\n" +
            "      \"someArray\": [1,2,3],\n" +
            "      \"someNestedObject\": {\"key\": \"value\"}\n" +
            "    }, \n" +
            "    \"ts\": 1609459200000}]\n" +
            "}\n";

    protected static final String LATEST_TS_NON_STRICT_DATA_EXAMPLE = "{\n" +
            "  \"stringTsKey\": [{ \"value\": \"value\", \"ts\": 1609459200000}],\n" +
            "  \"booleanTsKey\": [{ \"value\": \"false\", \"ts\": 1609459200000}],\n" +
            "  \"doubleTsKey\": [{ \"value\": \"42.2\", \"ts\": 1609459200000}],\n" +
            "  \"longTsKey\": [{ \"value\": \"73\", \"ts\": 1609459200000}],\n" +
            "  \"jsonTsKey\": [{ \"value\": \"{\\\"someNumber\\\": 42,\\\"someArray\\\": [1,2,3],\\\"someNestedObject\\\": {\\\"key\\\": \\\"value\\\"}}\", \"ts\": 1609459200000}]\n" +
            "}\n";

    protected static final String TS_STRICT_DATA_EXAMPLE = "{\n" +
            "  \"temperature\": [\n" +
            "    {\n" +
            "      \"value\": 36.7,\n" +
            "      \"ts\": 1609459200000\n" +
            "    },\n" +
            "    {\n" +
            "      \"value\": 36.6,\n" +
            "      \"ts\": 1609459201000\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    protected static final String SAVE_ATTRIBUTES_REQUEST_PAYLOAD = "The request payload is a JSON object with key-value format of attributes to create or update. " +
            "For example:\n\n"
            + MARKDOWN_CODE_BLOCK_START
            + "{\n" +
            " \"stringKey\":\"value1\", \n" +
            " \"booleanKey\":true, \n" +
            " \"doubleKey\":42.0, \n" +
            " \"longKey\":73, \n" +
            " \"jsonKey\": {\n" +
            "    \"someNumber\": 42,\n" +
            "    \"someArray\": [1,2,3],\n" +
            "    \"someNestedObject\": {\"key\": \"value\"}\n" +
            " }\n" +
            "}"
            + MARKDOWN_CODE_BLOCK_END + "\n";

    protected static final String SAVE_TIMESERIES_REQUEST_PAYLOAD = "The request payload is a JSON document with three possible formats:\n\n" +
            "Simple format without timestamp. In such a case, current server time will be used: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\"temperature\": 26}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\n Single JSON object with timestamp: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "{\"ts\":1634712287000,\"values\":{\"temperature\":26, \"humidity\":87}}" +
            MARKDOWN_CODE_BLOCK_END +
            "\n\n JSON array with timestamps: \n\n" +
            MARKDOWN_CODE_BLOCK_START +
            "[{\"ts\":1634712287000,\"values\":{\"temperature\":26, \"humidity\":87}}, {\"ts\":1634712588000,\"values\":{\"temperature\":25, \"humidity\":88}}]" +
            MARKDOWN_CODE_BLOCK_END ;

    protected static final String SECURITY_WRITE_CHECK = " Security check is performed to verify that the user has 'WRITE' permission for the entity (entities).";

    public static final String NAME_CONFLICT_POLICY_DESC = "Optional value of name conflict policy. Possible values: FAIL or UNIQUIFY. " +
            " If omitted, FAIL policy is applied. FAIL policy implies exception will be thrown if an entity with the same name already exists. " +
            " UNIQUIFY policy appends a suffix to the entity name, if a name conflict occurs.";

    public static final String UNIQUIFY_SEPARATOR_DESC = "Optional value of name suffix separator used by UNIQUIFY policy. By default, underscore separator is used. " +
            "For example, strategy is UNIQUIFY, separator is '-'; if a name conflict occurs for entity name 'test-name', " +
            "created entity will have name like 'test-name-7fsh4f'.";

    public static final String UNIQUIFY_STRATEGY_DESC = "Optional value of uniquify strategy used by UNIQUIFY policy. Possible values: RANDOM or INCREMENTAL. " +
            "By default, RANDOM strategy is used, which means random alphanumeric string will be added as a suffix to entity name. " +
            "INCREMENTAL implies the first possible number starting from 1 will be added as a name suffix. " +
            "For example, strategy is UNIQUIFY, uniquify strategy is INCREMENTAL; if a name conflict occurs for entity name 'test-name', " +
            "created entity will have name like 'test-name-1.";
}
