/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.DashboardPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_CUSTOMER_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.PHONE_NUMBER_ERROR_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;

public class CustomerEditMenuTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;
    private DashboardPageHelper dashboardPage;
    private String customerName;


    @BeforeMethod
    public void login() {
        openLocalhost();
        new LoginPageHelper(driver).authorizationTenant();
        testRestClient.login(TENANT_EMAIL, TENANT_PASSWORD);
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
        dashboardPage = new DashboardPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        if (customerName != null) {
            testRestClient.deleteCustomer(getCustomerByName(customerName).getId());
            customerName = null;
        }
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void changeTitle() {
        customerName = "Changed" + getRandomNumber();
        testRestClient.postCustomer(defaultCustomerPrototype(ENTITY_NAME));

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

    @Test(priority = 20, groups = "smoke")
    @Description
    public void deleteTitle() {
        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.titleFieldEntityView().clear();

        Assert.assertFalse(customerPage.doneBtnEditViewVisible().isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
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

    @Test(priority = 20, groups = "smoke")
    @Description
    public void editDescription() {
        customerName = ENTITY_NAME;
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        String description = "Description";

        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.descriptionEntityView().sendKeys(description);
        customerPage.doneBtnEditView().click();
        String description1 = customerPage.descriptionEntityView().getAttribute("value");
        customerPage.editPencilBtn().click();
        customerPage.descriptionEntityView().sendKeys(description);
        customerPage.doneBtnEditView().click();
        String description2 = customerPage.descriptionEntityView().getAttribute("value");
        customerPage.editPencilBtn().click();
        customerPage.changeDescription("");
        customerPage.doneBtnEditView().click();

        Assert.assertEquals(description, description1);
        Assert.assertEquals(description + description, description2);
        Assert.assertTrue(customerPage.descriptionEntityView().getAttribute("value").isEmpty());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void assignedDashboardFromDashboard() {
        customerName = ENTITY_NAME;
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));

        sideBarMenuView.dashboardBtn().click();
        dashboardPage.setDashboardTitle();
        dashboardPage.assignedBtn(dashboardPage.getDashboardTitle()).click();
        dashboardPage.assignedCustomer(customerName);
        sideBarMenuView.customerBtn().click();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.chooseDashboard();
        customerPage.doneBtnEditView().click();
        customerPage.setDashboardFromView();
        customerPage.closeEntityViewBtn().click();
        customerPage.manageCustomersUserBtn(customerName).click();
        customerPage.createCustomersUser();
        customerPage.userLoginBtn().click();

        Assert.assertNotNull(customerPage.usersWidget());
        Assert.assertTrue(customerPage.usersWidget().isDisplayed());
        Assert.assertEquals(customerPage.getDashboardFromView(), dashboardPage.getDashboardTitle());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void assignedDashboard() {
        customerName = ENTITY_NAME;
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDashboardsBtn(customerName).click();
        customerPage.assignedDashboard();
        sideBarMenuView.customerBtn().click();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.chooseDashboard();
        customerPage.doneBtnEditView().click();
        customerPage.setDashboardFromView();
        customerPage.closeEntityViewBtn().click();
        customerPage.manageCustomersUserBtn(customerName).click();
        customerPage.createCustomersUser();
        customerPage.userLoginBtn().click();

        Assert.assertNotNull(customerPage.usersWidget());
        Assert.assertTrue(customerPage.usersWidget().isDisplayed());
        Assert.assertEquals(customerPage.getDashboard(), customerPage.getDashboardFromView());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void assignedDashboardWithoutHide() {
        customerName = ENTITY_NAME;
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDashboardsBtn(customerName).click();
        customerPage.assignedDashboard();
        sideBarMenuView.customerBtn().click();
        customerPage.entity(customerName).click();
        customerPage.editPencilBtn().click();
        customerPage.chooseDashboard();
        customerPage.hideHomeDashboardToolbarCheckbox().click();
        customerPage.doneBtnEditView().click();
        customerPage.setDashboardFromView();
        customerPage.closeEntityViewBtn().click();
        customerPage.manageCustomersUserBtn(customerName).click();
        customerPage.createCustomersUser();
        customerPage.userLoginBtn().click();

        Assert.assertNotNull(customerPage.usersWidget());
        Assert.assertTrue(customerPage.usersWidget().isDisplayed());
        Assert.assertEquals(customerPage.getDashboard(), customerPage.getDashboardFromView());
        Assert.assertNotNull(customerPage.stateController());
        Assert.assertNotNull(customerPage.filterBtn());
        Assert.assertNotNull(customerPage.timeBtn());
        Assert.assertTrue(customerPage.stateController().isDisplayed());
        Assert.assertTrue(customerPage.filterBtn().isDisplayed());
        Assert.assertTrue(customerPage.timeBtn().isDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void addPhoneNumber() {
        customerName = ENTITY_NAME;
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
        String number = "2015550123";

        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.phoneNumberEntityView().sendKeys(number);
        customerPage.doneBtnEditView().click();

        Assert.assertTrue(customerPage.phoneNumberEntityView().getAttribute("value").contains(number));
    }

    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "incorrectPhoneNumber")
    @Description
    public void addIncorrectPhoneNumber(String number) {
        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.phoneNumberEntityView().sendKeys(number);
        boolean doneBtnIsEnable = customerPage.doneBtnEditViewVisible().isEnabled();
        customerPage.doneBtnEditViewVisible().click();

        Assert.assertFalse(doneBtnIsEnable);
        Assert.assertNotNull(customerPage.errorMessage());
        Assert.assertTrue(customerPage.errorMessage().isDisplayed());
        Assert.assertEquals(customerPage.errorMessage().getText(), PHONE_NUMBER_ERROR_MESSAGE);
    }

    @Test(priority = 30, groups = "smoke")
    @Description
    public void addAllInformation() {
        customerName = ENTITY_NAME;
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));
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
}