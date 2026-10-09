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

public class CustomerMoveToGroupTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private CustomerPageHelper customerPage;
    private final String title = ENTITY_NAME + random();
    private String groupName1;
    private String groupName2;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        testRestClient.deleteCustomer(getCustomerByName(title).getId());
        if (groupName1 != null) {
            testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, groupName1).getId());
            groupName1 = null;
        }
        if (groupName2 != null) {
            testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, groupName2).getId());
            groupName2 = null;
        }
    }

    @Epic("Customers smoke tests")
    @Feature("Move customer from group to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Move customer to group")
    public void addGroup() {
        String groupName1 = "group1" + random();
        String groupName2 = "group2" + random();
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(groupName1, EntityType.CUSTOMER));
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(groupName2, EntityType.CUSTOMER));
        testRestClient.postCustomer(defaultCustomerPrototype(title), getEntityGroupByName(EntityType.CUSTOMER, groupName1).getId());
        this.groupName1 = groupName1;
        this.groupName2 = groupName2;

        sideBarMenuView.goToCustomerGroups();
        customerPage.entity(groupName1).click();
        customerPage.checkBox(title).click();
        customerPage.moveToGroupBtn().click();
        jsClick(customerPage.selectGroupViewExistField());
        customerPage.entityFromDropDown(groupName2).click();
        customerPage.selectGroupViewSubmitBtn().click();
        sideBarMenuView.goToCustomerGroups();
        customerPage.entity(groupName2).click();

        Assert.assertNotNull(customerPage.entity(title));
        Assert.assertTrue(customerPage.entity(title).isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Move customer from group to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Move customer to group without select")
    public void moveToGroupWithoutSelect() {
        String groupName = "group" + random();
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(groupName, EntityType.CUSTOMER));
        testRestClient.postCustomer(defaultCustomerPrototype(title), getEntityGroupByName(EntityType.CUSTOMER, groupName).getId());
        groupName1 = groupName;

        sideBarMenuView.goToCustomerGroups();
        customerPage.entity(groupName1).click();
        customerPage.checkBox(title).click();
        customerPage.moveToGroupBtn().click();

        Assert.assertFalse(customerPage.selectGroupViewSubmitBtnVisible().isEnabled());
    }

    @Epic("Customers smoke tests")
    @Feature("Move customer from group to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Create new customer group")
    public void createNewEntityGroup() {
        String groupName = "group1" + random();
        String newGroupName = "group2" + random();
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(groupName, EntityType.CUSTOMER));
        testRestClient.postCustomer(defaultCustomerPrototype(title), getEntityGroupByName(EntityType.CUSTOMER, groupName).getId());
        groupName1 = groupName;

        sideBarMenuView.goToCustomerGroups();
        customerPage.entity(groupName1).click();
        customerPage.checkBox(title).click();
        customerPage.moveToGroupBtn().click();
        customerPage.selectGroupViewNewGroupRadioBtn().click();
        customerPage.enterText(customerPage.selectGroupViewNewGroupField(), newGroupName);
        customerPage.selectGroupViewSubmitBtn().click();
        groupName2 = newGroupName;
        sideBarMenuView.goToCustomerGroups();

        Assert.assertNotNull(customerPage.entity(newGroupName));
        Assert.assertTrue(customerPage.entity(newGroupName).isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Move customer from group to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Add customer's group without the name")
    public void createNewEntityGroupWithoutName() {
        String groupName = "group" + random();
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(groupName, EntityType.CUSTOMER));
        testRestClient.postCustomer(defaultCustomerPrototype(title), getEntityGroupByName(EntityType.CUSTOMER, groupName).getId());
        groupName1 = groupName;

        sideBarMenuView.goToCustomerGroups();
        customerPage.entity(groupName).click();
        customerPage.checkBox(title).click();
        customerPage.moveToGroupBtn().click();
        customerPage.selectGroupViewNewGroupRadioBtn().click();

        Assert.assertFalse(customerPage.selectGroupViewSubmitBtnVisible().isEnabled());
    }

    @Epic("Customers smoke tests")
    @Feature("Move customer from group to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Create customer's group only with spase in name")
    public void createNewEntityGroupWithSpace() {
        String groupName = "group" + random();
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(groupName, EntityType.CUSTOMER));
        testRestClient.postCustomer(defaultCustomerPrototype(title), getEntityGroupByName(EntityType.CUSTOMER, groupName).getId());
        groupName1 = groupName;

        sideBarMenuView.goToCustomerGroups();
        customerPage.entity(groupName).click();
        customerPage.checkBox(title).click();
        customerPage.moveToGroupBtn().click();
        customerPage.selectGroupViewNewGroupRadioBtn().click();
        customerPage.selectGroupViewNewGroupField().sendKeys(" ");
        customerPage.selectGroupViewSubmitBtn().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), EMPTY_GROUP_NAME_MESSAGE);
        Assert.assertNotNull(customerPage.addToEntityGroupView());
        Assert.assertTrue(customerPage.addToEntityGroupView().isDisplayed());
    }

    @Epic("Customers smoke tests")
    @Feature("Move customer from group to group")
    @Test(priority = 10, groups = "smoke")
    @Description("Create a customer's group with the same name")
    public void addGroupWithSameName() {
        String groupName = "group" + random();
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(groupName, EntityType.CUSTOMER));
        testRestClient.postCustomer(defaultCustomerPrototype(title), getEntityGroupByName(EntityType.CUSTOMER, groupName).getId());
        groupName1 = groupName;

        sideBarMenuView.goToCustomerGroups();
        customerPage.entity(groupName).click();
        customerPage.checkBox(title).click();
        customerPage.moveToGroupBtn().click();
        customerPage.selectGroupViewNewGroupRadioBtn().click();
        customerPage.enterText(customerPage.selectGroupViewNewGroupField(), groupName);
        customerPage.selectGroupViewSubmitBtn().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), SAME_NAME_WARNING_ENTITY_GROUP_MESSAGE);
        Assert.assertNotNull(customerPage.addToEntityGroupView());
        Assert.assertTrue(customerPage.addToEntityGroupView().isDisplayed());
    }
}
