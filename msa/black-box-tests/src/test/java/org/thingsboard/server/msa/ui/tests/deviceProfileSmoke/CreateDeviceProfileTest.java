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
package org.thingsboard.server.msa.ui.tests.deviceProfileSmoke;

import io.qameta.allure.Description;
import org.openqa.selenium.Keys;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_DEVICE_PROFILE_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.NAME_IS_REQUIRED_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.SAME_NAME_WARNING_DEVICE_PROFILE_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;

public class CreateDeviceProfileTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;
    private String name;


    @BeforeMethod
    public void login() {
        openLocalhost();
        new LoginPageHelper(driver).authorizationTenant();
        testRestClient.login(TENANT_EMAIL, TENANT_PASSWORD);
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        profilesPage = new ProfilesPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        if (name != null) {
            testRestClient.deleteDeviseProfile(getDeviceProfileByName(name).getId());
            name = null;
        }
    }

    @Test
    public void createDeviceProfile() {
        String name = ENTITY_NAME;

        sideBarMenuView.openDeviceProfiles();
        profilesPage.openCreateDeviceProfileView();
        profilesPage.addDeviceProfileNameField().click();
        profilesPage.addDeviceProfileNameField().sendKeys(name);
        profilesPage.addDeviceProfileAddBtn().click();
        this.name = name;
        profilesPage.refreshBtn().click();

        Assert.assertNotNull(profilesPage.entity(name));
        Assert.assertTrue(profilesPage.entity(name).isDisplayed());
    }

    @Test
    public void createDeviceProfileWithDetails() {
        String name = ENTITY_NAME;
        String ruleChain = "Root Rule Chain";
        String mobileDashboard = "Firmware";
        String queue = "Main";
        String description = "Description";

        sideBarMenuView.openDeviceProfiles();
        profilesPage.openCreateDeviceProfileView();
        profilesPage.enterName(name);
        profilesPage.chooseRuleChain(ruleChain);
        profilesPage.chooseMobileDashboard(mobileDashboard);
        profilesPage.chooseQueue(queue);
        profilesPage.enterDescription(description);
        profilesPage.addDeviceProfileAddBtn().click();
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

    @Test
    public void createDeviseProfileWithSameName() {
        String name = ENTITY_NAME;
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));
        this.name = name;

        sideBarMenuView.openDeviceProfiles();
        profilesPage.openCreateDeviceProfileView();
        profilesPage.addDeviceProfileNameField().click();
        profilesPage.addDeviceProfileNameField().sendKeys(name);
        profilesPage.addDeviceProfileAddBtn().click();

        Assert.assertNotNull(profilesPage.warningMessage());
        Assert.assertTrue(profilesPage.warningMessage().isDisplayed());
        Assert.assertEquals(profilesPage.warningMessage().getText(), SAME_NAME_WARNING_DEVICE_PROFILE_MESSAGE);
        Assert.assertNotNull(profilesPage.addDeviceProfileView());
        Assert.assertTrue(profilesPage.addDeviceProfileView().isDisplayed());
    }

    @Test
    public void createDeviceProfileWithoutName() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.openCreateDeviceProfileView();
        profilesPage.addDeviceProfileAddBtn().click();

        Assert.assertNotNull(profilesPage.addDeviceProfileView());
        Assert.assertTrue(profilesPage.addDeviceProfileView().isDisplayed());
        Assert.assertNotNull(profilesPage.errorMessage());
        Assert.assertEquals(profilesPage.errorMessage().getText(), NAME_IS_REQUIRED_MESSAGE);
    }

    @Test
    public void createDeviseProfileWithOnlySpaceInName() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.openCreateDeviceProfileView();
        profilesPage.addDeviceProfileNameField().click();
        profilesPage.addDeviceProfileNameField().sendKeys(Keys.SPACE);
        profilesPage.addDeviceProfileAddBtn().click();

        Assert.assertNotNull(profilesPage.warningMessage());
        Assert.assertTrue(profilesPage.warningMessage().isDisplayed());
        Assert.assertEquals(profilesPage.warningMessage().getText(), EMPTY_DEVICE_PROFILE_MESSAGE);
        Assert.assertNotNull(profilesPage.addDeviceProfileView());
        Assert.assertTrue(profilesPage.addDeviceProfileView().isDisplayed());
    }

    @Test
    public void createDeviceProfileWithoutRefresh() {
        String name = ENTITY_NAME;

        sideBarMenuView.openDeviceProfiles();
        profilesPage.openCreateDeviceProfileView();
        profilesPage.addDeviceProfileNameField().click();
        profilesPage.addDeviceProfileNameField().sendKeys(name);
        profilesPage.addDeviceProfileAddBtn().click();
        this.name = name;

        Assert.assertNotNull(profilesPage.entity(name));
        Assert.assertTrue(profilesPage.entity(name).isDisplayed());
    }

    @Test(groups = "smoke")
    @Description
    public void documentation() {
        String urlPath = "docs/user-guide/device-profiles/";

        sideBarMenuView.openDeviceProfiles();
        profilesPage.allEntity().get(0).click();
        profilesPage.goToHelpPage();

        Assert.assertTrue(urlContains(urlPath));
    }
}

