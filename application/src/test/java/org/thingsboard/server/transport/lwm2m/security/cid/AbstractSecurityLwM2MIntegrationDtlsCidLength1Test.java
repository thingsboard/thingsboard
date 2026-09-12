// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.security.cid;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.dao.service.DaoSqlTest;


@TestPropertySource(properties = {
        "transport.lwm2m.dtls.connection_id_length=1"
})

@DaoSqlTest
@Slf4j
public abstract class AbstractSecurityLwM2MIntegrationDtlsCidLength1Test extends AbstractSecurityLwM2MIntegrationDtlsCidLengthTest {


    private static final Integer serverDtlsCidLength = 1;

    protected void  testNoSecDtlsCidLength(Integer dtlsCidLength) throws Exception {
        testNoSecDtlsCidLength(dtlsCidLength, serverDtlsCidLength);
    }
    protected void  testPskDtlsCidLength(Integer dtlsCidLength) throws Exception {
        testPskDtlsCidLength(dtlsCidLength, serverDtlsCidLength);
    }
}
