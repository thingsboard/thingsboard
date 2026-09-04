// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
