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
package org.thingsboard.server.transport.lwm2m.transportConfiguration;

import org.junit.Test;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MDeviceCredentials;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.COMPOSITE_ALL;
import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.COMPOSITE_BY_OBJECT;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

public class ObserveStrategyWithNoSecQueueModeConnectTest extends AbstractSecurityLwM2MIntegrationTest {

    @Test
    public void testWithNoSecQueueModeConnectLwm2mSuccessAndObserveSingleTelemetryUpdateProfileAfterConnected() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC + "_ObserveSingle";
        LwM2MDeviceCredentials clientCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        super.basicTestConnectionObserveSingleTelemetry(SECURITY_NO_SEC, clientCredentials, clientEndpoint, true, true);
    }

    @Test
    public void testWithNoSecQueueModeConnectLwm2mSuccessAndObserveCompositeAllTelemetry_Both_UpdateProfileAfterConnected() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC + "_ObserveCompositeAll";
        LwM2MDeviceCredentials clientCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = super.getTransportConfiguration(TELEMETRY_WITH_SINGLE_PARAMS_OBJECT_ID_5_ID_3, getBootstrapServerCredentialsNoSec(NONE));
        transportConfiguration.getObserveAttr().setObserveStrategy(COMPOSITE_ALL);
        super.basicTestConnectionObserveCompositeTelemetry(SECURITY_NO_SEC, clientCredentials, clientEndpoint, transportConfiguration, 1, 0);
    }

    @Test
    public void testWithNoSecQueueModeConnectLwm2mSuccessAndObserveCompositeByObjectTelemetry_Both_UpdateProfileAfterConnected() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC + "_ObserveCompositeByObject";
        LwM2MDeviceCredentials clientCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = super.getTransportConfiguration(TELEMETRY_WITH_SINGLE_PARAMS_OBJECT_ID_5_ID_3, getBootstrapServerCredentialsNoSec(NONE));
        transportConfiguration.getObserveAttr().setObserveStrategy(COMPOSITE_BY_OBJECT);
        super.basicTestConnectionObserveCompositeTelemetry(SECURITY_NO_SEC, clientCredentials, clientEndpoint, transportConfiguration, 2, 1);
    }

    @Test
    public void testWithNoSecQueueModeConnectLwm2mSuccessAndObserveCompositeByObjectTelemetry_Single_UpdateProfileAfterConnected() throws Exception {
        String clientEndpoint = CLIENT_ENDPOINT_NO_SEC + "_ObserveCompositeByObject_Single";
        LwM2MDeviceCredentials clientCredentials = getDeviceCredentialsNoSec(createNoSecClientCredentials(clientEndpoint));
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = super.getTransportConfiguration(TELEMETRY_WITH_SINGLE_PARAMS_OBJECT_ID_5_ID_3, getBootstrapServerCredentialsNoSec(NONE));
        transportConfiguration.getObserveAttr().setObserveStrategy(COMPOSITE_BY_OBJECT);
        super.basicTestConnectionObserveCompositeTelemetry(SECURITY_NO_SEC, clientCredentials, clientEndpoint, transportConfiguration, 2, 2);
    }
}

