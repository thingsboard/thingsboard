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

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;

public class DeleteDeviceProfileTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;

    @BeforeMethod
    public void login() {
        openLocalhost();
        new LoginPageHelper(driver).authorizationTenant();
        testRestClient.login(TENANT_EMAIL, TENANT_PASSWORD);
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        profilesPage = new ProfilesPageHelper(driver);
    }

    @Test
    public void removeDeviceProfile() {
        String name = ENTITY_NAME;
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.deleteBtn(name).click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.entityIsNotPresent(name));
    }

    @Test
    public void removeDeviceProfileFromView() {
        String name = ENTITY_NAME;
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.entity(name).click();
        profilesPage.deviceProfileViewDeleteBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.entityIsNotPresent(name));
    }

    @Test
    public void removeSelectedDeviceProfile() {
        String name = ENTITY_NAME;
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.checkBox(name).click();
        profilesPage.deleteSelectedBtn().click();
        profilesPage.warningPopUpYesBtn().click();
        profilesPage.refreshBtn();

        Assert.assertTrue(profilesPage.entityIsNotPresent(name));
    }

    @Test
    public void removeDefaultDeviceProfile() {
        sideBarMenuView.openDeviceProfiles();

        Assert.assertFalse(profilesPage.deleteBtn("default").isEnabled());
    }

    @Test
    public void removeDefaultDeviceProfileFromView() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.entity("default").click();

        Assert.assertTrue(profilesPage.deleteDeviceProfileFromViewBtnIsNotDisplayed());
    }

    @Test
    public void removeSelectedDefaultDeviceProfile() {
        sideBarMenuView.openDeviceProfiles();

        Assert.assertNotNull(profilesPage.presentCheckBox("default"));
        Assert.assertFalse(profilesPage.presentCheckBox("default").isDisplayed());
    }

    @Test
    public void removeDeviceProfileWithoutRefresh() {
        String name = ENTITY_NAME;
        testRestClient.postDeviceProfile(EntityPrototypes.defaultDeviceProfile(name));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.deleteBtn(name).click();
        profilesPage.warningPopUpYesBtn().click();

        Assert.assertTrue(profilesPage.entityIsNotPresent(name));
    }
}
