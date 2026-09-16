// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.tests.devicessmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.PUBLIC_CUSTOMER_NAME;

@Feature("Make device private")
public class MakeDevicePrivateTest extends AbstractDeviceTest {

    private CustomerPageHelper customerPage;

    @BeforeMethod
    public void createPublicDevice() {
        customerPage = new CustomerPageHelper(driver);
        Device device = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME + random()));
        testRestClient.setDevicePublic(device.getId());
        deviceName = device.getName();
    }

    @AfterClass
    public void deletePublicCustomer() {
        deleteCustomerByName(PUBLIC_CUSTOMER_NAME);
    }

    @Test(groups = "smoke")
    @Description("Make device private by right side btn")
    public void makeDevicePrivateByRightSideBtn() {
        sideBarMenuView.goToDevicesPage();
        devicePage.makeDevicePrivateByRightSideBtn(deviceName);
        WebElement customerInColumn = devicePage.deviceCustomerOnPage(deviceName);
        assertIsDisplayed(devicePage.deviceIsPrivateCheckbox(deviceName));
        assertInvisibilityOfElement(customerInColumn);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(PUBLIC_CUSTOMER_NAME).click();
        devicePage.assertEntityIsNotPresent(deviceName);
    }

    @Test(groups = "smoke")
    @Description("Make device public by btn on details tab")
    public void makeDevicePrivateFromDetailsTab() {
        sideBarMenuView.goToDevicesPage();
        devicePage.device(deviceName).click();
        WebElement customerInColumn = devicePage.deviceCustomerOnPage(deviceName);
        devicePage.makeDevicePrivateFromDetailsTab();
        devicePage.closeDeviceDetailsViewBtn().click();
        assertIsDisplayed(devicePage.deviceIsPrivateCheckbox(deviceName));
        assertInvisibilityOfElement(customerInColumn);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(PUBLIC_CUSTOMER_NAME).click();
        devicePage.assertEntityIsNotPresent(deviceName);
    }
}
