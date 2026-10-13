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
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

public class SortCustomerGroupsByNameTest extends AbstractDriverBaseTest {

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
    @Feature("Sort customer groups by name")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort customers 'UP'")
    public void specialCharacterUp(String name) {
        customerGroupName = name;
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerGroupName, EntityType.CUSTOMER));

        sideBarMenuView.goToCustomerGroups();
        customerPage.sortByNameBtn().click();
        customerPage.setEntityGroupName();

        Assert.assertEquals(customerPage.getEntityGroupName(), customerGroupName);
    }

    @Epic("Customers smoke tests")
    @Feature("Sort customer groups by name")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort customers 'DOWN'")
    public void specialCharacterDown(String name) {
        customerGroupName = name;
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerGroupName, EntityType.CUSTOMER));

        sideBarMenuView.goToCustomerGroups();
        customerPage.sortByNameDown();
        customerPage.setEntityGroupName(customerPage.entityGroups().size() - 1);

        Assert.assertEquals(customerPage.getEntityGroupName(), customerGroupName);
    }

    @Epic("Customers smoke tests")
    @Feature("Sort customer groups by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort customers 'UP'")
    public void allSortUp(String customer, String customerSymbol, String customerNumber) {
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerSymbol, EntityType.CUSTOMER));
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customer, EntityType.CUSTOMER));
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerNumber, EntityType.CUSTOMER));

        sideBarMenuView.goToCustomerGroups();
        customerPage.sortByNameBtn().click();
        customerPage.setEntityGroupName(0);
        String firstGroup = customerPage.getEntityGroupName();
        customerPage.setEntityGroupName(1);
        String secondGroup = customerPage.getEntityGroupName();
        customerPage.setEntityGroupName(2);
        String thirdGroup = customerPage.getEntityGroupName();

        testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, customer).getId());
        testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, customerNumber).getId());
        testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, customerSymbol).getId());

        Assert.assertEquals(firstGroup, customerSymbol);
        Assert.assertEquals(secondGroup, customerNumber);
        Assert.assertEquals(thirdGroup, customer);
    }

    @Epic("Customers smoke tests")
    @Feature("Sort customer groups by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort customers 'DOWN'")
    public void allSortDown(String customer, String customerSymbol, String customerNumber) {
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerSymbol, EntityType.CUSTOMER));
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customer, EntityType.CUSTOMER));
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerNumber, EntityType.CUSTOMER));

        sideBarMenuView.goToCustomerGroups();
        int lastIndex = customerPage.entityGroups().size() - 1;
        customerPage.sortByNameDown();
        customerPage.setEntityGroupName(lastIndex);
        String firstGroup = customerPage.getEntityGroupName();
        customerPage.setEntityGroupName(lastIndex - 1);
        String secondGroup = customerPage.getEntityGroupName();
        customerPage.setEntityGroupName(lastIndex - 2);
        String thirdGroup = customerPage.getEntityGroupName();

        testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, customer).getId());
        testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, customerNumber).getId());
        testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, customerSymbol).getId());

        Assert.assertEquals(firstGroup, customerSymbol);
        Assert.assertEquals(secondGroup, customerNumber);
        Assert.assertEquals(thirdGroup, customer);
    }
}
