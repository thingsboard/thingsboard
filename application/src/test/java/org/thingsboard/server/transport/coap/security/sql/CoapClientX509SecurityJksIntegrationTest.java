// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap.security.sql;

import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.dao.service.DaoSqlTest;
import org.thingsboard.server.transport.coap.security.AbstractCoapSecurityIntegrationTest;

@Slf4j
@DaoSqlTest
@TestPropertySource(properties = {
        "coap.dtls.credentials.type=KEYSTORE",
        "coap.dtls.credentials.keystore.store_file=coap/credentials/coapserverTest.jks",
        "coap.dtls.credentials.keystore.key_password=server_ks_password",
        "coap.dtls.credentials.keystore.key_alias=server",
})
public class CoapClientX509SecurityJksIntegrationTest extends AbstractCoapSecurityIntegrationTest {

    @Before
    public void beforeTest() throws Exception {
        processBeforeTest();
    }

    @Test
    public void testX509NoTrustFromJksConnectCoapSuccessUpdateAttributesSuccess() throws Exception {
        clientX509FromJksUpdateAttributesTest();
    }
}
