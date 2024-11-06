package org.thingsboard.server.transport.coap.security.sql;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.ClaimRequest;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.dao.device.claim.ClaimResponse;
import org.thingsboard.server.dao.device.claim.ClaimResult;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.coap.CoapTestClient;
import org.thingsboard.server.transport.coap.CoapTestClientX509;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;
import org.thingsboard.server.transport.coap.security.AbstractSecurityCoapIntegrationTest;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@DaoSqlTest
public class X509_NoTrustCoapIntegrationTest extends AbstractSecurityCoapIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Post Telemetry device json payload, security X509")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.JSON)
                .build();
        processBeforeTestX509(configProperties);
    }

    @After
    public void afterTest() throws Exception {
        processAfterTestX509();
    }


    @Test
    public void testWithX509NoTrustConnectCoapSuccess() throws Exception {
        log.info("Start test WithX509NoTrustConnectCoapSuccess");
        processAttributesTestX509();
    }

}
