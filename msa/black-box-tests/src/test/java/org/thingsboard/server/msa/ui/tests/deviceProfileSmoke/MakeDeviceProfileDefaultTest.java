// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.tests.deviceProfileSmoke;

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

public class MakeDeviceProfileDefaultTest extends AbstractDriverBaseTest {
    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        profilesPage = new ProfilesPageHelper(driver);
    }

    @AfterMethod
    public void makeProfileDefault() {
        testRestClient.setDefaultDeviceProfile(getDeviceProfileByName("default").getId());
    }

    @Epic("Device profile smoke tests")
    @Feature("Make device profile default")
    @Test(priority = 10, groups = "smoke")
    @Description("Make device profile default by clicking on the 'Make device profile default'  icon in the right corner")
    public void makeDeviceProfileDefaultByRightCornerBtn() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.setProfileName();
        String profile = profilesPage.getProfileName();
        profilesPage.makeProfileDefaultBtn(profile).click();
        profilesPage.warningPopUpYesBtn().click();

        Assert.assertTrue(profilesPage.defaultCheckbox(profile).isDisplayed());
    }

    @Epic("Device profile smoke tests")
    @Feature("Make device profile default")
    @Test(priority = 10, groups = "smoke")
    @Description("Make device profile default by clicking on the 'Make device profile default' button in the entity view")
    public void makeDeviceProfileDefaultFromView() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.setProfileName();
        String profile = profilesPage.getProfileName();
        profilesPage.entity(profile).click();
        profilesPage.deviceProfileViewMakeDefaultBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.closeEntityViewBtn().click();

        Assert.assertTrue(profilesPage.defaultCheckbox(profile).isDisplayed());
    }
}
