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
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultEntityGroupPrototype;

public class CustomerRemoveFromGroupTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private CustomerPageHelper customerPage;
    private String title;
    private String groupName;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        testRestClient.deleteCustomer(getCustomerByName(title).getId());
        if (groupName != null) {
            testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, groupName).getId());
            groupName = null;
        }
    }

    @Epic("Customers smoke tests")
    @Feature("Remove customer from group")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove the customer from group")
    public void removeFromGroup() {
        String groupName = "group" + random();
        title = ENTITY_NAME + random();
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(groupName, EntityType.CUSTOMER));
        testRestClient.postCustomer(defaultCustomerPrototype(title), getEntityGroupByName(EntityType.CUSTOMER, groupName).getId());
        this.groupName = groupName;

        sideBarMenuView.goToCustomerGroups();
        customerPage.entity(this.groupName).click();
        customerPage.checkBox(title).click();
        customerPage.removeFromGroupBtn().click();
        customerPage.warningPopUpYesBtn().click();

        Assert.assertTrue(customerPage.elementIsNotPresent(customerPage.getEntity(title)));
    }

    @Epic("Customers smoke tests")
    @Feature("Remove customer from group")
    @Test(priority = 10, groups = "smoke")
    @Description("Cancel remove the customer from group")
    public void cancelRemoveFromGroup() {
        String groupName = "group" + random();
        title = ENTITY_NAME + random();
        testRestClient.postEntityGroup(defaultEntityGroupPrototype(groupName, EntityType.CUSTOMER));
        testRestClient.postCustomer(defaultCustomerPrototype(title), getEntityGroupByName(EntityType.CUSTOMER, groupName).getId());
        this.groupName = groupName;

        sideBarMenuView.goToCustomerGroups();
        customerPage.entity(groupName).click();
        customerPage.checkBox(title).click();
        customerPage.removeFromGroupBtn().click();
        customerPage.warningPopUpNoBtn().click();

        Assert.assertTrue(customerPage.elementIsNotPresent(customerPage.getConfirmDialog()));
        Assert.assertNotNull(customerPage.entity(title));
        Assert.assertTrue(customerPage.entity(title).isDisplayed());
    }
}