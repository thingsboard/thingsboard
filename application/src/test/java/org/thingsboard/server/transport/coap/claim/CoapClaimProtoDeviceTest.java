// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap.claim;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.gen.transport.TransportApiProtos;
import org.thingsboard.server.transport.coap.CoapTestClient;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;

@Slf4j
@DaoSqlTest
public class CoapClaimProtoDeviceTest extends CoapClaimDeviceTest {

    @Before
    public void beforeTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Claim device Proto")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .build();
        processBeforeTest(configProperties);
    }

    @After
    public void afterTest() throws Exception { super.afterTest(); }

    @Test
    public void testClaimingDevice() throws Exception {
        processTestClaimingDevice(false);
    }

    @Test
    public void testClaimingDeviceWithoutSecretAndDuration() throws Exception {
        processTestClaimingDevice(true);
    }

    @Override
    protected void processTestClaimingDevice(boolean emptyPayload) throws Exception {
        client = new CoapTestClient(accessToken, FeatureType.CLAIM);
        byte[] payloadBytes;
        if (emptyPayload) {
            TransportApiProtos.ClaimDevice claimDevice = getClaimDevice(0, emptyPayload);
            payloadBytes = claimDevice.toByteArray();
        } else {
            TransportApiProtos.ClaimDevice claimDevice = getClaimDevice(60000, emptyPayload);
            payloadBytes = claimDevice.toByteArray();
        }
        TransportApiProtos.ClaimDevice claimDevice = getClaimDevice(1, emptyPayload);
        byte[] failurePayloadBytes = claimDevice.toByteArray();
        validateClaimResponse(emptyPayload, client, payloadBytes, failurePayloadBytes);
    }

    private TransportApiProtos.ClaimDevice getClaimDevice(long duration, boolean emptyPayload) {
        TransportApiProtos.ClaimDevice.Builder claimDeviceBuilder = TransportApiProtos.ClaimDevice.newBuilder();
        if (!emptyPayload) {
            claimDeviceBuilder.setSecretKey("value");
        }
        if (duration > 0) {
            claimDeviceBuilder.setSecretKey("value");
            claimDeviceBuilder.setDurationMs(duration);
        } else {
            claimDeviceBuilder.setDurationMs(0);
        }
        return claimDeviceBuilder.build();
    }


}
