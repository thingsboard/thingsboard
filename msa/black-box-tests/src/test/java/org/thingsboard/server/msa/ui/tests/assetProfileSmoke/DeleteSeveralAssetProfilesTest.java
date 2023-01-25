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
package org.thingsboard.server.msa.ui.tests.assetProfileSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

public class DeleteSeveralAssetProfilesTest extends AbstractDriverBaseTest {
    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;

    @BeforeMethod
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        profilesPage = new ProfilesPageHelper(driver);
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void canDeleteSeveralAssetProfilesByTopBtn() {
        String name1 = ENTITY_NAME + random() + "1";
        String name2 = ENTITY_NAME + random() + "2";
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name1));
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name2));

        sideBarMenuView.openAssetProfiles();
        profilesPage.clickOnCheckBoxes(2);
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn().click();

        Assert.assertTrue(profilesPage.profileIsNotPresent(name1));
        Assert.assertTrue(profilesPage.profileIsNotPresent(name2));
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void selectAllDAssetProfiles() {
        String name1 = ENTITY_NAME + random() + "1";
        String name2 = ENTITY_NAME + random() +"2";
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name1));
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name2));

        sideBarMenuView.openAssetProfiles();
        profilesPage.selectAllCheckBox().click();
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn().click();

        Assert.assertTrue(profilesPage.profileIsNotPresent(name1));
        Assert.assertTrue(profilesPage.profileIsNotPresent(name2));
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeDefaultAssetProfile() {
        sideBarMenuView.openAssetProfiles();
        profilesPage.selectAllCheckBox().click();

        Assert.assertFalse(profilesPage.checkBoxIsDisplayed("default"));
        Assert.assertFalse(profilesPage.deleteBtn("default").isEnabled());
    }

    @Test(priority = 30, groups = "smoke")
    @Description
    public void deleteSeveralAssetProfilesByTopBtnWithoutRefresh() {
        String name1 = ENTITY_NAME + random() + "1";
        String name2 = ENTITY_NAME + random() + "2";
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name1));
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name2));

        sideBarMenuView.openAssetProfiles();
        profilesPage.clickOnCheckBoxes(2);
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();

        Assert.assertTrue(profilesPage.profileIsNotPresent(name1));
        Assert.assertTrue(profilesPage.profileIsNotPresent(name2));
    }
}
