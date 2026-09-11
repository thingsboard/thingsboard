// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.connectivity.lwm2m.rpc;

import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.DisableUIListeners;
import org.thingsboard.server.msa.connectivity.lwm2m.AbstractLwm2mClientTest;
import org.thingsboard.server.msa.connectivity.lwm2m.Lwm2mDevicesForTest;

import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;

@DisableUIListeners
public class Lwm2mObserveCompositeTest extends AbstractLwm2mClientTest {

    private Lwm2mDevicesForTest lwm2mDevicesForTest;

    private final static String name = "lwm2m-NoSec-ObserveComposite";

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
        observeCompositeResource_Update_AfterUpdateRegistration_test(this.lwm2mDevicesForTest.getLwM2MTestClient(), this.lwm2mDevicesForTest.getLwM2MDeviceTest().getId());
    }
}
