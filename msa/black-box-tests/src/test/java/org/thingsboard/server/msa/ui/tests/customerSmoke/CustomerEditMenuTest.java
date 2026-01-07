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
package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.openqa.selenium.Keys;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.DashboardPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_CUSTOMER_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.PHONE_NUMBER_ERROR_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;

public class CustomerEditMenuTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private LoginPageHelper loginPage;
    private CustomerPageHelper customerPage;
    private DashboardPageHelper dashboardPage;
    private String customerName;

    @BeforeClass
    public void login() {
        loginPage = new LoginPageHelper(driver);
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
        dashboardPage = new DashboardPageHelper(driver);
        loginPage.authorizationTenant();
    }

    @AfterMethod
    public void delete() {
        if (customerName != null) {
            testRestClient.deleteCustomer(getCustomerByName(customerName).getId());
            customerName = null;
        }
    }

    @BeforeMethod
    public void reLogin() {
        if (getJwtTokenFromLocalStorage() == null) {
            loginPage.authorizationTenant();
        }
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 10, groups = "smoke")
    @Description("Change title by edit menu")
    public void changeTitle() {
        String customerName = "Changed" + getRandomNumber();
        testRestClient.postCustomer(defaultCustomerPrototype(ENTITY_NAME + random()));
        this.customerName = customerName;

        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.setHeaderName();
        String titleBefore = customerPage.getHeaderName();
        customerPage.editPencilBtn().click();
        customerPage.changeTitleEditMenu(customerName);
        customerPage.doneBtnEditView().click();
        customerPage.setHeaderName();
        String titleAfter = customerPage.getHeaderName();

        Assert.assertNotEquals(titleBefore, titleAfter);
        Assert.assertEquals(titleAfter, customerName);
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Delete title and save")
    public void deleteTitle() {
        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.titleFieldEntityView().clear();

        Assert.assertFalse(customerPage.doneBtnEditViewVisible().isEnabled());
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Save only with space in title")
    public void saveOnlyWithSpace() {
        sideBarMenuView.customerBtn().click();
        customerPage.setCustomerName();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.changeTitleEditMenu(" ");
        customerPage.doneBtnEditView().click();
        customerPage.setHeaderName();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), EMPTY_CUSTOMER_MESSAGE);
        Assert.assertEquals(customerPage.getCustomerName(), customerPage.getHeaderName());
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "editMenuDescription")
    @Description("Write the description and save the changes/Change the description and save the changes/Delete the description and save the changes")
    public void editDescription(String description, String newDescription, String finalDescription) {
        String name = ENTITY_NAME + random();
        testRestClient.postCustomer(EntityPrototypes.defaultCustomerPrototype(name, description));
        customerName = name;

        sideBarMenuView.customerBtn().click();
        customerPage.entity(name).click();
        customerPage.editPencilBtn().click();
        customerPage.descriptionEntityView().sendKeys(newDescription);
        customerPage.doneBtnEditView().click();
        customerPage.setDescription();

        Assert.assertEquals(customerPage.getDescription(), finalDescription);
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Assigned dashboard from dashboards page")
    public void assignedDashboardFromDashboard() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;

        sideBarMenuView.dashboardBtn().click();
        dashboardPage.setDashboardTitle();
        dashboardPage.assignedBtn(dashboardPage.getDashboardTitle()).click();
        dashboardPage.assignedCustomer(customerName);
        sideBarMenuView.customerBtn().click();
        customerPage.entity(customerName).click();
        jsClick(customerPage.editPencilBtn());
        customerPage.chooseDashboard(dashboardPage.getDashboardTitle());
        customerPage.doneBtnEditView().click();
        customerPage.setDashboardFromView();
        customerPage.closeEntityViewBtn().click();
        jsClick(customerPage.manageCustomersUserBtn(customerName));
        customerPage.createCustomersUser();
        jsClick(customerPage.userLoginBtn());

        Assert.assertNotNull(customerPage.usersWidget());
        Assert.assertTrue(customerPage.usersWidget().isDisplayed());
        Assert.assertEquals(customerPage.getDashboardFromView(), dashboardPage.getDashboardTitle());
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Assigned dashboard")
    public void assignedDashboard() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDashboardsBtn(customerName).click();
        customerPage.assignedDashboard();
        sideBarMenuView.customerBtn().click();
        customerPage.entity(customerName).click();
        jsClick(customerPage.editPencilBtn());
        customerPage.chooseDashboard(customerPage.getDashboard());
        customerPage.doneBtnEditView().click();
        customerPage.setDashboardFromView();
        customerPage.closeEntityViewBtn().click();
        jsClick(customerPage.manageCustomersUserBtn(customerName));
        customerPage.createCustomersUser();
        jsClick(customerPage.userLoginBtn());

        Assert.assertNotNull(customerPage.usersWidget());
        Assert.assertTrue(customerPage.usersWidget().isDisplayed());
        Assert.assertEquals(customerPage.getDashboard(), customerPage.getDashboardFromView());
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = { "smoke", "broken" })
    @Description("Assigned dashboard without hide")
    public void assignedDashboardWithoutHide() {
        String customerName = ENTITY_NAME + random();
        String dashboardName = "Firmware";
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDashboardsBtn(customerName).click();
        customerPage.assignedDashboard(dashboardName);
        sideBarMenuView.customerBtn().click();
        customerPage.entity(customerName).click();
        jsClick(customerPage.editPencilBtn());
        customerPage.chooseDashboard(dashboardName);
        customerPage.disableHideHomeDashboardToolbar();
        customerPage.doneBtnEditView().click();
        customerPage.waitUntilDashboardFieldToBeNotEmpty();
        customerPage.setDashboardFromView();
        customerPage.closeEntityViewBtn().click();
        jsClick(customerPage.manageCustomersUserBtn(customerName));
        customerPage.createCustomersUser();
        jsClick(customerPage.userLoginBtn());

        Assert.assertTrue(customerPage.usersWidget().isDisplayed());
        Assert.assertEquals(dashboardName, customerPage.getDashboardFromView());
        Assert.assertNotNull(customerPage.stateController());
        Assert.assertNotNull(customerPage.filterBtn());
        Assert.assertNotNull(customerPage.timeBtn());
        Assert.assertTrue(customerPage.stateController().isDisplayed());
        Assert.assertTrue(customerPage.filterBtn().isDisplayed());
        Assert.assertTrue(customerPage.timeBtn().isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Add phone number")
    public void addPhoneNumber() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;
        String number = "2015550123";

        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.phoneNumberEntityView().sendKeys(number);
        customerPage.doneBtnEditView().click();

        Assert.assertTrue(customerPage.phoneNumberEntityView().getAttribute("value").contains(number));
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "incorrectPhoneNumber")
    @Description("Add incorrect phone number")
    public void addIncorrectPhoneNumber(String number) {
        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.enterPhoneNumber(number);

        Assert.assertFalse(customerPage.doneBtnEditViewVisible().isEnabled());
        Assert.assertNotNull(customerPage.errorMessage());
        Assert.assertTrue(customerPage.errorMessage().isDisplayed());
        Assert.assertEquals(customerPage.errorMessage().getText(), PHONE_NUMBER_ERROR_MESSAGE);
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(priority = 30, groups = "smoke")
    @Description("Add all information")
    public void addAllInformation() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        this.customerName = customerName;
        String text = "Text";
        String email = "email@mail.com";
        String number = "2015550123";

        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.selectCountryEntityView();
        customerPage.descriptionEntityView().sendKeys(text);
        customerPage.cityEntityView().sendKeys(text);
        customerPage.stateEntityView().sendKeys(text);
        customerPage.zipEntityView().sendKeys(text);
        customerPage.addressEntityView().sendKeys(text);
        customerPage.address2EntityView().sendKeys(text);
        customerPage.phoneNumberEntityView().sendKeys(number);
        customerPage.emailEntityView().sendKeys(email);
        customerPage.doneBtnEditView().click();

        Assert.assertEquals(customerPage.countrySelectMenuEntityView().getText(), customerPage.getCountry());
        Assert.assertEquals(customerPage.descriptionEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.cityEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.stateEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.zipEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.addressEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.address2EntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.phoneNumberEntityView().getAttribute("value"), "+1" + number);
        Assert.assertEquals(customerPage.emailEntityView().getAttribute("value"), email);
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer")
    @Test(groups = "smoke")
    @Description("Delete phone number")
    public void deletePhoneNumber() {
        String customerName = ENTITY_NAME;
        int number = 2015550123;
        testRestClient.postCustomer(defaultCustomerPrototype(customerName, number));
        this.customerName = customerName;

        sideBarMenuView.customerBtn().click();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.phoneNumberEntityView().click();
        customerPage.phoneNumberEntityView().sendKeys(Keys.CONTROL + "A" + Keys.BACK_SPACE);
        customerPage.doneBtnEditView().click();

        Assert.assertTrue(customerPage.phoneNumberEntityView().getAttribute("value").isEmpty(), "Phone field is empty");
    }
}
