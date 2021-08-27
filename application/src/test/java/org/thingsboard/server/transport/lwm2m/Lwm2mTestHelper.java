/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import org.eclipse.californium.core.network.config.NetworkConfig;
import org.eclipse.leshan.client.object.Security;

import static org.eclipse.leshan.client.object.Security.noSec;

public class Lwm2mTestHelper {

    // Server
    public static final int SECURE_PORT = 5686;
    public static final int SECURE_PORT_BS = 5688;
    public static final String HOST = "localhost";
    public static final String HOST_BS = "localhost";
    public static final NetworkConfig SECURE_COAP_CONFIG = new NetworkConfig().setString("COAP_SECURE_PORT", Integer.toString(SECURE_PORT));
    public static final String ENDPOINT_SECURITY = "deviceAEndpoint";
    public static final String SECURE_URI = "coaps://localhost:" + SECURE_PORT;

    public static final int PORT = 5685;
    public static final int PORT_BS = 5687;
    public static final int SHORT_SERVER_ID = 123;
    public static final int SHORT_SERVER_ID_BS = 111;

    public static final Security SECURITY = noSec("coap://localhost:" + PORT, SHORT_SERVER_ID);
    public static final NetworkConfig COAP_CONFIG = new NetworkConfig().setString("COAP_PORT", Integer.toString(PORT));

    // Models
    public static final String[] resources = new String[]{"0.xml", "1.xml", "2.xml", "3.xml", "5.xml", "6.xml", "9.xml", "19.xml", "3303.xml"};
    public static final int BINARY_APP_DATA_CONTAINER = 19;
    public static final int TEMPERATURE_SENSOR = 3303;

    // Ids in Client
    public static final int objectInstanceId_0 = 0;
    public static final int objectInstanceId_1 = 1;
    public static final int objectInstanceId_12 = 12;
    public static final int resourceId_0 = 0;
    protected static final int resourceId_1 = 1;
    protected static final int resourceId_2 = 2;
    protected static final int resourceId_3 = 3;
    public static final int resourceId_9 = 9;
    protected static final int resourceId_11 = 11;
    public static final int resourceId_14 = 14;
    protected static final int resourceId_15= 15;

    public static final String resourceIdName_3_9 = "batteryLevel";
    public static final String resourceIdName_3_14 = "UtfOffset";
    public static final String resourceIdName_19_0_0 = "dataRead";
    public static final String resourceIdName_19_1_0 = "dataWrite";

}
