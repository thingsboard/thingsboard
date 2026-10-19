// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;

public class OpenCustomerGroupTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private CustomerPageHelper customerPage;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @Epic("Customers smoke tests")
    @Feature("Open customer group")
    @Test(groups = "smoke")
    @Description("Open customer group by click on its name")
    public void openWindowByRightCornerBtn() {
        sideBarMenuView.goToCustomerGroups();
        customerPage.setEntityGroupName();
        String entityGroupName = customerPage.getEntityGroupName();
        customerPage.entity(entityGroupName).click();

        Assert.assertTrue(urlContains(getEntityGroupByName(EntityType.CUSTOMER, entityGroupName).getId().toString()));
        Assert.assertNotNull(customerPage.entityGroupTableHeader());
        Assert.assertTrue(customerPage.entityGroupTableHeader().isDisplayed());
        Assert.assertTrue(customerPage.entityGroupTableHeader().getText().contains(entityGroupName));
        Assert.assertNotNull(customerPage.entityGroupHeader(entityGroupName));
        Assert.assertTrue(customerPage.entityGroupHeader(entityGroupName).isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Open customer group")
    @Test(groups = "smoke")
    @Description("Open customer group by click on 'Open entity group' btn in customer group view")
    public void openWindowByViewBtn() {
        sideBarMenuView.goToCustomerGroups();
        customerPage.setEntityGroupName();
        String entityGroupName = customerPage.getEntityGroupName();
        customerPage.detailsBtn(entityGroupName).click();
        customerPage.openEntityGroupViewBtn().click();

        Assert.assertTrue(urlContains(getEntityGroupByName(EntityType.CUSTOMER, entityGroupName).getId().toString()));
        Assert.assertNotNull(customerPage.entityGroupTableHeader());
        Assert.assertTrue(customerPage.entityGroupTableHeader().isDisplayed());
        Assert.assertTrue(customerPage.entityGroupTableHeader().getText().contains(entityGroupName));
        Assert.assertNotNull(customerPage.entityGroupHeader(entityGroupName));
        Assert.assertTrue(customerPage.entityGroupHeader(entityGroupName).isDisplayed());
    }
}