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

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_GROUP_NAME_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.SAME_NAME_WARNING_ENTITY_GROUP_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultEntityGroupPrototype;

public class CustomerAddToGroupTest extends AbstractDriverBaseTest {
    private SideBarMenuViewHelper sideBarMenuView;
    private CustomerPageHelper customerPage;
    private String title;
    private String name;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @AfterMethod()
    public void delete() {
        testRestClient.deleteCustomer(getCustomerByName(title).getId());
        if (name != null) {
            testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, name).getId());
            name = null;
        }
    }

    @Epic("Customers smoke tests")
    @Feature("Add customer to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Add customer specifying to group")
    public void addGroup() {
        name = ENTITY_NAME + random() + '1';
        title = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(title));
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(name, EntityType.CUSTOMER));

        sideBarMenuView.goToCustomerGroups();
        customerPage.entity("All").click();
        customerPage.checkBox(title).click();
        customerPage.addToGroupBtn().click();
        jsClick(customerPage.selectGroupViewExistField());
        customerPage.entityFromDropDown(name).click();
        customerPage.selectGroupViewSubmitBtn().click();
        sideBarMenuView.customersBtn().click();
        sideBarMenuView.goToCustomerGroups();
        customerPage.entity(name).click();

        Assert.assertNotNull(customerPage.entity(title));
        Assert.assertTrue(customerPage.entity(title).isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Add customer to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Add customer specifying to group without select group")
    public void addGroupWithoutSelect() {
        title = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(title));

        sideBarMenuView.goToCustomerGroups();
        customerPage.entity("All").click();
        customerPage.checkBox(title).click();
        customerPage.addToGroupBtn().click();

        Assert.assertFalse(customerPage.selectGroupViewSubmitBtnVisible().isEnabled());
    }

    @Epic("Customers smoke tests")
    @Feature("Add customer to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Add customer's group specifying the name (text/numbers /special characters)")
    public void createNewEntityGroup() {
        title = ENTITY_NAME + random();
        String groupName = title + '1';
        testRestClient.postCustomer(defaultCustomerPrototype(title));

        sideBarMenuView.goToCustomerGroups();
        customerPage.entity("All").click();
        customerPage.checkBox(title).click();
        customerPage.addToGroupBtn().click();
        customerPage.selectGroupViewNewGroupRadioBtn().click();
        customerPage.enterText(customerPage.selectGroupViewNewGroupField(), groupName);
        customerPage.selectGroupViewSubmitBtn().click();
        name = groupName;
        sideBarMenuView.goToCustomerGroups();

        Assert.assertNotNull(customerPage.entity(groupName));
        Assert.assertTrue(customerPage.entity(groupName).isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Add customer to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Add customer's group without the name")
    public void createNewEntityGroupWithoutName() {
        title = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(title));

        sideBarMenuView.goToCustomerGroups();
        customerPage.entity("All").click();
        customerPage.checkBox(title).click();
        customerPage.addToGroupBtn().click();
        customerPage.selectGroupViewNewGroupRadioBtn().click();

        Assert.assertFalse(customerPage.selectGroupViewSubmitBtnVisible().isEnabled());
    }

    @Epic("Customers smoke tests")
    @Feature("Add customer to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Create customer's group only with spase in name")
    public void createNewEntityGroupWithSpace() {
        title = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(title));

        sideBarMenuView.goToCustomerGroups();
        customerPage.entity("All").click();
        customerPage.checkBox(title).click();
        customerPage.addToGroupBtn().click();
        customerPage.selectGroupViewNewGroupRadioBtn().click();
        customerPage.enterText(customerPage.selectGroupViewNewGroupField(), " ");
        customerPage.selectGroupViewSubmitBtn().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), EMPTY_GROUP_NAME_MESSAGE);
        Assert.assertNotNull(customerPage.addToEntityGroupView());
        Assert.assertTrue(customerPage.addToEntityGroupView().isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Add customer to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Create a customer's group with the same name")
    public void addGroupWithSameName() {
        title = ENTITY_NAME + random();
        name = ENTITY_NAME + random() + '1';
        testRestClient.postCustomer(defaultCustomerPrototype(title));
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(name, EntityType.CUSTOMER));

        sideBarMenuView.goToCustomerGroups();
        customerPage.entity("All").click();
        customerPage.checkBox(title).click();
        customerPage.addToGroupBtn().click();
        customerPage.selectGroupViewNewGroupRadioBtn().click();
        customerPage.enterText(customerPage.selectGroupViewNewGroupField(), name);
        customerPage.selectGroupViewSubmitBtn().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), SAME_NAME_WARNING_ENTITY_GROUP_MESSAGE);
        Assert.assertNotNull(customerPage.addToEntityGroupView());
        Assert.assertTrue(customerPage.addToEntityGroupView().isDisplayed());
    }
}