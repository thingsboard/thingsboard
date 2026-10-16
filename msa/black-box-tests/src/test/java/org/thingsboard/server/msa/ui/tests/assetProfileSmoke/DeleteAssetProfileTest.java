// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.tests.assetProfileSmoke;

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

public class DeleteAssetProfileTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        profilesPage = new ProfilesPageHelper(driver);
    }

    @Epic("Asset profiles smoke")
    @Feature("Delete one asset profile")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove the asset profile by clicking on the trash icon in the right side of asset profile")
    public void removeAssetProfile() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));

        sideBarMenuView.openAssetProfiles();
        profilesPage.deleteBtn(name).click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.assertEntityIsNotPresent(name));
    }

    @Epic("Asset profiles smoke")
    @Feature("Delete one asset profile")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove the asset profile by clicking on the 'Delete asset profile' btn in the entity view")
    public void removeAssetProfileFromView() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));

        sideBarMenuView.openAssetProfiles();
        profilesPage.entity(name).click();
        profilesPage.assetProfileViewDeleteBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.assertEntityIsNotPresent(name));
    }

    @Epic("Asset profiles smoke")
    @Feature("Delete one asset profile")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove asset profile by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedAssetProfile() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));

        sideBarMenuView.openAssetProfiles();
        profilesPage.checkBox(name).click();
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.assertEntityIsNotPresent(name));
    }

    @Epic("Asset profiles smoke")
    @Feature("Delete one asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the default asset profile by clicking on the trash icon in the right side of asset profile")
    public void removeDefaultAssetProfile() {
        sideBarMenuView.openAssetProfiles();

        Assert.assertFalse(profilesPage.deleteBtn("default").isEnabled());
    }

    @Epic("Asset profiles smoke")
    @Feature("Delete one asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the Default asset profile by clicking on the 'Delete asset profile' btn in the entity view")
    public void removeDefaultAssetProfileFromView() {
        sideBarMenuView.openAssetProfiles();
        profilesPage.entity("default").click();

        Assert.assertTrue(profilesPage.deleteAssetProfileFromViewBtnIsNotDisplayed());
    }

    @Epic("Asset profiles smoke")
    @Feature("Delete one asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the default asset profile by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedDefaultAssetProfile() {
        sideBarMenuView.openAssetProfiles();

        Assert.assertNotNull(profilesPage.presentCheckBox("default"));
        Assert.assertFalse(profilesPage.presentCheckBox("default").isDisplayed());
    }

    @Epic("Asset profiles smoke")
    @Feature("Delete one asset profile")
    @Test(priority = 30, groups = "smoke")
    @Description("Remove the asset profile by clicking on the trash icon in the right side of asset profile without refresh")
    public void removeAssetProfileWithoutRefresh() {
        String name = ENTITY_NAME + random();
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));

        sideBarMenuView.openAssetProfiles();
        profilesPage.deleteBtn(name).click();
        profilesPage.warningPopUpYesBtn().click();

        Assert.assertTrue(profilesPage.assertEntityIsNotPresent(name));
    }
}
