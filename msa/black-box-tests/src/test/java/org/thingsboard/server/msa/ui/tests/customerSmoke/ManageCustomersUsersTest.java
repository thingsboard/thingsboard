// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

public class ManageCustomersUsersTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;
    private final String iconText = "Customer Users";

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @Epic("Customers smoke tests")
    @Feature("Manage customer users")
    @Test(groups = "smoke")
    @Description("Open manage window by right corner btn")
    public void openWindowByRightCornerBtn() {
        sideBarMenuView.customerBtn().click();
        customerPage.setCustomerName();
        jsClick(customerPage.manageCustomersUserBtn(customerPage.getCustomerName()));

        Assert.assertTrue(urlContains("user"));
        Assert.assertNotNull(customerPage.customerUserIconHeader());
        Assert.assertTrue(customerPage.customerUserIconHeader().isDisplayed());
        Assert.assertTrue(customerPage.customerManageWindowIconHead().getText().contains(iconText));
    }

    @Epic("Customers smoke tests")
    @Feature("Manage customer users")
    @Test(groups = "smoke")
    @Description("Open manage window by btn in entity view")
    public void openWindowByView() {
        sideBarMenuView.customerBtn().click();
        customerPage.setCustomerName();
        customerPage.entity(customerPage.getCustomerName()).click();
        jsClick(customerPage.manageCustomersUserBtnView());

        Assert.assertTrue(urlContains("user"));
        Assert.assertNotNull(customerPage.customerUserIconHeader());
        Assert.assertTrue(customerPage.customerUserIconHeader().isDisplayed());
        Assert.assertTrue(customerPage.customerManageWindowIconHead().getText().contains(iconText));
    }
}
