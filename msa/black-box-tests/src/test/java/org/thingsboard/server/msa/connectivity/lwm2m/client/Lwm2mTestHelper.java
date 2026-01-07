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
package org.thingsboard.server.msa.connectivity.lwm2m.client;

import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.leshan.client.object.Security;

import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CONNECTION_ID_LENGTH;
import static org.eclipse.californium.scandium.config.DtlsConfig.DTLS_CONNECTION_ID_NODE_ID;
import static org.eclipse.leshan.client.object.Security.noSec;

public class Lwm2mTestHelper {

    // Models
    public static final String[] resources = new String[]{"0.xml", "1.xml", "2.xml", "3.xml", "5.xml", "19.xml"};
    public static final int serverId = 1;

    public static final int port = 5685;
    public static final int securityPort = 5686;
    public static final Integer shortServerId = 123;

    public static final String host = "localhost";
    public static final String COAP = "coap://";
    public static final String COAPS = "coaps://";
    public static final String URI = COAP + host + ":" + port;
    public static final String SECURE_URI = COAPS + host + ":" + securityPort;
    public static final String CLIENT_ENDPOINT_NO_SEC = "LwNoSec00000000";
    public static final Security SECURITY_NO_SEC = noSec(URI, shortServerId);

    public static final String CLIENT_ENDPOINT_PSK = "LwPsk00000000";
    public static final String CLIENT_PSK_IDENTITY = "SOME_PSK_ID";
    public static final String CLIENT_PSK_KEY = "73656372657450534b73656372657450";

    public static final int BINARY_APP_DATA_CONTAINER = 19;

    public static final String RESOURCE_ID_NAME_3_9 = "batteryLevel";
    public static final String RESOURCE_ID_NAME_3_14 = "UtfOffset";
    public static final String RESOURCE_ID_NAME_19_0_0 = "dataRead";
    public static final String RESOURCE_ID_NAME_19_0_2 = "dataCreationTime";
    public static final String RESOURCE_ID_NAME_19_1_0 = "dataWrite";

    public static String OBSERVE_ATTRIBUTES_WITH_PARAMS =

            "    {\n" +
                    "    \"keyName\": {\n" +
                    "      \"/3_1.2/0/9\": \"batteryLevel\",\n" +
                    "      \"/3_1.2/0/14\": \"UtfOffset\",\n" +
                    "      \"/19_1.1/0/0\": \"dataRead\",\n" +
                    "      \"/19_1.1/1/0\": \"dataWrite\",\n" +
                    "      \"/19_1.1/0/2\": \"dataCreationTime\"\n" +
                    "    },\n" +
                    "    \"observe\": [\n" +
                    "      \"/3_1.2/0/9\",\n" +
                    "      \"/19_1.1/0/0\",\n" +
                    "      \"/19_1.1/1/0\",\n" +
                    "      \"/19_1.1/0/2\"\n" +
                    "    ],\n" +
                    "    \"attribute\": [\n" +
                    "      \"/3_1.2/0/14\",\n" +
                    "      \"/19_1.1/0/2\"\n" +
                    "    ],\n" +
                    "    \"telemetry\": [\n" +
                    "      \"/3_1.2/0/9\",\n" +
                    "      \"/19_1.1/0/0\",\n" +
                    "      \"/19_1.1/1/0\"\n" +
                    "    ],\n" +
                    "    \"attributeLwm2m\": {}\n" +
                    "  }";
    public static final String CLIENT_LWM2M_SETTINGS =
            "     {\n" +
                    "    \"edrxCycle\": null,\n" +
                    "    \"powerMode\": \"DRX\",\n" +
                    "    \"fwUpdateResource\": null,\n" +
                    "    \"fwUpdateStrategy\": 1,\n" +
                    "    \"psmActivityTimer\": null,\n" +
                    "    \"swUpdateResource\": null,\n" +
                    "    \"swUpdateStrategy\": 1,\n" +
                    "    \"pagingTransmissionWindow\": null,\n" +
                    "    \"clientOnlyObserveAfterConnect\": 1\n" +
                    "  }";

    public enum LwM2MClientState {

        ON_INIT(0, "onInit"),
        ON_BOOTSTRAP_STARTED(1, "onBootstrapStarted"),
        ON_BOOTSTRAP_SUCCESS(2, "onBootstrapSuccess"),
        ON_BOOTSTRAP_FAILURE(3, "onBootstrapFailure"),
        ON_BOOTSTRAP_TIMEOUT(4, "onBootstrapTimeout"),
        ON_REGISTRATION_STARTED(5, "onRegistrationStarted"),
        ON_REGISTRATION_SUCCESS(6, "onRegistrationSuccess"),
        ON_REGISTRATION_FAILURE(7, "onRegistrationFailure"),
        ON_REGISTRATION_TIMEOUT(7, "onRegistrationTimeout"),
        ON_UPDATE_STARTED(8, "onUpdateStarted"),
        ON_UPDATE_SUCCESS(9, "onUpdateSuccess"),
        ON_UPDATE_FAILURE(10, "onUpdateFailure"),
        ON_UPDATE_TIMEOUT(11, "onUpdateTimeout"),
        ON_DEREGISTRATION_STARTED(12, "onDeregistrationStarted"),
        ON_DEREGISTRATION_SUCCESS(13, "onDeregistrationSuccess"),
        ON_DEREGISTRATION_FAILURE(14, "onDeregistrationFailure"),
        ON_DEREGISTRATION_TIMEOUT(15, "onDeregistrationTimeout"),
        ON_EXPECTED_ERROR(16, "onUnexpectedError"),
        ON_READ_CONNECTION_ID(17, "onReadConnection"),
        ON_WRITE_CONNECTION_ID(18, "onWriteConnection");

        public int code;
        public String type;

        LwM2MClientState(int code, String type) {
            this.code = code;
            this.type = type;
        }

        public static LwM2MClientState fromLwM2MClientStateByType(String type) {
            for (LwM2MClientState to : LwM2MClientState.values()) {
                if (to.type.equals(type)) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Client State type  : %s", type));
        }

        public static LwM2MClientState fromLwM2MClientStateByCode(int code) {
            for (LwM2MClientState to : LwM2MClientState.values()) {
                if (to.code == code) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Client State code : %s", code));
        }
    }

    public static void setDtlsConnectorConfigCidLength(Configuration serverCoapConfig, Integer cIdLength) {
        serverCoapConfig.setTransient(DTLS_CONNECTION_ID_LENGTH);
        serverCoapConfig.setTransient(DTLS_CONNECTION_ID_NODE_ID);
        serverCoapConfig.set(DTLS_CONNECTION_ID_LENGTH, cIdLength);
        if (cIdLength > 4) {
            serverCoapConfig.set(DTLS_CONNECTION_ID_NODE_ID, 0);
        } else {
            serverCoapConfig.set(DTLS_CONNECTION_ID_NODE_ID, null);
        }
    }
}