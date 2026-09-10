// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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

