/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

/**
 * @author Andrew Shvayka
 */
public enum EntityType {

    TENANT("Tenant"),
    CUSTOMER("Customer"),
    USER("User"),
    DASHBOARD("Dashboard"),
    ASSET("Asset"),
    DEVICE("Device"),
    ALARM("Alarm"),
    RULE_CHAIN("Rule chain"),
    RULE_NODE("Rule node"),
    ENTITY_VIEW("Entity view"),
    WIDGETS_BUNDLE("Widget bundle"),
    WIDGET_TYPE("Widget type"),
    TENANT_PROFILE("Tenant profile"),
    DEVICE_PROFILE("Device profile"),
    ASSET_PROFILE("Asset profile"),
    API_USAGE_STATE("Api usage state"),
    TB_RESOURCE("TB resource"),
    OTA_PACKAGE("OTA package"),
    EDGE("Edge"),
    RPC("Rpc"),
    QUEUE("Queue");

    private final String displayName;

    EntityType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
