package org.thingsboard.server.msa.ui.tests.deviceProfileSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.ProfilesPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;

import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_IMPORT_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_DEVICE_PROFILE_FILE_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_DEVICE_PROFILE_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_TXT_FILE_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;

public class CreateDeviceProfileImportTest extends AbstractDriverBaseTest {

    private SideBarMenuViewHelper sideBarMenuView;
    private ProfilesPageHelper profilesPage;
    private final String absolutePathToFileImportDeviceProfile = getClass().getClassLoader().getResource(IMPORT_DEVICE_PROFILE_FILE_NAME).getPath();
    private final String absolutePathToFileImportTxt = getClass().getClassLoader().getResource(IMPORT_TXT_FILE_NAME).getPath();
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
    public void importDeviceProfile() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.openImportDeviceProfileView();
        profilesPage.browseFile().sendKeys(absolutePathToFileImportDeviceProfile);
        profilesPage.importBrowseFileBtn().click();
        name = IMPORT_DEVICE_PROFILE_NAME;
        profilesPage.refreshBtn().click();

        Assert.assertNotNull(profilesPage.entity(IMPORT_DEVICE_PROFILE_NAME));
        Assert.assertTrue(profilesPage.entity(IMPORT_DEVICE_PROFILE_NAME).isDisplayed());
    }

    @Test
    public void importTxtFile() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.openImportDeviceProfileView();
        profilesPage.browseFile().sendKeys(absolutePathToFileImportTxt);

        Assert.assertNotNull(profilesPage.importingFile(EMPTY_IMPORT_MESSAGE));
        Assert.assertTrue(profilesPage.importingFile(EMPTY_IMPORT_MESSAGE).isDisplayed());
    }

    @Test(groups = "smoke")
    @Description
    public void addFileTiImportAndRemove() {
        sideBarMenuView.openDeviceProfiles();
        profilesPage.openImportDeviceProfileView();
        profilesPage.browseFile().sendKeys(absolutePathToFileImportDeviceProfile);
        profilesPage.clearImportFileBtn().click();

        Assert.assertNotNull(profilesPage.importingFile(EMPTY_IMPORT_MESSAGE));
        Assert.assertTrue(profilesPage.importingFile(EMPTY_IMPORT_MESSAGE).isDisplayed());
        Assert.assertTrue(profilesPage.entityIsNotPresent(IMPORT_DEVICE_PROFILE_NAME));
    }

    @Test
    public void importDeviceProfileWithSameName() {
        String name = ENTITY_NAME;
        testRestClient.postDeviceProfile()

        sideBarMenuView.openDeviceProfiles();
        profilesPage.openImportDeviceProfileView();
        profilesPage.browseFile().sendKeys(absolutePathToFileImportDeviceProfile);
        profilesPage.importBrowseFileBtn().click();
        this.name = IMPORT_DEVICE_PROFILE_NAME;
        profilesPage.refreshBtn().click();

        Assert.assertNotNull(profilesPage.entity(IMPORT_DEVICE_PROFILE_NAME));
        Assert.assertTrue(profilesPage.entity(IMPORT_DEVICE_PROFILE_NAME).isDisplayed());
    }
}
