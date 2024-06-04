/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

public enum ObjectType {
    TENANT,
    CUSTOMER,
    ADMIN_SETTINGS,
    QUEUE,
    RPC,
    RULE_CHAIN,
    OTA_PACKAGE,
    RESOURCE,
    RULE_NODE,
    USER,
    USER_CREDENTIALS,
    USER_AUTH_SETTINGS,
    EDGE,
    EDGE_EVENT,
    WIDGETS_BUNDLE,
    WIDGET_TYPE,
    WIDGETS_BUNDLE_WIDGET,
    DASHBOARD,
    DEVICE_PROFILE,
    DEVICE,
    DEVICE_CREDENTIALS,
    ASSET_PROFILE,
    ASSET,
    ENTITY_VIEW,
    ALARM,
    ENTITY_ALARM,
    OAUTH2_PARAMS,
    OAUTH2_DOMAIN,
    OAUTH2_MOBILE,
    OAUTH2_REGISTRATION,
    RULE_NODE_STATE,
    USER_SETTINGS,
    NOTIFICATION_TARGET,
    NOTIFICATION_TEMPLATE,
    NOTIFICATION_RULE,
    ALARM_COMMENT,
    ALARM_TYPE,
    EVENT,
    AUDIT_LOG,
    RELATION,
    ATTRIBUTE_KV,
    LATEST_TS_KV,
    TS_KV;

}
