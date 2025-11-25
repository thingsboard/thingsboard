/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.security.cid;

import lombok.extern.slf4j.Slf4j;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.dao.service.DaoSqlTest;


@TestPropertySource(properties = {
        "transport.lwm2m.dtls.connection_id_length="
})

@DaoSqlTest
@Slf4j
public abstract class AbstractSecurityLwM2MIntegrationDtlsCidLengthNullTest extends AbstractSecurityLwM2MIntegrationDtlsCidLengthTest {

    private static final Integer  serverDtlsCidLength = null;

    protected void  testNoSecDtlsCidLength(Integer dtlsCidLength) throws Exception {
        testNoSecDtlsCidLength(dtlsCidLength, serverDtlsCidLength);
    }
    protected void  testPskDtlsCidLength(Integer dtlsCidLength) throws Exception {
        testPskDtlsCidLength(dtlsCidLength, serverDtlsCidLength);
    }
}
