// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap.telemetry.timeseries;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;

@Slf4j
public abstract class AbstractCoapTimeseriesJsonIntegrationTest extends AbstractCoapTimeseriesIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device json payload")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTest(configProperties);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTest();
    }


    @Test
    public void testPushTelemetry() throws Exception {
        super.testPushTelemetry();
    }

    @Test
    public void testPushTelemetryWithTs() throws Exception {
        super.testPushTelemetryWithTs();
    }


}
