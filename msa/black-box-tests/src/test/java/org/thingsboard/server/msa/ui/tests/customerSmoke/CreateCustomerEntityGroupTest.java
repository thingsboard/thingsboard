// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_GROUP_NAME_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.SAME_NAME_WARNING_ENTITY_GROUP_MESSAGE;

public class CreateCustomerEntityGroupTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private CustomerPageHelper customerPage;
    private String customerGroupName;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        if (customerGroupName != null) {
            testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, customerGroupName).getId());
            customerGroupName = null;
        }
    }

    @Epic("Customers smoke tests")
    @Feature("Create customer entity group")
    @Test(priority = 10, groups = "smoke")
    @Description("Add customer's group specifying the name (text/numbers /special characters)")
    public void createCustomerGroup() {
        customerGroupName = ENTITY_NAME + random();

        sideBarMenuView.goToCustomerGroups();
        customerPage.plusBtn().click();
        customerPage.addEntityGroupViewNameField().click();
        customerPage.addEntityGroupViewNameField().sendKeys(customerGroupName);
        customerPage.addBtnC().click();
        customerPage.refreshBtn().click();

        Assert.assertNotNull(customerPage.entity(customerGroupName));
        Assert.assertTrue(customerPage.entity(customerGroupName).isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Create customer entity group")
    @Test(priority = 20, groups = "smoke")
    @Description("Add customer's group without the name")
    public void createCustomerGroupWithoutName() {
        sideBarMenuView.goToCustomerGroups();
        customerPage.plusBtn().click();
        customerPage.addBtnC().click();

        Assert.assertNotNull(customerPage.addEntityGroupView());
        Assert.assertTrue(customerPage.errorMessage().isDisplayed());
        Assert.assertEquals(customerPage.errorMessage().getText(), "Name is required.");
    }

    @Epic("Customers smoke tests")
    @Feature("Create customer entity group")
    @Test(priority = 20, groups = "smoke")
    @Description("Create customer's group only with spase in name")
    public void createCustomerWithOnlySpace() {
        sideBarMenuView.goToCustomerGroups();
        customerPage.plusBtn().click();
        customerPage.addEntityGroupViewNameField().click();
        customerPage.addEntityGroupViewNameField().sendKeys(" ");
        customerPage.addBtnC().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), EMPTY_GROUP_NAME_MESSAGE);
        Assert.assertNotNull(customerPage.addEntityGroupView());
        Assert.assertTrue(customerPage.addEntityGroupView().isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Create customer entity group")
    @Test(priority = 20, groups = "smoke")
    @Description("Create a customer's group with the same name")
    public void createCustomerGroupWithSameName() {
        customerGroupName = ENTITY_NAME + random();
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerGroupName, EntityType.CUSTOMER));

        sideBarMenuView.goToCustomerGroups();
        customerPage.plusBtn().click();
        customerPage.addEntityGroupViewNameField().sendKeys(customerGroupName);
        customerPage.addBtnC().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), SAME_NAME_WARNING_ENTITY_GROUP_MESSAGE);
        Assert.assertNotNull(customerPage.addEntityGroupView());
        Assert.assertTrue(customerPage.addEntityGroupView().isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Create customer entity group")
    @Test(priority = 30, groups = "smoke")
    @Description("Go to group documentation page")
    public void documentation() {
        String urlPath = "/docs/pe/user-guide/groups/";

        sideBarMenuView.goToCustomerGroups();
        customerPage.setEntityGroupName();
        customerPage.detailsBtn(customerPage.getEntityGroupName()).click();
        customerPage.goToHelpEntityGroupPage();

        Assert.assertTrue(urlContains(urlPath));
    }
}