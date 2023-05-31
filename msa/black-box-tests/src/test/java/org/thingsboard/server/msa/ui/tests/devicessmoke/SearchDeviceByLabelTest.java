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
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

@Feature("Search devices by label")
public class SearchDeviceByLabelTest extends AbstractDeviceTest {

    private Device deviceWithLabel;
    private Device deviceWithLabel1;
    private Device deviceWithLabel2;

    @BeforeClass
    public void create() {
        deviceWithLabel = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Device", "", "Things Board"));
        deviceWithLabel1 = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Device", "", ENTITY_NAME));
        deviceWithLabel2 = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Device", "", getRandomNumber()));
    }

    @AfterClass
    public void delete() {
        deleteDeviceByName(deviceWithLabel.getName());
        deleteDeviceByName(deviceWithLabel1.getName());
        deleteDeviceByName(deviceWithLabel2.getName());
    }

    @Test(dataProviderClass = DataProviderCredential.class, dataProvider = "devicesSearchPath")
    @Description("Search device by first word, by second word, by symbol in the label's name")
    public void searchDeviceByLabel(String namePath) {
        sideBarMenuView.goToDevicesPage();
        devicePage.searchEntity(namePath);

        assertListOfElementContainsText(devicePage.listOfLabelsCells(), namePath);
    }

    @Test
    @Description("Search device by number in the label's name")
    public void searchDeviceByLabelByNumber() {
        sideBarMenuView.goToDevicesPage();
        devicePage.searchEntity(deviceWithLabel2.getLabel());

        assertListOfElementContainsText(devicePage.listOfLabelsCells(), deviceWithLabel2.getLabel());
    }
}
