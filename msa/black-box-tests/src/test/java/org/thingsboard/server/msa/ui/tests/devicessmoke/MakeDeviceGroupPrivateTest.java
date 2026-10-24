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
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.PUBLIC_CUSTOMER_NAME;

@Feature("Make device group private")
public class MakeDeviceGroupPrivateTest extends AbstractDeviceTest {

    private String deviceGroupName;

    @AfterClass
    public void deletePublicCustomer() {
        deleteCustomerByName(PUBLIC_CUSTOMER_NAME);
    }

    @BeforeMethod
    public void createPublicDeviceGroup() {
        EntityGroup entityGroup = testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(ENTITY_NAME + random(), EntityType.DEVICE));
        testRestClient.setEntityGroupPublic(entityGroup.getId());
        deviceGroupName = entityGroup.getName();
    }

    @AfterMethod
    public void delete() {
        deleteEntityGroupByName(EntityType.DEVICE, deviceGroupName);
    }

    @Test(groups = "smoke")
    @Description("Make device group private by right side btn")
    public void makeDeviceGroupPrivateByRightSideBtn() {
        sideBarMenuView.goToDeviceGroups();
        devicePage.makeDeviceGroupPrivateByRightSideBtn(deviceGroupName);

        assertIsDisplayed(devicePage.deviceIsPrivateCheckbox(deviceGroupName));
    }

    @Test(groups = "smoke")
    @Description("Make device group public by btn on details tab")
    public void makeDeviceGroupPrivateFromDetailsTab() {
        sideBarMenuView.goToDeviceGroups();
        devicePage.detailsBtn(deviceGroupName).click();
        devicePage.makeDeviceGroupPrivateFromDetailsTab();
        devicePage.closeDetailsViewBtn().click();

        assertIsDisplayed(devicePage.deviceIsPrivateCheckbox(deviceGroupName));
    }
}
