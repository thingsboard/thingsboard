/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.common.data.sync.ie.importing.csv;

import lombok.Getter;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MSecurityMode;

@Getter
public enum BulkImportColumnType {
    NAME,
    TYPE,
    LABEL,
    SHARED_ATTRIBUTE(AttributeScope.SHARED_SCOPE.name(), true),
    SERVER_ATTRIBUTE(AttributeScope.SERVER_SCOPE.name(), true),
    TIMESERIES(true),
    ACCESS_TOKEN,
    X509,
    MQTT_CLIENT_ID,
    MQTT_USER_NAME,
    MQTT_PASSWORD,
    LWM2M_CLIENT_ENDPOINT("endpoint"),
    LWM2M_CLIENT_SECURITY_CONFIG_MODE("securityConfigClientMode", LwM2MSecurityMode.NO_SEC.name()),
    LWM2M_CLIENT_IDENTITY("identity"),
    LWM2M_CLIENT_KEY("key"),
    LWM2M_CLIENT_CERT("cert"),
    LWM2M_BOOTSTRAP_SERVER_SECURITY_MODE("securityMode", LwM2MSecurityMode.NO_SEC.name()),
    LWM2M_BOOTSTRAP_SERVER_PUBLIC_KEY_OR_ID("clientPublicKeyOrId"),
    LWM2M_BOOTSTRAP_SERVER_SECRET_KEY("clientSecretKey"),
    LWM2M_SERVER_SECURITY_MODE("securityMode", LwM2MSecurityMode.NO_SEC.name()),
    LWM2M_SERVER_CLIENT_PUBLIC_KEY_OR_ID("clientPublicKeyOrId"),
    LWM2M_SERVER_CLIENT_SECRET_KEY("clientSecretKey"),
    SNMP_HOST,
    SNMP_PORT,
    SNMP_VERSION,
    SNMP_COMMUNITY_STRING,
    IS_GATEWAY,
    DESCRIPTION,
    ROUTING_KEY,
    SECRET;

    private String key;
    private String defaultValue;
    private boolean isKv = false;

    BulkImportColumnType() {
    }

    BulkImportColumnType(String key) {
        this.key = key;
    }

    BulkImportColumnType(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    BulkImportColumnType(boolean isKv) {
        this.isKv = isKv;
    }

    BulkImportColumnType(String key, boolean isKv) {
        this.key = key;
        this.isKv = isKv;
    }
}
