// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.openqa.selenium.Keys;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.OWNER_NOT_SELECTED_ERROR;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;

public class CustomerChangeOwnerTest extends AbstractDriverBaseTest {
    private SideBarMenuViewHelper sideBarMenuView;
    private CustomerPageHelper customerPage;
    private final String title = ENTITY_NAME + random();
    private final String title1 = ENTITY_NAME + random() + '1';

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @AfterMethod()
    public void delete() {
        testRestClient.deleteCustomer(getCustomerByName(title).getId());
    }

    @Epic("Customers smoke tests")
    @Feature("Change customer owner")
    @Test(priority = 10, groups = "smoke")
    @Description("Change owner")
    public void changeOwner() {
        testRestClient.postCustomer(defaultCustomerPrototype(title));
        testRestClient.postCustomer(defaultCustomerPrototype(title1));

        sideBarMenuView.goToAllCustomers();
        customerPage.checkBox(title1).click();
        customerPage.changeOwnerBtn().click();
        customerPage.changeOwner(title);
        customerPage.manageCustomerGroupsBtn(title).click();

        Assert.assertNotNull(customerPage.entity(title1));
        Assert.assertTrue(customerPage.entity(title1).isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Change customer owner")
    @Test(priority = 10, groups = "smoke")
    @Description("Change owner without select customer")
    public void changeOwnerWithoutName() {
        testRestClient.postCustomer(defaultCustomerPrototype(title));

        sideBarMenuView.goToAllCustomers();
        customerPage.checkBox(title).click();
        customerPage.changeOwnerBtn().click();
        customerPage.changeOwnerViewField().click();
        customerPage.changeOwnerViewField().sendKeys(Keys.ESCAPE);
        jsClick(customerPage.changeOwnerViewChangeOwnerBtnVisible());

        Assert.assertFalse(customerPage.changeOwnerViewChangeOwnerBtnVisible().isEnabled());
        Assert.assertNotNull(customerPage.errorMessage());
        Assert.assertTrue(customerPage.errorMessage().isDisplayed());
        Assert.assertEquals(customerPage.errorMessage().getText(), OWNER_NOT_SELECTED_ERROR);
    }

    @Epic("Customers smoke tests")
    @Feature("Change customer owner")
    @Test(priority = 10, groups = "smoke")
    @Description("Change owner with only space")
    public void changeOwnerWithOnlySpace() {
        testRestClient.postCustomer(defaultCustomerPrototype(title));

        sideBarMenuView.goToAllCustomers();
        customerPage.checkBox(title).click();
        customerPage.changeOwnerBtn().click();
        customerPage.changeOwnerViewField().sendKeys(" ");

        Assert.assertFalse(customerPage.changeOwnerViewChangeOwnerBtnVisible().isEnabled());
    }

    @Epic("Customers smoke tests")
    @Feature("Change customer owner")
    @Test(priority = 10, groups = "smoke")
    @Description("Change owner several customers")
    public void changeOwnerSeveralCustomers() {
        String title3 = title + '2';
        testRestClient.postCustomer(defaultCustomerPrototype(title));
        testRestClient.postCustomer(defaultCustomerPrototype(title1));
        testRestClient.postCustomer(defaultCustomerPrototype(title3));

        sideBarMenuView.goToAllCustomers();
        customerPage.checkBox(title1).click();
        customerPage.checkBox(title3).click();
        customerPage.changeOwnerBtn().click();
        customerPage.changeOwner(title);
        customerPage.manageCustomerGroupsBtn(title).click();

        Assert.assertNotNull(customerPage.entity(title1));
        Assert.assertNotNull(customerPage.entity(title3));
        Assert.assertTrue(customerPage.entity(title1).isDisplayed());
        Assert.assertTrue(customerPage.entity(title3).isDisplayed());
    }
}
