/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;

public class SortByNameTest extends AbstractDriverBaseTest {
    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;
    private String customerName;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        if (customerName != null) {
            testRestClient.deleteCustomer(getCustomerByName(customerName).getId());
            customerName = null;
        }
    }

    @Epic("Customers smoke tests")
    @Feature("Sort customers by name")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort customers 'UP'")
    public void specialCharacterUp(String title) {
        testRestClient.postCustomer(defaultCustomerPrototype(title));
        this.customerName = title;

        sideBarMenuView.customerBtn().click();
        customerPage.sortByTitleBtn().click();
        customerPage.setCustomerName();

        Assert.assertEquals(customerPage.getCustomerName(), title);
    }

    @Epic("Customers smoke tests")
    @Feature("Sort customers by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort customers 'UP'")
    public void allSortUp(String customer, String customerSymbol, String customerNumber) {
        testRestClient.postCustomer(defaultCustomerPrototype(customerSymbol));
        testRestClient.postCustomer(defaultCustomerPrototype(customer));
        testRestClient.postCustomer(defaultCustomerPrototype(customerNumber));

        sideBarMenuView.customerBtn().click();
        customerPage.sortByTitleBtn().click();
        customerPage.setCustomerName(0);
        String firstCustomer = customerPage.getCustomerName();
        customerPage.setCustomerName(1);
        String secondCustomer = customerPage.getCustomerName();
        customerPage.setCustomerName(2);
        String thirdCustomer = customerPage.getCustomerName();

        testRestClient.deleteCustomer(getCustomerByName(customer).getId());
        testRestClient.deleteCustomer(getCustomerByName(customerNumber).getId());
        testRestClient.deleteCustomer(getCustomerByName(customerSymbol).getId());

        Assert.assertEquals(firstCustomer, customerSymbol);
        Assert.assertEquals(secondCustomer, customerNumber);
        Assert.assertEquals(thirdCustomer, customer);
    }

    @Epic("Customers smoke tests")
    @Feature("Sort customers by name")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort customers 'DOWN'")
    public void specialCharacterDown(String title) {
        testRestClient.postCustomer(defaultCustomerPrototype(title));
        customerName = title;

        sideBarMenuView.customerBtn().click();
        customerPage.sortByNameDown();
        customerPage.setCustomerName(customerPage.allEntity().size() - 1);

        Assert.assertEquals(customerPage.getCustomerName(), title);
    }

    @Epic("Customers smoke tests")
    @Feature("Sort customers by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort customers 'DOWN'")
    public void allSortDown(String customer, String customerSymbol, String customerNumber) {
        testRestClient.postCustomer(defaultCustomerPrototype(customerSymbol));
        testRestClient.postCustomer(defaultCustomerPrototype(customer));
        testRestClient.postCustomer(defaultCustomerPrototype(customerNumber));

        sideBarMenuView.customerBtn().click();
        int lastIndex = customerPage.allEntity().size() - 1;
        customerPage.sortByNameDown();
        customerPage.setCustomerName(lastIndex);
        String firstCustomer = customerPage.getCustomerName();
        customerPage.setCustomerName(lastIndex - 1);
        String secondCustomer = customerPage.getCustomerName();
        customerPage.setCustomerName(lastIndex - 2);
        String thirdCustomer = customerPage.getCustomerName();

        testRestClient.deleteCustomer(getCustomerByName(customer).getId());
        testRestClient.deleteCustomer(getCustomerByName(customerNumber).getId());
        testRestClient.deleteCustomer(getCustomerByName(customerSymbol).getId());

        Assert.assertEquals(firstCustomer, customerSymbol);
        Assert.assertEquals(secondCustomer, customerNumber);
        Assert.assertEquals(thirdCustomer, customer);
    }
}
