// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tuya.mq;

public enum TuyaRegion {

    CN("pulsar+ssl://mqe.tuyacn.com:7285/", "https://openapi.tuyacn.com"),
    US("pulsar+ssl://mqe.tuyaus.com:7285/", "https://openapi.tuyaus.com"),
    EU("pulsar+ssl://mqe.tuyaeu.com:7285/", "https://openapi.tuyaeu.com"),
    IN("pulsar+ssl://mqe.tuyain.com:7285/", "https://openapi.tuyain.com");

    private final String messagingServerUrl;
    private final String apiServerUrl;

    TuyaRegion(String messagingServerUrl, String apiServerUrl) {
        this.messagingServerUrl = messagingServerUrl;
        this.apiServerUrl = apiServerUrl;
    }

    public String getMessagingServerUrl() {
        return messagingServerUrl;
    }

    public String getApiServerUrl() {
        return apiServerUrl;
    }
}
