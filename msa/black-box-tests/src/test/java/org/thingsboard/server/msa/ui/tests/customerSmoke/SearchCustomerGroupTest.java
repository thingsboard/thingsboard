// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
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

public class SearchCustomerGroupTest extends AbstractDriverBaseTest {

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
    @Feature("Search customer group")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class,
            dataProvider = "customerGroupNameForSearchByFirstAndSecondWord")
    @Description("Search customer group by first/second word in the name")
    public void searchFirstSecondWord(String namePath) {
        customerGroupName = "Entity Group " + RandomStringUtils.randomAlphanumeric(7);
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerGroupName, EntityType.CUSTOMER));

        sideBarMenuView.goToCustomerGroups();
        customerPage.searchEntity(namePath);

        customerPage.entityGroups().forEach(x -> Assert.assertTrue(x.getText().contains(namePath)));
    }

    @Epic("Customers smoke tests")
    @Feature("Search customer group")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSearchBySymbolAndNumber")
    @Description("Search customer by symbol/number in the name")
    public void searchNumberAndSymbol(String name, String namePath) {
        customerGroupName = name;
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerGroupName, EntityType.CUSTOMER));

        sideBarMenuView.goToCustomerGroups();
        customerPage.searchEntity(namePath);
        customerPage.setEntityGroupName();

        customerPage.entityGroups().forEach(x -> Assert.assertTrue(x.getText().contains(namePath)));
    }
}