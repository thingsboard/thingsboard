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
package org.thingsboard.server.msa.ui.tests.assetProfileSmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_ASSET_PROFILE_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.SAME_NAME_WARNING_ASSET_PROFILE_MESSAGE;

public class CreateAssetProfileTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;
    private String name;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        profilesPage = new ProfilesPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        if (name != null) {
            testRestClient.deleteAssetProfile(getAssetProfileByName(name).getId());
            name = null;
        }
    }

    @Epic("Asset profiles smoke")
    @Feature("Create asset profile")
    @Test(priority = 10, groups = "smoke")
    @Description("Add asset profile after specifying the name (text/numbers /special characters)")
    public void createAssetProfile() {
        String name = ENTITY_NAME + random();

        sideBarMenuView.openAssetProfiles();
        profilesPage.openCreateAssetProfileView();
        profilesPage.addAssetProfileViewEnterName(name);
        profilesPage.addAssetProfileAddBtn().click();
        this.name = name;
        profilesPage.refreshBtn().click();

        Assert.assertNotNull(profilesPage.entity(name));
        Assert.assertTrue(profilesPage.entity(name).isDisplayed());
    }

    @Epic("Asset profiles smoke")
    @Feature("Create asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Add asset profile after specifying the name with details")
    public void createAssetProfileWithDetails() {
        String name = ENTITY_NAME + random();
        String ruleChain = "Root Rule Chain";
        String mobileDashboard = "Firmware";
        String queue = "Main";
        String description = "Description";

        sideBarMenuView.openAssetProfiles();
        profilesPage.openCreateAssetProfileView();
        profilesPage.addAssetProfileViewEnterName(name);
        profilesPage.addAssetProfileViewChooseRuleChain(ruleChain);
        profilesPage.addAssetProfileViewChooseMobileDashboard(mobileDashboard);
        profilesPage.addAssetsProfileViewChooseQueue(queue);
        profilesPage.addAssetProfileViewEnterDescription(description);
        profilesPage.addAssetProfileAddBtn().click();
        this.name = name;
        profilesPage.refreshBtn().click();
        profilesPage.entity(name).click();
        profilesPage.setName();
        profilesPage.setRuleChain();
        profilesPage.setMobileDashboard();
        profilesPage.setQueue();
        profilesPage.setDescription();

        Assert.assertNotNull(profilesPage.entity(name));
        Assert.assertTrue(profilesPage.entity(name).isDisplayed());
        Assert.assertEquals(name, profilesPage.getName());
        Assert.assertEquals(ruleChain, profilesPage.getRuleChain());
        Assert.assertEquals(mobileDashboard, profilesPage.getMobileDashboard());
        Assert.assertEquals(queue, profilesPage.getQueue());
        Assert.assertEquals(description, profilesPage.getDescription());
    }

    @Epic("Asset profiles smoke")
    @Feature("Create asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Create asset profile with the same name")
    public void createAssetProfileWithSameName() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));
        this.name = name;

        sideBarMenuView.openAssetProfiles();
        profilesPage.openCreateAssetProfileView();
        profilesPage.addAssetProfileViewEnterName(name);
        profilesPage.addAssetProfileAddBtn().click();

        Assert.assertNotNull(profilesPage.warningMessage());
        Assert.assertTrue(profilesPage.warningMessage().isDisplayed());
        Assert.assertEquals(profilesPage.warningMessage().getText(), SAME_NAME_WARNING_ASSET_PROFILE_MESSAGE);
        Assert.assertNotNull(profilesPage.addAssetProfileView());
        Assert.assertTrue(profilesPage.addAssetProfileView().isDisplayed());
    }

    @Epic("Asset profiles smoke")
    @Feature("Create asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Add asset profile without the name")
    public void createAssetProfileWithoutName() {
        sideBarMenuView.openAssetProfiles();
        profilesPage.openCreateAssetProfileView();

        Assert.assertFalse(profilesPage.addBtnV().isEnabled());
    }

    @Epic("Asset profiles smoke")
    @Feature("Create asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Create asset profile only with spase in name")
    public void createAssetProfileWithOnlySpaceInName() {
        sideBarMenuView.openAssetProfiles();
        profilesPage.openCreateAssetProfileView();
        profilesPage.addAssetProfileViewEnterName(" ");
        profilesPage.addAssetProfileAddBtn().click();

        Assert.assertNotNull(profilesPage.warningMessage());
        Assert.assertTrue(profilesPage.warningMessage().isDisplayed());
        Assert.assertEquals(profilesPage.warningMessage().getText(), EMPTY_ASSET_PROFILE_MESSAGE);
        Assert.assertNotNull(profilesPage.addAssetProfileView());
        Assert.assertTrue(profilesPage.addAssetProfileView().isDisplayed());
    }

    @Epic("Asset profiles smoke")
    @Feature("Create asset profile")
    @Test(priority = 30, groups = "smoke")
    @Description("Add asset profile after specifying the name (text/numbers /special characters) without refresh")
    public void createAssetProfileWithoutRefresh() {
        String name = ENTITY_NAME + random();

        sideBarMenuView.openAssetProfiles();
        profilesPage.openCreateAssetProfileView();
        profilesPage.addAssetProfileViewEnterName(name);
        profilesPage.addAssetProfileAddBtn().click();
        this.name = name;

        Assert.assertNotNull(profilesPage.entity(name));
        Assert.assertTrue(profilesPage.entity(name).isDisplayed());
    }

    @Epic("Asset profiles smoke")
    @Feature("Create asset profile")
    @Test(priority = 40, groups = "smoke")
    @Description("Go to asset profile documentation page")
    public void documentation() {
        String urlPath = "docs/user-guide/asset-profiles/";

        sideBarMenuView.openAssetProfiles();
        profilesPage.profileNames().get(0).click();
        profilesPage.goToProfileHelpPage();

        Assert.assertTrue(urlContains(urlPath), "URL not contains " + urlPath);
    }
}
