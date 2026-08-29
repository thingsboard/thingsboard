/**
 * Copyright © 2016-2026 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.transport.coap.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.junit.Test;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.dao.service.DaoSqlTest;

@Slf4j
@DaoSqlTest
@TestPropertySource(properties = {
        "transport.coap.piggyback_timeout=2000"
})
public class CoapClientPiggybackedIntegrationTest extends AbstractCoapClientPiggybackedIntegrationTest {
    @Test
    public void testConfirmable() throws Exception {
        // response should be included in the ACK packet (piggybacked)
        testCase(true, CoAP.Type.ACK);
    }

    @Test
    public void testNonConfirmable() throws Exception {
        testCase(false, CoAP.Type.NON);
    }
}
