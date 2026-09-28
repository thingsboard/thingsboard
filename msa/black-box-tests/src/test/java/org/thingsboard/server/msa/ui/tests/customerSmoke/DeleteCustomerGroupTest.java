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
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

public class DeleteCustomerGroupTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private CustomerPageHelper customerPage;
    private RuleChainsPageHelper ruleChainsPage;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        customerPage = new CustomerPageHelper(driver);
        ruleChainsPage = new RuleChainsPageHelper(driver);
    }

    @Epic("Customers smoke tests")
    @Feature("Delete one customer group")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove the customer group by clicking on the trash icon in the right side of refresh")
    public void removeCustomerByRightSideBtn() {
        String customerGroupName = ENTITY_NAME + random();
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerGroupName, EntityType.CUSTOMER));

        sideBarMenuView.goToCustomerGroups();
        String deletedCustomerGroup = customerPage.deleteRuleChainTrash(customerGroupName);
        customerPage.refreshBtn().click();

        Assert.assertTrue(customerPage.assertEntityIsNotPresent(deletedCustomerGroup));
    }

    @Epic("Customers smoke tests")
    @Feature("Delete one customer group")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove customer group by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedCustomer() {
        String customerGroupName = ENTITY_NAME + random();
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerGroupName, EntityType.CUSTOMER));

        sideBarMenuView.goToCustomerGroups();
        String deletedCustomer = customerPage.deleteSelected(customerGroupName);
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.assertEntityIsNotPresent(deletedCustomer));
    }

    @Epic("Customers smoke tests")
    @Feature("Delete one customer group")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the customer group by clicking on the 'Delete customer group' btn in the entity view")
    public void removeFromCustomerView() {
        String customerGroupName = ENTITY_NAME + random();
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerGroupName, EntityType.CUSTOMER));

        sideBarMenuView.goToCustomerGroups();
        customerPage.detailsBtn(customerGroupName).click();
        customerPage.entityGroupViewDeleteBtn().click();
        customerPage.warningPopUpYesBtn().click();
        customerPage.refreshBtn().click();

        Assert.assertTrue(customerPage.assertEntityIsNotPresent(customerGroupName));
    }

    @Epic("Customers smoke tests")
    @Feature("Delete one customer group")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the customer group by clicking on the trash icon in the right side of customer without refresh")
    public void removeCustomerByRightSideBtnWithoutRefresh() {
        String customerGroupName = ENTITY_NAME + random();
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerGroupName, EntityType.CUSTOMER));

        sideBarMenuView.goToCustomerGroups();
        String deletedCustomer = customerPage.deleteRuleChainTrash(customerGroupName);
        customerPage.refreshBtn().click();

        Assert.assertTrue(customerPage.assertEntityIsNotPresent(deletedCustomer));
    }
}
