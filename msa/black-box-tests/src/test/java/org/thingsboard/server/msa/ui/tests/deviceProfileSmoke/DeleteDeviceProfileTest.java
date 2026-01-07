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
package org.thingsboard.server.msa.ui.tests.deviceProfileSmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

public class DeleteDeviceProfileTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        profilesPage = new ProfilesPageHelper(driver);
    }

    @Epic("Device profile smoke tests")
    @Feature("Delete one device profile")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove the device profile by clicking on the trash icon in the right side of device profile")
    public void removeDeviceProfile() {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.deleteBtn(name).click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.assertEntityIsNotPresent(name));
    }

    @Epic("Device profile smoke tests")
    @Feature("Delete one device profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the device profile by clicking on the 'Delete device profile' btn in the entity view")
    public void removeDeviceProfileFromView() {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.entity(name).click();
        profilesPage.deviceProfileViewDeleteBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.assertEntityIsNotPresent(name));
    }

    @Epic("Device profile smoke tests")
    @Feature("Delete one device profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove device profile by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedDeviceProfile() {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.checkBox(name).click();
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.assertEntityIsNotPresent(name));
    }

    @Epic("Device profile smoke tests")
    @Feature("Delete one device profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the default device profile by clicking on the trash icon in the right side of device profile")
    public void removeDefaultDeviceProfile() {
        sideBarMenuView.openDeviceProfiles();

        Assert.assertFalse(profilesPage.deleteBtn("default").isEnabled());
    }

    @Epic("Device profile smoke tests")
    @Feature("Delete one device profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the Default device profile by clicking on the 'Delete device profile' btn in the entity view")
    public void removeDefaultDeviceProfileFromView() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.entity("default").click();

        Assert.assertTrue(profilesPage.deleteDeviceProfileFromViewBtnIsNotDisplayed());
    }

    @Epic("Device profile smoke tests")
    @Feature("Delete one device profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the default device profile by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedDefaultDeviceProfile() {
        sideBarMenuView.openDeviceProfiles();

        Assert.assertNotNull(profilesPage.presentCheckBox("default"));
        Assert.assertFalse(profilesPage.presentCheckBox("default").isDisplayed());
    }
    @Epic("Device profile smoke tests")
    @Feature("Delete one device profile")
    @Test(priority = 30, groups = "smoke")
    @Description("Remove the device profile by clicking on the trash icon in the right side of device profile without refresh")
    public void removeDeviceProfileWithoutRefresh() {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.deleteBtn(name).click();
        profilesPage.warningPopUpYesBtn().click();

        Assert.assertTrue(profilesPage.assertEntityIsNotPresent(name));
    }
}
