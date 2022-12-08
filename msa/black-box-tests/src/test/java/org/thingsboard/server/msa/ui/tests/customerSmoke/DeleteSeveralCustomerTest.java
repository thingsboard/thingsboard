/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultCustomerPrototype;

public class DeleteSeveralCustomerTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelper customerPage;

    @BeforeMethod
    public void login() {
        openLocalhost();
        new LoginPageHelper(driver).authorizationTenant();
        testRestClient.login(TENANT_EMAIL, TENANT_PASSWORD);
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void canDeleteSeveralCustomersByTopBtn() {
        String title1 = ENTITY_NAME + "1";
        String title2 = ENTITY_NAME + "2";
        testRestClient.postCustomer(defaultCustomerPrototype(title1));
        testRestClient.postCustomer(defaultCustomerPrototype(title2));

        sideBarMenuView.customerBtn().click();
        customerPage.clickOnCheckBoxes(2);
        customerPage.deleteSelectedBtn().click();
        customerPage.warningPopUpYesBtn().click();
        customerPage.refreshBtn().click();

        Assert.assertTrue(customerPage.customerIsNotPresent(title1));
        Assert.assertTrue(customerPage.customerIsNotPresent(title2));
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void selectAllCustomers() {
        sideBarMenuView.customerBtn().click();
        customerPage.selectAllCheckBox().click();
        customerPage.deleteSelectedBtn().click();

        Assert.assertNotNull(customerPage.warningPopUpTitle());
        Assert.assertTrue(customerPage.warningPopUpTitle().isDisplayed());
        Assert.assertTrue(customerPage.warningPopUpTitle().getText().contains(String.valueOf(customerPage.markCheckbox().size())));
    }

    @Test(priority = 30, groups = "smoke")
    @Description
    public void deleteSeveralCustomersByTopBtnWithoutRefresh() {
        String title1 = ENTITY_NAME + "1";
        String title2 = ENTITY_NAME + "2";
        testRestClient.postCustomer(defaultCustomerPrototype(title1));
        testRestClient.postCustomer(defaultCustomerPrototype(title2));

        sideBarMenuView.customerBtn().click();
        customerPage.clickOnCheckBoxes(2);
        customerPage.deleteSelectedBtn().click();
        customerPage.warningPopUpYesBtn().click();

        Assert.assertTrue(customerPage.customerIsNotPresent(title1));
        Assert.assertTrue(customerPage.customerIsNotPresent(title2));
    }
}
