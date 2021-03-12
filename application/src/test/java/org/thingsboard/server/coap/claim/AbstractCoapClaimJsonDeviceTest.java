/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.coap.claim;

import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.common.data.CoapDeviceType;
import org.thingsboard.server.common.data.TransportPayloadType;

@Slf4j
public abstract class AbstractCoapClaimJsonDeviceTest extends AbstractCoapClaimDeviceTest {
    
    @Before
    public void beforeTest() throws Exception {
        super.processBeforeTest("Test Claim device Json", CoapDeviceType.DEFAULT, TransportPayloadType.JSON);
        createCustomerAndUser();
    }

    @After
    public void afterTest() throws Exception {
        super.afterTest();
    }

    @Test
    public void testClaimingDevice() throws Exception {
        super.testClaimingDevice();
    }

    @Test
    public void testClaimingDeviceWithoutSecretAndDuration() throws Exception {
        super.testClaimingDeviceWithoutSecretAndDuration();
    }
}
