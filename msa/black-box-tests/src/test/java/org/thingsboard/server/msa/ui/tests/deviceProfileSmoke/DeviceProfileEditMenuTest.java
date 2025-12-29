/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.openqa.selenium.Keys;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_DEVICE_PROFILE_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

public class DeviceProfileEditMenuTest extends AbstractDriverBaseTest {

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
            testRestClient.deleteDeviceProfile(getDeviceProfileByName(name).getId());
            name = null;
        }
    }

    @Epic("Device profile smoke tests")
    @Feature("Edit device profile")
    @Test(priority = 10, groups = "smoke")
    @Description("Change name by edit menu")
    public void changeName() {
        String name = ENTITY_NAME + random();
        String newName = "Changed" + getRandomNumber();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));
        this.name = name;

        sideBarMenuView.openDeviceProfiles();
        profilesPage.entity(name).click();
        profilesPage.setHeaderName();
        String titleBefore = profilesPage.getHeaderName();
        jsClick(profilesPage.editPencilBtn());
        profilesPage.changeNameEditMenu(newName);
        profilesPage.doneBtnEditView().click();
        this.name = newName;
        profilesPage.setHeaderName();
        String titleAfter = profilesPage.getHeaderName();

        Assert.assertNotEquals(titleBefore, titleAfter);
        Assert.assertEquals(titleAfter, newName);
    }

    @Epic("Device profile smoke tests")
    @Feature("Edit device profile")
    @Test(priority = 10, groups = "smoke")
    @Description("Delete name and save")
    public void deleteName() {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));
        this.name = name;

        sideBarMenuView.openDeviceProfiles();
        profilesPage.entity(name).click();
        jsClick(profilesPage.editPencilBtn());
        profilesPage.changeNameEditMenu("");

        Assert.assertFalse(profilesPage.doneBtnEditViewVisible().isEnabled());
    }

    @Epic("Device profile smoke tests")
    @Feature("Edit device profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Save only with space in name")
    public void saveWithOnlySpaceInName() {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));
        this.name = name;

        sideBarMenuView.openDeviceProfiles();
        profilesPage.entity(name).click();
        jsClick(profilesPage.editPencilBtn());
        profilesPage.changeNameEditMenu(Keys.SPACE);
        profilesPage.doneBtnEditView().click();

        Assert.assertNotNull(profilesPage.warningMessage());
        Assert.assertTrue(profilesPage.warningMessage().isDisplayed());
        Assert.assertEquals(profilesPage.warningMessage().getText(), EMPTY_DEVICE_PROFILE_MESSAGE);
    }

    @Epic("Device profile smoke tests")
    @Feature("Edit device profile")
    @Test(priority = 30, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "editMenuDescription")
    @Description("Write the description and save the changes/Change the description and save the changes/Delete the description and save the changes")
    public void editDescription(String description, String newDescription, String finalDescription) {
        String name = ENTITY_NAME + random();
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name, description));
        this.name = name;

        sideBarMenuView.openDeviceProfiles();
        profilesPage.entity(name).click();
        jsClick(profilesPage.editPencilBtn());
        profilesPage.profileViewDescriptionField().sendKeys(newDescription);
        profilesPage.doneBtnEditView().click();
        profilesPage.setDescription();

        Assert.assertEquals(profilesPage.getDescription(), finalDescription);
    }
}
