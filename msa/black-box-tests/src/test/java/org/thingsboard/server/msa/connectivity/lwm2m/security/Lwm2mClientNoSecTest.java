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
package org.thingsboard.server.msa.connectivity.lwm2m.security;

import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.connectivity.lwm2m.AbstractLwm2mClientTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mDevicesForTest;

import static org.thingsboard.server.msa.connectivity.lwm2m.client.Lwm2mTestHelper.CLIENT_ENDPOINT_NO_SEC;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;

@DisableUIListeners
public class Lwm2mClientNoSecTest extends AbstractLwm2mClientTest {
    private Lwm2mDevicesForTest lwm2mDevicesForTest;

    @BeforeMethod
    public void setUp() throws Exception {
        testRestClient.login(TENANT_EMAIL, TENANT_PASSWORD);
        this.lwm2mDevicesForTest = new Lwm2mDevicesForTest(initTest("lwm2m-NoSec-profile" + "-" +  RandomStringUtils.randomAlphanumeric(7)));
    }

    @AfterMethod
    public void tearDown() {
        destroyAfter(this.lwm2mDevicesForTest);
    }

    @Test
    public void connectLwm2mClientNoSecWithLwm2mServer() throws Exception {
        createLwm2mDevicesForConnectNoSec(CLIENT_ENDPOINT_NO_SEC, this.lwm2mDevicesForTest);
        basicTestConnection(this.lwm2mDevicesForTest.getLwM2MTestClient(), "TestConnection Lwm2m NoSec (msa)");
    }
}
