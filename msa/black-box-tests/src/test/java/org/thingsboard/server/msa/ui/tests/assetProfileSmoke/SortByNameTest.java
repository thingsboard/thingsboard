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
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultAssetProfile;

public class SortByNameTest extends AbstractDriverBaseTest {
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
    @Feature("Sort by name")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort asset profile 'UP'")
    public void specialCharacterUp(String name) {
        testRestClient.postAssetProfile(defaultAssetProfile(name));
        this.name = name;

        sideBarMenuView.openAssetProfiles();
        profilesPage.sortByNameBtn().click();
        profilesPage.setProfileName();

        Assert.assertEquals(profilesPage.getProfileName(), name);
    }

    @Epic("Asset profiles smoke")
    @Feature("Sort by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort asset profile 'UP'")
    public void allSortUp(String assetProfile, String assetProfileSymbol, String assetProfileNumber) {
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfileSymbol));
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfile));
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfileNumber));

        sideBarMenuView.openAssetProfiles();
        profilesPage.sortByNameBtn().click();
        profilesPage.setProfileName(0);
        String firstAssetProfile = profilesPage.getProfileName();
        profilesPage.setProfileName(1);
        String secondAssetProfile = profilesPage.getProfileName();
        profilesPage.setProfileName(2);
        String thirdAssetProfile = profilesPage.getProfileName();

        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfile).getId());
        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfileNumber).getId());
        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfileSymbol).getId());

        Assert.assertEquals(firstAssetProfile, assetProfileSymbol);
        Assert.assertEquals(secondAssetProfile, assetProfileNumber);
        Assert.assertEquals(thirdAssetProfile, assetProfile);
    }

    @Epic("Asset profiles smoke")
    @Feature("Sort by name")
    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description("Sort asset profile 'DAWN'")
    public void specialCharacterDown(String name) {
        testRestClient.postAssetProfile(defaultAssetProfile(name));
        this.name = name;

        sideBarMenuView.openAssetProfiles();
        profilesPage.sortByNameDown();
        profilesPage.setProfileName(profilesPage.allEntity().size() - 1);

        Assert.assertEquals(profilesPage.getProfileName(), name);
    }

    @Epic("Asset profiles smoke")
    @Feature("Sort by name")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description("Sort asset profile 'DOWN'")
    public void allSortDown(String assetProfile, String assetProfileSymbol, String assetProfileNumber) {
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfileSymbol));
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfile));
        testRestClient.postAssetProfile(defaultAssetProfile(assetProfileNumber));

        sideBarMenuView.openAssetProfiles();
        int lastIndex = profilesPage.allEntity().size() - 1;
        profilesPage.sortByNameDown();
        profilesPage.setProfileName(lastIndex);
        String firstAssetProfile = profilesPage.getProfileName();
        profilesPage.setProfileName(lastIndex - 1);
        String secondAssetProfile = profilesPage.getProfileName();
        profilesPage.setProfileName(lastIndex - 2);
        String thirdAssetProfile = profilesPage.getProfileName();

        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfile).getId());
        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfileNumber).getId());
        testRestClient.deleteAssetProfile(getAssetProfileByName(assetProfileSymbol).getId());

        Assert.assertEquals(firstAssetProfile, assetProfileSymbol);
        Assert.assertEquals(secondAssetProfile, assetProfileNumber);
        Assert.assertEquals(thirdAssetProfile, assetProfile);
    }
}
