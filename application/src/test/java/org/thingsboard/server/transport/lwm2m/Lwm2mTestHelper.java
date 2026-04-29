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
package org.thingsboard.server.transport.lwm2m;

public class Lwm2mTestHelper {

    public static final String[] lwm2mClientResources = new String[]{"3-1_2.xml", "5.xml", "6.xml", "9.xml", "19.xml", "3303.xml"};

    // Models
    public static final int BINARY_APP_DATA_CONTAINER = 19;
    public static final int TEMPERATURE_SENSOR = 3303;

    // Ids in Client
    public static final int OBJECT_INSTANCE_ID_0 = 0;
    public static final int OBJECT_INSTANCE_ID_1 = 1;
    public static final int OBJECT_INSTANCE_ID_2 = 2;
    public static final int OBJECT_INSTANCE_ID_12 = 12;
    public static final int RESOURCE_ID_0 = 0;
    public static final int RESOURCE_ID_1 = 1;
    public static final int RESOURCE_ID_2 = 2;
    public static final int RESOURCE_ID_3 = 3;
    public static final int RESOURCE_ID_4 = 4;
    public static final int RESOURCE_ID_5 = 5;
    public static final int RESOURCE_ID_6 = 6;
    public static final int RESOURCE_ID_7 = 7;
    public static final int RESOURCE_ID_8 = 8;
    public static final int RESOURCE_ID_9 = 9;
    public static final int RESOURCE_ID_11 = 11;
    public static final int RESOURCE_ID_14 = 14;
    public static final int RESOURCE_ID_15 = 15;
    public static final int RESOURCE_ID_5700 = 5700;
    public static final int RESOURCE_INSTANCE_ID_0 = 0;
    public static final int RESOURCE_INSTANCE_ID_2 = 2;

    public static final String RESOURCE_ID_NAME_3_9 = "batteryLevel";
    public static final String RESOURCE_ID_NAME_3_14 = "UtfOffset";
    public static final String RESOURCE_ID_NAME_19_0_0 = "dataRead";
    public static final String RESOURCE_ID_NAME_19_0_2 = "dataCreationTime";
    public static final String RESOURCE_ID_NAME_19_1_0 = "dataWrite";
    public static final String RESOURCE_ID_NAME_19_0_3 = "dataDescription";
    public static final String RESOURCE_ID_NAME_3303_12_5700 = "sensorValue";
    public static final double RESOURCE_ID_3303_12_5700_VALUE_0 = 25.05d;
    public static final double RESOURCE_ID_3303_12_5700_VALUE_1 = 35.12d;
    public static long RESOURCE_ID_3303_12_5700_TS_0 = 0;
    public static long RESOURCE_ID_3303_12_5700_TS_1 = 0;
    public static final int RESOURCE_ID_VALUE_3303_12_5700_DELTA_TS = 3000;

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

    public enum LwM2MProfileBootstrapConfigType {

        LWM2M_ONLY(1, "only Lwm2m Server"),
        BOOTSTRAP_ONLY(2, "only Bootstrap Server"),
        BOTH(3, "Lwm2m Server and Bootstrap Server"),
        NONE(4, "Without Lwm2m Server and Bootstrap Server");

        public int code;
        public String type;

        LwM2MProfileBootstrapConfigType(int code, String type) {
            this.code = code;
            this.type = type;
        }

        public static LwM2MProfileBootstrapConfigType fromLwM2MBootstrapConfigByType(String type) {
            for (LwM2MProfileBootstrapConfigType to : LwM2MProfileBootstrapConfigType.values()) {
                if (to.type.equals(type)) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Profile Bootstrap Config type  : %s", type));
        }

        public static LwM2MProfileBootstrapConfigType fromLwM2MBootstrapConfigByCode(int code) {
            for (LwM2MProfileBootstrapConfigType to : LwM2MProfileBootstrapConfigType.values()) {
                if (to.code == code) {
                    return to;
                }
            }
            throw new IllegalArgumentException(String.format("Unsupported Profile Bootstrap Config code : %s", code));
        }
    }
}
