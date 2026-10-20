// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.tests.devicessmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.openqa.selenium.Keys;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.tabs.ChangeDeviceOwnerTabHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

@Feature("Change owner")
public class ChangeOwnerTest extends AbstractDeviceTest {

    private ChangeDeviceOwnerTabHelper changeDeviceOwnerTab;
    private CustomerPageHelper customerPage;
    private Device device;
    private Device device1;
    private String customerName;

    @BeforeClass
    public void create() {
        changeDeviceOwnerTab = new ChangeDeviceOwnerTabHelper(driver);
        customerPage = new CustomerPageHelper(driver);
        Customer customer = testRestClient.postCustomer(EntityPrototypes.defaultCustomerPrototype(ENTITY_NAME + random()));
        customerName = customer.getName();
        device1 = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Device " + random()));
    }

    @AfterClass
    public void deleteCustomer() {
        deleteCustomerByName(customerName);
        deleteDeviceByName(device1.getName());
    }

    @BeforeMethod
    public void createDevice() {
        device = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype(ENTITY_NAME));
        deviceName = device.getName();
    }

    @Test(groups = "smoke")
    @Description("Change owner")
    public void changeOwner() {
        sideBarMenuView.goToDevicesPage();
        devicePage.changeOwnerSelectedDevices(deviceName);
        changeDeviceOwnerTab.changeOwnerOn(customerName);
        assertIsDisplayed(devicePage.deviceOwnerOnPage(deviceName));
        assertThat(devicePage.deviceOwnerOnPage(deviceName).getText())
                .as("Customer added correctly").isEqualTo(customerName);

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(customerName).click();

        assertIsDisplayed(devicePage.device(deviceName));
    }

    @Test(groups = "smoke")
    @Description("Can't change owner with empty owner field")
    public void changeOwnerWithoutEnterName() {
        sideBarMenuView.goToDevicesPage();
        devicePage.changeOwnerSelectedDevices(deviceName);
        changeDeviceOwnerTab.selectOwner(customerName);
        changeDeviceOwnerTab.clearBtn().click();
        changeDeviceOwnerTab.changeOwnerField().sendKeys(Keys.ESCAPE);

        assertIsDisplayed(changeDeviceOwnerTab.errorOwnerRequired());
        assertIsDisable(changeDeviceOwnerTab.changeOwnerBtnVisible());
    }

    @Test(groups = "smoke")
    @Description("Can't change owner with only space in owner field")
    public void changeOwnerWithOnlySpaseInChangeOwnerField() {
        sideBarMenuView.goToDevicesPage();
        devicePage.changeOwnerSelectedDevices(deviceName);
        changeDeviceOwnerTab.changeOwnerField().sendKeys(" ");
        changeDeviceOwnerTab.changeOwnerField().sendKeys(Keys.ESCAPE);

        assertIsDisable(changeDeviceOwnerTab.changeOwnerBtnVisible());
    }

    @Test(groups = "smoke")
    @Description("Change owner for several device")
    public void changeOwnerForSeveralDevice() {
        sideBarMenuView.goToDevicesPage();
        devicePage.changeOwnerSelectedDevices(deviceName, device1.getName());
        changeDeviceOwnerTab.changeOwnerOn(customerName);
        List.of(deviceName, device1.getName())
                .forEach(d -> assertIsDisplayed(devicePage.deviceOwnerOnPage(d)));
        List.of(deviceName, device1.getName())
                .forEach(d -> assertThat(devicePage.deviceOwnerOnPage(d).getText())
                        .as("Customer added correctly").isEqualTo(customerName));

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(customerName).click();

        List.of(deviceName, device1.getName())
                .forEach(d -> assertIsDisplayed(devicePage.device(d)));
    }
}
