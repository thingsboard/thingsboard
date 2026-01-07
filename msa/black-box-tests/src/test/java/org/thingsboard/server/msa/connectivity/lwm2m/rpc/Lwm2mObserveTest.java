/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.msa.connectivity.lwm2m.rpc;

import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.connectivity.lwm2m.AbstractLwm2mClientTest;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mDevicesForTest;

import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;

@DisableUIListeners
public class Lwm2mObserveTest extends AbstractLwm2mClientTest {

    private Lwm2mDevicesForTest lwm2mDevicesForTest;

    private final static String name = "lwm2m-NoSec-Observe";

    @BeforeMethod
    public void setUp() throws Exception {
        testRestClient.login(TENANT_EMAIL, TENANT_PASSWORD);
        this.lwm2mDevicesForTest = new Lwm2mDevicesForTest(initTest(name + "-profile" +  RandomStringUtils.randomAlphanumeric(7)));
    }

    @AfterMethod
    public void tearDown() {
        destroyAfter(this.lwm2mDevicesForTest);
    }

    @Test
    public void testObserveResource_Update_AfterUpdateRegistration() throws Exception {
        createLwm2mDevicesForConnectNoSec( name + "-" +  RandomStringUtils.randomAlphanumeric(7), this.lwm2mDevicesForTest );
        observeResource_Update_AfterUpdateRegistration_test(this.lwm2mDevicesForTest.getLwM2MTestClient(), this.lwm2mDevicesForTest.getLwM2MDeviceTest().getId());
    }
}
