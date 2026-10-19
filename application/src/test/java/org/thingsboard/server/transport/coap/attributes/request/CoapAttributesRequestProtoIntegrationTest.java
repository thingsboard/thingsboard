// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap.attributes.request;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.TransportPayloadType;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.coap.CoapTestConfigProperties;

@Slf4j
@DaoSqlTest
public class CoapAttributesRequestProtoIntegrationTest extends CoapAttributesRequestIntegrationTest {

    @Before
    @Override
    public void beforeTest() throws Exception {
        CoapTestConfigProperties configProperties = CoapTestConfigProperties.builder()
                .deviceName("Test Request attribute values from the server proto")
                .coapDeviceType(CoapDeviceType.DEFAULT)
                .transportPayloadType(TransportPayloadType.PROTOBUF)
                .attributesProtoSchema(ATTRIBUTES_SCHEMA_STR)
                .build();
        processBeforeTest(configProperties);
    }

    @Test
    public void testRequestAttributesValuesFromTheServer() throws Exception {
        processProtoTestRequestAttributesValuesFromTheServer();
    }
}
