// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.tests.solutiontemplates.smartoffice;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.msa.ui.tests.solutiontemplates.AbstractSolutionTemplateTest;
import org.thingsboard.server.msa.ui.utils.Const;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.TestProperties.getBaseUiUrl;
import static org.thingsboard.server.msa.ui.utils.Const.ALARM_RULES_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.Const.CONNECTIVITY_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.Const.DASHBOARD_GIDE_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.Const.HTTP_API_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.BUILDINGS_ASSET_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.ENERGY_METER_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.ENERGY_METER_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.HVAC_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.HVAC_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.OFFICE_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.OFFICE_ASSET_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.OFFICE_SENSORS_DEVICE_GROUPS;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.REMOTE_OFFICE_R1_EDGE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_OFFICE_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_OFFICE_DASHBOARDS_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SENSOR_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SENSOR_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_DEVICE_PROFILE_SO;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METER_DEVICE;

@Feature("Installation")
@Story("Smart office")
public class SmartOfficeInstallTest extends AbstractSolutionTemplateTest {

    @AfterMethod
    public void delete() {
        testRestClient.deleteSmartOffice();
    }

    @Test
    @Description("Redirect to page with short description (with screenshots) and corresponds to the selected template" +
            " by click on details button from general page")
    public void smartOfficeOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        assertThat(getUrl()).as("Redirected URL equals to details ST URL").isEqualTo(Const.URL + "/solutionTemplates/smart_office");
        assertThat(solutionTemplateDetailsPage.getHeadOfTitleCardName()).as("Title of page").isEqualTo("Smart office");
        assertThat(solutionTemplateDetailsPage.getTitleCardParagraphText()).as("Title of ST card").contains("Smart Office template");
        assertThat(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText()).as("Solution description").contains("Smart Office template");
        solutionTemplateDetailsPage.assertSmartOfficeScreenshotsAreCorrected();
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from general page")
    public void installSmartOfficeFromGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP, SMART_OFFICE_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        assertThat(solutionTemplatesInstalledView.solutionInstructionFirstParagraphSmartOffice().getText())
                .as("First paragraph of solution instruction").contains(SMART_OFFICE_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.solutionInstructionThirdParagraphSmartOffice().getText())
                .as("Third paragraph of solution instruction").contains(SMART_OFFICE_DASHBOARD);
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from details page")
    public void installSmartOfficeFromDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP, SMART_OFFICE_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        assertThat(solutionTemplatesInstalledView.solutionInstructionFirstParagraphSmartOffice().getText())
                .as("First paragraph of solution instruction").contains(SMART_OFFICE_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.solutionInstructionThirdParagraphSmartOffice().getText())
                .as("Third paragraph of solution instruction").contains(SMART_OFFICE_DASHBOARD);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on button the close (x mark)")
    public void closeInstallSmartOfficePopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on bottom button")
    public void closeInstallSmartOfficePopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Check entity installation after solution template installation")
    public void installEntities() {
        testRestClient.postSmartOffice();
        sideBarMenuView.openDeviceProfiles();
        List.of(SMART_SENSOR_DEVICE_PROFILE, HVAC_DEVICE_PROFILE, ENERGY_METER_DEVICE_PROFILE, ENERGY_METER_DEVICE_PROFILE,
                        WATER_METERING_DEVICE_PROFILE_SO)
                .forEach(dp -> assertIsDisplayed(profilesPage.entity(dp)));

        sideBarMenuView.goToDeviceGroups();
        assertIsDisplayed(devicePage.entity(OFFICE_SENSORS_DEVICE_GROUPS));

        devicePage.entity("All").click();
        List.of(SMART_SENSOR_DEVICE, HVAC_DEVICE, ENERGY_METER_DEVICE, WATER_METER_DEVICE)
                .forEach(d -> assertIsDisplayed(devicePage.entity(d)));

        sideBarMenuView.goToAssetGroups();
        assertIsDisplayed(assetPage.entity(BUILDINGS_ASSET_GROUP));

        assetPage.entity("All").click();
        assertIsDisplayed(assetPage.entity(OFFICE_ASSET));

        sideBarMenuView.openAssetProfiles();
        assertIsDisplayed(profilesPage.entity(OFFICE_ASSET_PROFILE));

        sideBarMenuView.goToAllDashboards();
        assertIsDisplayed(dashboardPage.entity(SMART_OFFICE_DASHBOARD));

        sideBarMenuView.goToInstances();
        assertIsDisplayed(instancesPage.entity(REMOTE_OFFICE_R1_EDGE));
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from general solution templates page)")
    public void temperatureSmartOfficeDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.smartOfficeInstallBtn();
        testRestClient.postSmartOffice();
        refreshPage();

        assertInvisibilityOfElement(element);
        assertIsDisplayed(solutionTemplatesHomePage.smartOfficeDeleteBtn());
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from details solution templates page)")
    public void smartOfficeDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postSmartOffice();
        refreshPage();

        assertInvisibilityOfElement(element);
        assertIsDisplayed(solutionTemplateDetailsPage.deleteBtn());
    }

    @Test
    @Description("After install open instruction by click on instruction button (from general solution templates page)")
    public void smartOfficeOpenInstruction() {
        testRestClient.postSmartOffice();
        sideBarMenuView.solutionTemplates().click();
        refreshPage();
        solutionTemplatesHomePage.smartOfficeInstructionBtn().click();

        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark)")
    public void smartOfficeCloseInstruction() {
        testRestClient.postSmartOffice();
        sideBarMenuView.solutionTemplates().click();
        refreshPage();
        solutionTemplatesHomePage.smartOfficeInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button")
    public void smartOfficeCloseInstructionByCloseBtn() {
        testRestClient.postSmartOffice();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from details solution templates page)")
    public void smartOfficeInstructionGoToMainDashboard() {
        testRestClient.postSmartOffice();
        sideBarMenuView.solutionTemplates().click();
        refreshPage();
        solutionTemplatesHomePage.smartOfficeInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboard();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP, SMART_OFFICE_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("After install open instruction by click on instruction button (from details solution templates page)")
    public void smartOfficeDetailsPageOpenInstruction() {
        testRestClient.postSmartOffice();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark) (from details solution templates page)")
    public void smartOfficeDetailsPageCloseInstruction() {
        testRestClient.postSmartOffice();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button (from details solution templates page)")
    public void smartOfficeDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from general solution templates page)")
    public void smartOfficeDetailsPageInstructionGoToMainDashboard() {
        testRestClient.postSmartOffice();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP, SMART_OFFICE_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("Check delete entity after delete solution template")
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteSmartOffice();

        sideBarMenuView.openDeviceProfiles();
        List.of(SMART_SENSOR_DEVICE_PROFILE, HVAC_DEVICE_PROFILE, ENERGY_METER_DEVICE_PROFILE, WATER_METERING_DEVICE_PROFILE_SO)
                .forEach(dp -> profilesPage.assertEntityIsNotPresent(dp));

        sideBarMenuView.goToDeviceGroups();
        devicePage.assertEntityIsNotPresent(OFFICE_SENSORS_DEVICE_GROUPS);

        devicePage.entity("All").click();
        List.of(SMART_SENSOR_DEVICE, HVAC_DEVICE, ENERGY_METER_DEVICE, WATER_METER_DEVICE)
                .forEach(d -> devicePage.assertEntityIsNotPresent(d));

        sideBarMenuView.goToAssetGroups();
        assetPage.assertEntityIsNotPresent(BUILDINGS_ASSET_GROUP);

        assetPage.entity("All").click();
        assetPage.assertEntityIsNotPresent(OFFICE_ASSET);

        sideBarMenuView.openAssetProfiles();
        profilesPage.assertEntityIsNotPresent(OFFICE_ASSET_PROFILE);

        sideBarMenuView.goToAllDashboards();
        dashboardPage.assertEntityIsNotPresent(SMART_OFFICE_DASHBOARD);

        sideBarMenuView.goToInstances();
        instancesPage.assertEntityIsNotPresent(REMOTE_OFFICE_R1_EDGE);
    }

    @Test
    @Description("Check redirect by click on links in instruction")
    public void linksBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartOfficeInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String guide = solutionTemplatesInstalledView.getGuideLink();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String linkAlarmRule = solutionTemplatesInstalledView.getAlarmRuleLink();
        String linkDeviceProfile = solutionTemplatesInstalledView.getDeviceProfileLink();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP, SMART_OFFICE_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_OFFICE_DASHBOARDS_GROUP).getUuidId().toString();

        assertThat(urls).hasSize(1).as("All dashboard links btn redirect to dashboard")
                .contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertThat(guide).as("Dashboard guide link").isEqualTo(DASHBOARD_GIDE_DOCS_URL);
        assertThat(linkHttpApi).as("HTTP API link").isEqualTo(HTTP_API_DOCS_URL);
        assertThat(linkConnectionDevices).as("Connection devices link").isEqualTo(CONNECTIVITY_DOCS_URL);
        assertThat(linkAlarmRule).as("Alarm rule link").isEqualTo(ALARM_RULES_DOCS_URL);
        assertThat(linkDeviceProfile).as("Device profile link").isEqualTo(getBaseUiUrl() + "/profiles/deviceProfiles");
    }
}
