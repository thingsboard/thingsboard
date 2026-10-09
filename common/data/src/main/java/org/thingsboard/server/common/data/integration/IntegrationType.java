// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.integration;

import lombok.Getter;

@Getter
public enum IntegrationType {
    OCEANCONNECT(false, null),
    SIGFOX(false),
    THINGPARK(false),
    TPE(false),
    CHIRPSTACK(false),
    PARTICLE(false),
    TMOBILE_IOT_CDP(false, null),
    HTTP(false),
    MQTT(true),
    PUB_SUB(true),
    AWS_IOT(true),
    AWS_SQS(true),
    AWS_KINESIS(false),
    TTN(true),
    TTI(true),
    AZURE_EVENT_HUB(true),
    OPC_UA(true),
    CUSTOM(false, true, null),
    UDP(false, true),
    TCP(false, true),
    KAFKA(true),
    AZURE_IOT_HUB(true),
    APACHE_PULSAR(true),
    RABBITMQ(false),
    LORIOT(false),
    COAP(false),
    TUYA(true),
    AZURE_SERVICE_BUS(true),
    KPN(false);

    //Identifies if the Integration instance is one per cluster.
    private final boolean singleton;
    private final boolean remoteOnly;
    private final String directory;

    IntegrationType(boolean singleton) {
        this(singleton, false);
    }

    IntegrationType(boolean singleton, boolean remoteOnly) {
       this.singleton = singleton;
       this.remoteOnly = remoteOnly;
       this.directory = this.name();
    }

    IntegrationType(boolean singleton, String directory) {
        this(singleton, false, directory);
    }

    IntegrationType(boolean singleton, boolean remoteOnly, String directory) {
        this.singleton = singleton;
        this.remoteOnly = remoteOnly;
        this.directory = directory;
    }

}
