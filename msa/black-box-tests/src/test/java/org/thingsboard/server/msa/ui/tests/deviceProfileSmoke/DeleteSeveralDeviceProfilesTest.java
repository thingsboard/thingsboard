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

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultDeviceProfile;

public class DeleteSeveralDeviceProfilesTest extends AbstractDriverBaseTest {
    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        profilesPage = new ProfilesPageHelper(driver);
    }

    @Epic("Device profile smoke tests")
    @Feature("Delete several device profiles")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove several device profiles by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void canDeleteSeveralDeviceProfilesByTopBtn() {
        String name1 = ENTITY_NAME + random() + "1";
        String name2 = ENTITY_NAME + random() + "2";
        testRestClient.postDeviceProfile(defaultDeviceProfile(name1));
        testRestClient.postDeviceProfile(defaultDeviceProfile(name2));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.clickOnCheckBoxes(2);
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn().click();

        Assert.assertTrue(profilesPage.profileIsNotPresent(name1));
        Assert.assertTrue(profilesPage.profileIsNotPresent(name2));
    }

    @Epic("Device profile smoke tests")
    @Feature("Delete several device profiles")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove several device profiles by mark all the device profiles on the page by clicking in the topmost checkbox and then clicking on the trash icon in the menu that appears")
    public void selectAllDeviceProfiles() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.selectAllCheckBox().click();
        profilesPage.deleteSelectedBtn().click();

        Assert.assertNotNull(profilesPage.warningPopUpTitle());
        Assert.assertTrue(profilesPage.warningPopUpTitle().isDisplayed());
        Assert.assertTrue(profilesPage.warningPopUpTitle().getText().contains(String.valueOf(profilesPage.markCheckbox().size())));
    }

    @Epic("Device profile smoke tests")
    @Feature("Delete several device profiles")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the default device profile by mark all the device profiles on the page by clicking in the topmost checkbox and then clicking on the trash icon in the menu that appears")
    public void removeDefaultDeviceProfile() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.selectAllCheckBox().click();

        Assert.assertFalse(profilesPage.checkBoxIsDisplayed("default"));
        Assert.assertFalse(profilesPage.deleteBtn("default").isEnabled());
    }

    @Epic("Device profile smoke tests")
    @Feature("Delete several device profiles")
    @Test(priority = 30, groups = "smoke")
    @Description("Remove several device profiles by mark in the checkbox and then click on the trash can icon in the menu that appears at the top without refresh")
    public void deleteSeveralDeviceProfilesByTopBtnWithoutRefresh() {
        String name1 = ENTITY_NAME + random() + "1";
        String name2 = ENTITY_NAME + random() + "2";
        testRestClient.postDeviceProfile(defaultDeviceProfile(name1));
        testRestClient.postDeviceProfile(defaultDeviceProfile(name2));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.clickOnCheckBoxes(2);
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();

        Assert.assertTrue(profilesPage.profileIsNotPresent(name1));
        Assert.assertTrue(profilesPage.profileIsNotPresent(name2));
    }
}
