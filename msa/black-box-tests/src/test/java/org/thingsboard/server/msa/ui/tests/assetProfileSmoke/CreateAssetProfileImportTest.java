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

import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_IMPORT_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_ASSET_PROFILE_FILE_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_ASSET_PROFILE_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_TXT_FILE_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.SAME_NAME_WARNING_ASSET_PROFILE_MESSAGE;

public class CreateAssetProfileImportTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;
    private final String absolutePathToFileImportAssetProfile = getClass().getClassLoader().getResource(IMPORT_ASSET_PROFILE_FILE_NAME).getPath();
    private final String absolutePathToFileImportTxt = getClass().getClassLoader().getResource(IMPORT_TXT_FILE_NAME).getPath();
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
    @Feature("Import asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Import asset profile")
    public void importAssetProfile() {
        sideBarMenuView.openAssetProfiles();
        profilesPage.openImportAssetProfileView();
        profilesPage.browseFile().sendKeys(absolutePathToFileImportAssetProfile);
        profilesPage.importBrowseFileBtn().click();
        name = IMPORT_ASSET_PROFILE_NAME;
        profilesPage.refreshBtn().click();

        Assert.assertNotNull(profilesPage.entity(IMPORT_ASSET_PROFILE_NAME));
        Assert.assertTrue(profilesPage.entity(IMPORT_ASSET_PROFILE_NAME).isDisplayed());
    }

    @Epic("Asset profiles smoke")
    @Feature("Import asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Import txt file")
    public void importTxtFile() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.openImportDeviceProfileView();
        profilesPage.browseFile().sendKeys(absolutePathToFileImportTxt);

        Assert.assertNotNull(profilesPage.importingFile(EMPTY_IMPORT_MESSAGE));
        Assert.assertTrue(profilesPage.importingFile(EMPTY_IMPORT_MESSAGE).isDisplayed());
    }

    @Epic("Asset profiles smoke")
    @Feature("Import asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Drop json file and delete it")
    public void addFileToImportAndRemove() {
        sideBarMenuView.openAssetProfiles();
        profilesPage.openImportAssetProfileView();
        profilesPage.browseFile().sendKeys(absolutePathToFileImportAssetProfile);
        profilesPage.clearImportFileBtn().click();

        Assert.assertNotNull(profilesPage.importingFile(EMPTY_IMPORT_MESSAGE));
        Assert.assertTrue(profilesPage.importingFile(EMPTY_IMPORT_MESSAGE).isDisplayed());
        Assert.assertTrue(profilesPage.assertEntityIsNotPresent(IMPORT_ASSET_PROFILE_NAME));
    }

    @Epic("Asset profiles smoke")
    @Feature("Import asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Import asset profile with same name")
    public void importAssetProfileWithSameName() {
        String name = IMPORT_ASSET_PROFILE_NAME;
        testRestClient.postAssetProfile(EntityPrototypes.defaultAssetProfile(name));
        this.name = name;

        sideBarMenuView.openAssetProfiles();
        profilesPage.openImportAssetProfileView();
        profilesPage.browseFile().sendKeys(absolutePathToFileImportAssetProfile);
        profilesPage.importBrowseFileBtn().click();
        profilesPage.refreshBtn().click();

        Assert.assertNotNull(profilesPage.warningMessage());
        Assert.assertTrue(profilesPage.warningMessage().isDisplayed());
        Assert.assertEquals(profilesPage.warningMessage().getText(), SAME_NAME_WARNING_ASSET_PROFILE_MESSAGE);
    }

    @Epic("Asset profiles smoke")
    @Feature("Import asset profile")
    @Test(priority = 20, groups = "smoke")
    @Description("Import asset profile without refresh")
    public void importAssetProfileWithoutRefresh() {
        sideBarMenuView.openAssetProfiles();
        profilesPage.openImportAssetProfileView();
        profilesPage.browseFile().sendKeys(absolutePathToFileImportAssetProfile);
        profilesPage.importBrowseFileBtn().click();
        name = IMPORT_ASSET_PROFILE_NAME;

        Assert.assertNotNull(profilesPage.entity(IMPORT_ASSET_PROFILE_NAME));
        Assert.assertTrue(profilesPage.entity(IMPORT_ASSET_PROFILE_NAME).isDisplayed());
    }
}
