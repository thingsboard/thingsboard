// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
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
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;

public class ManageCustomerEntityView extends AbstractDriverBaseTest {
    private SideBarMenuViewHelper sideBarMenuView;
    private CustomerPageHelper customerPage;
    private final String iconText = ": Entity Views";

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @Epic("Customers smoke tests")
    @Feature("Manage customer entity view ")
    @Test(groups = "smoke")
    @Description("Open manage window by right corner btn")
    public void openWindowByRightCornerBtn() {
        sideBarMenuView.goToAllCustomers();
        customerPage.setCustomerName();
        customerPage.manageCustomerEntityViewBtn(customerPage.getCustomerName()).click();

        Assert.assertTrue(urlContains("entityViews"));
        Assert.assertNotNull(customerPage.customerUserIconHeader());
        Assert.assertTrue(customerPage.customerUserIconHeader().isDisplayed());
        Assert.assertTrue(customerPage.customerUserIconHeader().getText().contains(customerPage.getCustomerName() + iconText));
        Assert.assertTrue(customerPage.customerManageWindowIconHead().getText().contains(customerPage.getCustomerName() + iconText));
    }

    @Epic("Customers smoke tests")
    @Feature("Manage customer entity view ")
    @Test(groups = "smoke")
    @Description("Open manage window by btn in entity view")
    public void openWindowByView() {
        sideBarMenuView.goToAllCustomers();
        customerPage.setCustomerName();
        customerPage.entity(customerPage.getCustomerName()).click();
        customerPage.manageEntityViewBtnView().click();

        Assert.assertTrue(urlContains("entityViews"));
        Assert.assertNotNull(customerPage.customerUserIconHeader());
        Assert.assertTrue(customerPage.customerUserIconHeader().isDisplayed());
        Assert.assertTrue(customerPage.customerUserIconHeader().getText().contains(customerPage.getCustomerName() + iconText));
        Assert.assertTrue(customerPage.customerManageWindowIconHead().getText().contains(customerPage.getCustomerName() + iconText));
    }
}