/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.msa.ui.tests.devicessmoke;

import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.tabs.AssignDeviceTabHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

public class AssignToCustomerTest extends AbstractDeviceTest {

    private AssignDeviceTabHelper assignDeviceTab;
    private CustomerPageHelper customerPage;
    private CustomerId customerId;
    private Device device;
    private String customerName;

    @BeforeClass
    public void create() {
        assignDeviceTab = new AssignDeviceTabHelper(driver);
        customerPage = new CustomerPageHelper(driver);
        Customer customer = testRestClient.postCustomer(EntityPrototypes.defaultCustomerPrototype(ENTITY_NAME + random()));
        customerId = customer.getId();
        customerName = customer.getName();
    }

    @AfterClass
    public void deleteCustomer() {
        deleteCustomerById(customerId);
        deleteCustomerByName("Public");
    }

    @BeforeMethod
    public void createDevice() {
        device = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME));
        deviceName = device.getName();
    }

    @Test
    public void assignToCustomerByRightSideBtn() {
        sideBarMenuView.goToDevicesPage();
        devicePage.assignBtn(deviceName).click();
        assignDeviceTab.assignOnCustomer(customerName);
        assertIsDisplayed(devicePage.deviceCustomerOnPage(deviceName));
        assertThat(devicePage.deviceCustomerOnPage(deviceName).getText())
                .as("Customer added correctly").isEqualTo(customerName);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(customerName).click();
        assertIsDisplayed(devicePage.device(deviceName));
    }

    @Test
    public void assignToCustomerFromDetailsTab() {
        sideBarMenuView.goToDevicesPage();
        devicePage.device(deviceName).click();
        devicePage.assignBtnDetailsTab().click();
        assignDeviceTab.assignOnCustomer(customerName);
        String customerInAssignedField = devicePage.assignFieldDetailsTab().getAttribute("value");
        devicePage.closeDeviceDetailsViewBtn().click();
        assertIsDisplayed(devicePage.deviceCustomerOnPage(deviceName));
        assertThat(devicePage.deviceCustomerOnPage(deviceName).getText())
                .as("Customer added correctly").isEqualTo(customerName);
        assertThat(customerInAssignedField)
                .as("Customer in details tab added correctly").isEqualTo(customerName);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(customerName).click();
        assertIsDisplayed(devicePage.device(deviceName));
    }

    @Test
    public void assignToCustomerMarkedDevice() {
        sideBarMenuView.goToDevicesPage();
        devicePage.assignMarkedDevices(deviceName);
        assignDeviceTab.assignOnCustomer(customerName);
        assertIsDisplayed(devicePage.deviceCustomerOnPage(deviceName));
        assertThat(devicePage.deviceCustomerOnPage(deviceName).getText())
                .as("Customer added correctly").isEqualTo(customerName);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(customerName).click();
        assertIsDisplayed(devicePage.device(deviceName));
    }

    @Test
    public void unassignedFromCustomerByRightSideBtn() {
        device.setCustomerId(customerId);
        testRestClient.postDevice("", device);

        sideBarMenuView.goToDevicesPage();
        WebElement element = devicePage.deviceCustomerOnPage(deviceName);
        devicePage.unassignedDeviceByRightSideBtn(deviceName);
        assertInvisibilityOfElement(element);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(customerName).click();
        devicePage.assertEntityIsNotPresent(deviceName);
    }

    @Test
    public void unassignedFromCustomerFromDetailsTab() {
        device.setCustomerId(customerId);
        testRestClient.postDevice("", device);

        sideBarMenuView.goToDevicesPage();
        WebElement customerInColumn = devicePage.deviceCustomerOnPage(deviceName);
        devicePage.device(deviceName).click();
        WebElement assignFieldDetailsTab = devicePage.assignFieldDetailsTab();
        devicePage.unassignedDeviceFromDetailsTab();
        assertInvisibilityOfElement(customerInColumn);
        assertInvisibilityOfElement(assignFieldDetailsTab);

        devicePage.closeDeviceDetailsViewBtn().click();
        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(customerName).click();
        devicePage.assertEntityIsNotPresent(deviceName);
    }

    @Test
    public void assignToSeveralCustomer() {
        device.setCustomerId(customerId);
        testRestClient.postDevice("", device);
        sideBarMenuView.goToDevicesPage();

        assertIsDisable(devicePage.assignBtnVisible(deviceName));
    }

    @Test
    public void assignPublicDevice() {
        testRestClient.setDevicePublic(device.getId());

        sideBarMenuView.goToDevicesPage();
        assertIsDisable(devicePage.assignBtnVisible(deviceName));
    }
}
