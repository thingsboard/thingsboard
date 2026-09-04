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
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;

public class DeleteCustomerTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;
    private RuleChainsPageHelper ruleChainsPage;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
        ruleChainsPage = new RuleChainsPageHelper(driver);
    }

    @Epic("Customers smoke tests")
    @Feature("Delete customer")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove the customer by clicking on the trash icon in the right side of refresh")
    public void removeCustomerByRightSideBtn() {
        String customer = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customer));

        sideBarMenuView.customerBtn().click();
        String deletedCustomer = customerPage.deleteRuleChainTrash(customer);
        customerPage.refreshBtn().click();

        Assert.assertTrue(customerPage.assertEntityIsNotPresent(deletedCustomer));
    }

    @Epic("Customers smoke tests")
    @Feature("Delete customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove customer by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedCustomer() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));

        sideBarMenuView.customerBtn().click();
        String deletedCustomer = customerPage.deleteSelected(customerName);
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.assertEntityIsNotPresent(deletedCustomer));
    }

    @Epic("Customers smoke tests")
    @Feature("Delete customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the customer by clicking on the 'Delete customer' btn in the entity view")
    public void removeFromCustomerView() {
        String customerName = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customerName));

        sideBarMenuView.customerBtn().click();
        customerPage.entity(customerName).click();
        jsClick(customerPage.customerViewDeleteBtn());
        customerPage.warningPopUpYesBtn().click();
        jsClick(customerPage.refreshBtn());

        Assert.assertTrue(customerPage.assertEntityIsNotPresent(customerName));
    }

    @Epic("Customers smoke tests")
    @Feature("Delete customer")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the customer by clicking on the trash icon in the right side of customer without refresh")
    public void removeCustomerByRightSideBtnWithoutRefresh() {
        String customer = ENTITY_NAME + random();
        testRestClient.postCustomer(defaultCustomerPrototype(customer));

        sideBarMenuView.customerBtn().click();
        String deletedCustomer = customerPage.deleteRuleChainTrash(customer);
        customerPage.refreshBtn().click();

        Assert.assertTrue(customerPage.assertEntityIsNotPresent(deletedCustomer));
    }
}
