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
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;

public class SearchCustomerTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @Epic("Customers smoke tests")
    @Feature("Search customer")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "customerNameForSearchByFirstAndSecondWord")
    @Description("Search customer by first word in the name/*CHANGE TESTCASE*")
    public void searchFirstWord(String namePath) {
        sideBarMenuView.customerBtn().click();
        customerPage.searchEntity(namePath);

        customerPage.allEntity().forEach(x -> Assert.assertTrue(x.getText().contains(namePath)));
    }

    @Epic("Customers smoke tests")
    @Feature("Search customer")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSearchBySymbolAndNumber")
    @Description("Search customer by symbol in the name/Search customer by number in the name")
    public void searchNumber(String name, String namePath) {
        testRestClient.postCustomer(defaultCustomerPrototype(name));

        sideBarMenuView.customerBtn().click();
        customerPage.searchEntity(namePath);
        customerPage.setCustomerName();
        boolean customerNameContainsPath = customerPage.getCustomerName().contains(namePath);

        testRestClient.deleteCustomer(getCustomerByName(name).getId());

        Assert.assertTrue(customerNameContainsPath);
    }
}
