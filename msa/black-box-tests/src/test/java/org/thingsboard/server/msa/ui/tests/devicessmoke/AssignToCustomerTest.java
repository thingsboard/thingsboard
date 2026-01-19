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
package org.thingsboard.server.msa.ui.tests.devicessmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.PUBLIC_CUSTOMER_NAME;

@Feature("Assign to customer")
public class AssignToCustomerTest extends AbstractDeviceTest {

    private AssignDeviceTabHelper assignDeviceTab;
    private CustomerPageHelper customerPage;
    private CustomerId customerId;
    private Device device;
    private Device device1;
    private String customerName;

    @BeforeClass
    public void create() {
        assignDeviceTab = new AssignDeviceTabHelper(driver);
        customerPage = new CustomerPageHelper(driver);
        Customer customer = testRestClient.postCustomer(EntityPrototypes.defaultCustomerPrototype(ENTITY_NAME + random()));
        customerId = customer.getId();
        customerName = customer.getName();
        device1 = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Device " + random()));
    }

    @AfterClass
    public void deleteCustomer() {
        deleteCustomerById(customerId);
        deleteCustomerByName(PUBLIC_CUSTOMER_NAME);
        deleteDeviceByName(device1.getName());
    }

    @BeforeMethod
    public void createDevice() {
        device = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME));
        deviceName = device.getName();
    }

    @Test(groups = "smoke")
    @Description("Assign to customer by right side of device btn")
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

    @Test(groups = "smoke")
    @Description("Assign to customer by 'Assign to customer' btn on details tab")
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

    @Test(groups = "smoke")
    @Description("Assign marked device by btn on the top")
    public void assignToCustomerMarkedDevice() {
        sideBarMenuView.goToDevicesPage();
        devicePage.assignSelectedDevices(deviceName);
        assignDeviceTab.assignOnCustomer(customerName);
        assertIsDisplayed(devicePage.deviceCustomerOnPage(deviceName));
        assertThat(devicePage.deviceCustomerOnPage(deviceName).getText())
                .as("Customer added correctly").isEqualTo(customerName);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(customerName).click();
        assertIsDisplayed(devicePage.device(deviceName));
    }

    @Test(groups = "smoke")
    @Description("Unassign from customer by right side of device btn")
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

    @Test(groups = "smoke")
    @Description("Unassign from customer by 'Unassign from customer' btn on details tab")
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

    @Test(groups = "smoke")
    @Description("Can't assign device on several customer")
    public void assignToSeveralCustomer() {
        device.setCustomerId(customerId);
        testRestClient.postDevice("", device);
        sideBarMenuView.goToDevicesPage();

        assertIsDisable(devicePage.assignBtnVisible(deviceName));
    }

    @Test(groups = "smoke")
    @Description("Can't assign public device")
    public void assignPublicDevice() {
        testRestClient.setDevicePublic(device.getId());

        sideBarMenuView.goToDevicesPage();
        assertIsDisable(devicePage.assignBtnVisible(deviceName));
    }

    @Test(groups = "smoke")
    @Description("Assign several devices by btn on the top")
    public void assignSeveralDevices() {
        sideBarMenuView.goToDevicesPage();
        devicePage.assignSelectedDevices(deviceName, device1.getName());
        assignDeviceTab.assignOnCustomer(customerName);
        assertIsDisplayed(devicePage.deviceCustomerOnPage(deviceName));
        assertThat(devicePage.deviceCustomerOnPage(deviceName).getText())
                .as("Customer added correctly").isEqualTo(customerName);
        assertThat(devicePage.deviceCustomerOnPage(device1.getName()).getText())
                .as("Customer added correctly").isEqualTo(customerName);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDevicesBtn(customerName).click();
        List.of(deviceName, device1.getName()).
                forEach(d -> assertIsDisplayed(devicePage.device(d)));
    }
}
