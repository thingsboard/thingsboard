package org.thingsboard.server.msa.ui.tests.deviceProfileSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;

import static org.thingsboard.server.msa.ui.utils.Const.*;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultDeviceProfile;

public class DeleteSeveralDeviceProfilesTest extends AbstractDriverBaseTest {
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

    @Test(priority = 10, groups = "smoke")
    @Description
    public void canDeleteSeveralDeviceProfilesByTopBtn() {
        String name1 = ENTITY_NAME + "1";
        String name2 = ENTITY_NAME + "2";
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

    @Test(priority = 10, groups = "smoke")
    @Description
    public void selectAllDeviceProfiles() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.selectAllCheckBox().click();
        profilesPage.deleteSelectedBtn().click();

        Assert.assertNotNull(profilesPage.warningPopUpTitle());
        Assert.assertTrue(profilesPage.warningPopUpTitle().isDisplayed());
        Assert.assertTrue(profilesPage.warningPopUpTitle().getText().contains(String.valueOf(profilesPage.markCheckbox().size())));
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeDefaultDeviceProfile() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.selectAllCheckBox().click();

        profilesPage.assertCheckBoxIsNotDisplayed("default");
        Assert.assertFalse(profilesPage.deleteBtn("default").isEnabled());
    }

    @Test(priority = 30, groups = "smoke")
    @Description
    public void deleteSeveralDeviceProfilesByTopBtnWithoutRefresh() {
        String name1 = ENTITY_NAME + "1";
        String name2 = ENTITY_NAME + "2";
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
