/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.msa.ui.tests.devicessmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

@Feature("Search devices by customer")
public class SearchDeviceByCustomerTest extends AbstractDeviceTest {

    private Device deviceWithCustomer;
    private Device deviceWithCustomer1;
    private Device deviceWithCustomer2;
    private Customer customer;
    private Customer customer1;
    private Customer customer2;

    @BeforeClass
    public void create() {
        customer = testRestClient.postCustomer(EntityPrototypes.defaultCustomerPrototype("Things Board" + random()));
        customer1 = testRestClient.postCustomer(EntityPrototypes.defaultCustomerPrototype(ENTITY_NAME + random()));
        customer2 = testRestClient.postCustomer(EntityPrototypes.defaultCustomerPrototype(getRandomNumber()));

        deviceWithCustomer = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Device", customer.getId()));
        deviceWithCustomer1 = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Device", customer1.getId()));
        deviceWithCustomer2 = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Device", customer2.getId()));
    }

    @AfterClass
    public void delete() {
        deleteDeviceByName(deviceWithCustomer.getName());
        deleteDeviceByName(deviceWithCustomer1.getName());
        deleteDeviceByName(deviceWithCustomer2.getName());
        deleteCustomerByName(customer.getName());
        deleteCustomerByName(customer1.getName());
        deleteCustomerByName(customer2.getName());
    }

    @Test(dataProviderClass = DataProviderCredential.class, dataProvider = "devicesSearchPath")
    @Description("Search device by first word, by second word, by symbol in the customer's name")
    public void searchDeviceByCustomer(String namePath) {
        sideBarMenuView.goToDevicesPage();
        devicePage.searchEntity(namePath);

        assertListOfElementContainsText(devicePage.listOfCustomersCells(), namePath);
    }

    @Test
    @Description("Search device by number in the customer's name")
    public void searchDeviceByCustomerByNumber() {
        sideBarMenuView.goToDevicesPage();
        devicePage.searchEntity(customer2.getName());

        assertListOfElementContainsText(devicePage.listOfCustomersCells(), customer2.getName());
    }
}
