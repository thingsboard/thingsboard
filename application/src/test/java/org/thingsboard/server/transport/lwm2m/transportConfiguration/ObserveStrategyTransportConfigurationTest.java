// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.transportConfiguration;

import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.transport.lwm2m.security.AbstractSecurityLwM2MIntegrationTest;

import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.COMPOSITE_ALL;
import static org.thingsboard.server.common.data.device.profile.lwm2m.TelemetryObserveStrategy.SINGLE;
import static org.thingsboard.server.transport.lwm2m.Lwm2mTestHelper.LwM2MProfileBootstrapConfigType.NONE;

public class ObserveStrategyTransportConfigurationTest extends AbstractSecurityLwM2MIntegrationTest {

    @Test
    public void testTransportConfigurationObserveStrategyBeforeParseNullAfterParseNotNull_STRATEGY_SINGLE() throws Exception {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(TELEMETRY_WITHOUT_OBSERVE, getBootstrapServerCredentialsNoSec(NONE));
        Assert.assertNotNull(transportConfiguration.getObserveAttr().getObserveStrategy());
        Assert.assertEquals(SINGLE, transportConfiguration.getObserveAttr().getObserveStrategy());
    }

    @Test
    public void testTransportConfigurationObserveStrategyBeforeParseNotNullAfterParseNotNull_STRATEGY_COMPOSITE_ALL() throws Exception {
        Lwm2mDeviceProfileTransportConfiguration transportConfiguration = getTransportConfiguration(TELEMETRY_WITH_COMPOSITE_ALL_OBSERVE_ID_3_ID_19, getBootstrapServerCredentialsNoSec(NONE));
        Assert.assertNotNull(transportConfiguration.getObserveAttr().getObserveStrategy());
        Assert.assertEquals(COMPOSITE_ALL, transportConfiguration.getObserveAttr().getObserveStrategy());
    }
}
