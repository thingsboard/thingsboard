// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
