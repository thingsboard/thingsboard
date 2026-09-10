// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.security.cid;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.dao.service.DaoSqlTest;


@TestPropertySource(properties = {
        "transport.lwm2m.dtls.connection_id_length=16"
})

@DaoSqlTest
@Slf4j
public abstract class AbstractSecurityLwM2MIntegrationDtlsCidLength16Test extends AbstractSecurityLwM2MIntegrationDtlsCidLengthTest {

    private static final Integer  serverDtlsCidLength = 16;

    protected void  testNoSecDtlsCidLength(Integer clientDtlsCidLength) throws Exception {
        testNoSecDtlsCidLength(clientDtlsCidLength, serverDtlsCidLength);
    }

    protected void  testPskDtlsCidLength(Integer clientDtlsCidLength) throws Exception {
        testPskDtlsCidLength(clientDtlsCidLength, serverDtlsCidLength);
    }
}
