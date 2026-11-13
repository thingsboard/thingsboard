// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.tests.devicessmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.PUBLIC_CUSTOMER_NAME;

@Feature("Make device group public")
public class MakeDeviceGroupPublicTest extends AbstractDeviceTest {

    private String deviceGroupName;

    @AfterClass
    public void deletePublicCustomer() {
        deleteCustomerByName(PUBLIC_CUSTOMER_NAME);
    }

    @BeforeMethod
    public void create() {
        deviceGroupName = testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(ENTITY_NAME + random(), EntityType.DEVICE)).getName();
    }

    @AfterMethod
    public void delete() {
        deleteEntityGroupByName(EntityType.DEVICE, deviceGroupName);
    }

    @Test(groups = "smoke", priority = 10)
    @Description("Make device public by right side btn")
    public void makeDeviceGroupPublicByRightSideBtn() {
        sideBarMenuView.goToDeviceGroups();
        devicePage.makeDeviceGroupPublicByRightSideBtn(deviceGroupName);

        assertIsDisplayed(devicePage.deviceGroupIsPublicCheckbox(deviceGroupName));
    }

    @Test(groups = "smoke", priority = 10)
    @Description("Make device group public by btn on details tab")
    public void makeDeviceGroupPublicFromDetailsTab() {
        sideBarMenuView.goToDeviceGroups();
        devicePage.detailsBtn(deviceGroupName).click();
        devicePage.makeDeviceGroupPublicFromDetailsTab();
        devicePage.closeDetailsViewBtn().click();

        assertIsDisplayed(devicePage.deviceGroupIsPublicCheckbox(deviceGroupName));
    }
}
