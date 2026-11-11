// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.tests.solutiontemplates.airqualitymonitoring;

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
import static org.thingsboard.server.msa.ui.utils.Const.HTTP_API_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_MONITORING_ADMINISTRATOR_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_MONITORING_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_MONITORING_DASHBOARD_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_SENSOR_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_SENSOR_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_SENSOR_3_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_SENSOR_4_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AIR_QUALITY_SENSOR_5_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AQI_CITY_ASSET_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AQI_CITY_ASSET_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AQI_CITY_REMOTE_LOCATION_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AQI_CITY_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AQI_SENSOR_DEVICE_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AQI_SENSOR_REMOTE_LOCATION_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.AQI_SENSOR_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.LOS_ANGELES_CA_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.REMOTE_LOCATION_EDGE;

@Feature("Installation")
@Story("Air Quality Monitoring")
public class AirQualityMonitoringInstallTest extends AbstractSolutionTemplateTest {

    @AfterMethod
    public void delete() {
        testRestClient.deleteAirQualityMonitoring();
    }

    @Test
    @Description("Redirect to page with short description (with screenshots) and corresponds to the selected template" +
            " by click on details button from general page")
    public void airQualityMonitoringOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        assertThat(getUrl()).as("Redirected URL equals to details ST URL").isEqualTo(Const.URL + "/solutionTemplates/air_quality_index");
        assertThat(solutionTemplateDetailsPage.getHeadOfTitleCardName()).as("Title of page").isEqualTo("Air Quality Monitoring");
        assertThat(solutionTemplateDetailsPage.getTitleCardParagraphText()).as("Title of ST card").contains("AIR Quality Monitoring template");
        assertThat(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText()).as("Solution description").contains("Air Quality Monitoring template");
        solutionTemplateDetailsPage.assertAirQualityMonitoringScreenshotsAreCorrected();
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from general page")
    public void installAirQualityMonitoringFromGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP, AIR_QUALITY_MONITORING_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        assertThat(solutionTemplatesInstalledView.alarmFirstParagraphAirQualityMonitoring().getText())
                .as("First paragraph of solution instruction").contains(AIR_QUALITY_MONITORING_DASHBOARD);
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from details page")
    public void installAirQualityMonitoringFromDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP, AIR_QUALITY_MONITORING_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        assertThat(solutionTemplatesInstalledView.alarmFirstParagraphAirQualityMonitoring().getText())
                .as("First paragraph of solution instruction").contains(AIR_QUALITY_MONITORING_DASHBOARD);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on button the close (x mark)")
    public void closeInstallAirQualityMonitoringPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on bottom button")
    public void closeInstallAirQualityMonitoringPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Check entity installation after solution template installation")
    public void installEntities() {
        testRestClient.postAirQualityMonitoring();
        sideBarMenuView.ruleChainsBtn().click();
        List.of(AQI_SENSOR_RULE_CHAIN, AQI_CITY_RULE_CHAIN)
                .forEach(rc -> assertIsDisplayed(ruleChainsPage.entity(rc)));

        sideBarMenuView.goToDeviceGroups();
        assertIsDisplayed(devicePage.entity(AQI_SENSOR_DEVICE_GROUP));

        devicePage.entity("All").click();
        List.of(AIR_QUALITY_SENSOR_1_DEVICE, AIR_QUALITY_SENSOR_2_DEVICE, AIR_QUALITY_SENSOR_3_DEVICE, AIR_QUALITY_SENSOR_4_DEVICE,
                        AIR_QUALITY_SENSOR_5_DEVICE)
                .forEach(d -> assertIsDisplayed(devicePage.entity(d)));

        sideBarMenuView.goToAssetGroups();
        assertIsDisplayed(assetPage.entity(AQI_CITY_ASSET_GROUP));

        assetPage.entity("All").click();
        assertIsDisplayed(assetPage.entity(LOS_ANGELES_CA_ASSET));

        sideBarMenuView.openAssetProfiles();
        assertIsDisplayed(profilesPage.entity(AQI_CITY_ASSET_PROFILE));

        sideBarMenuView.goToAllDashboards();
        List.of(AIR_QUALITY_MONITORING_DASHBOARD, AIR_QUALITY_MONITORING_ADMINISTRATOR_DASHBOARD)
                .forEach(db -> assertIsDisplayed(dashboardPage.entity(db)));

        sideBarMenuView.goToInstances();
        assertIsDisplayed(instancesPage.entity((REMOTE_LOCATION_EDGE)));

        sideBarMenuView.goToRuleChainTemplates();
        List.of(AQI_SENSOR_REMOTE_LOCATION_RULE_CHAIN, AQI_CITY_REMOTE_LOCATION_RULE_CHAIN)
                .forEach(rc -> assertIsDisplayed(ruleChainTemplatesPage.entity((rc))));
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from general solution templates page)")
    public void airQualityMonitoringDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.airQualityMonitoringInstallBtn();
        testRestClient.postAirQualityMonitoring();
        refreshPage();

        assertInvisibilityOfElement(element);
        assertIsDisplayed(solutionTemplatesHomePage.airQualityMonitoringDeleteBtn());
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from details solution templates page)")
    public void airQualityMonitoringDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postAirQualityMonitoring();
        refreshPage();

        assertInvisibilityOfElement(element);
        assertIsDisplayed(solutionTemplateDetailsPage.deleteBtn());
    }

    @Test
    @Description("After install open instruction by click on instruction button (from general solution templates page)")
    public void airQualityMonitoringOpenInstruction() {
        testRestClient.postAirQualityMonitoring();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstructionBtn().click();

        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark)")
    public void airQualityMonitoringCloseInstruction() {
        testRestClient.postAirQualityMonitoring();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button")
    public void airQualityMonitoringCloseInstructionByCloseBtn() {
        testRestClient.postAirQualityMonitoring();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from details solution templates page)")
    public void airQualityMonitoringDetailsPageInstructionGoToMainDashboard() {
        testRestClient.postAirQualityMonitoring();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP, AIR_QUALITY_MONITORING_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("After install open instruction by click on instruction button (from details solution templates page)")
    public void airQualityMonitoringDetailsPageOpenInstruction() {
        testRestClient.postAirQualityMonitoring();

        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark) (from details solution templates page)")
    public void airQualityMonitoringDetailsPageDetailsPageCloseInstruction() {
        testRestClient.postAirQualityMonitoring();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button (from details solution templates page)")
    public void airQualityMonitoringDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postAirQualityMonitoring();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from general solution templates page)")
    public void airQualityMonitoringInstructionGoToMainDashboard() {
        testRestClient.postAirQualityMonitoring();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboard();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP, AIR_QUALITY_MONITORING_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("Check delete entity after delete solution template")
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteAirQualityMonitoring();

        sideBarMenuView.ruleChainsBtn().click();
        List.of(AQI_SENSOR_RULE_CHAIN, AQI_CITY_RULE_CHAIN)
                .forEach(rc -> ruleChainsPage.assertEntityIsNotPresent(rc));

        sideBarMenuView.goToDeviceGroups();
        devicePage.assertEntityIsNotPresent(AQI_SENSOR_DEVICE_GROUP);

        devicePage.entity("All").click();
        List.of(AIR_QUALITY_SENSOR_1_DEVICE, AIR_QUALITY_SENSOR_2_DEVICE, AIR_QUALITY_SENSOR_3_DEVICE, AIR_QUALITY_SENSOR_4_DEVICE,
                        AIR_QUALITY_SENSOR_5_DEVICE)
                .forEach(d -> devicePage.assertEntityIsNotPresent(d));

        sideBarMenuView.goToAssetGroups();
        assetPage.assertEntityIsNotPresent(AQI_CITY_ASSET_GROUP);

        assetPage.entity("All").click();
        assetPage.assertEntityIsNotPresent(LOS_ANGELES_CA_ASSET);

        sideBarMenuView.openAssetProfiles();
        profilesPage.assertEntityIsNotPresent(AQI_CITY_ASSET_PROFILE);

        sideBarMenuView.goToAllDashboards();
        List.of(AIR_QUALITY_MONITORING_DASHBOARD, AIR_QUALITY_MONITORING_ADMINISTRATOR_DASHBOARD)
                .forEach(db -> dashboardPage.assertEntityIsNotPresent(db));

        sideBarMenuView.goToInstances();
        instancesPage.assertEntityIsNotPresent((REMOTE_LOCATION_EDGE));

        sideBarMenuView.goToRuleChainTemplates();
        List.of(AQI_SENSOR_REMOTE_LOCATION_RULE_CHAIN, AQI_CITY_REMOTE_LOCATION_RULE_CHAIN)
                .forEach(rc -> ruleChainTemplatesPage.assertEntityIsNotPresent((rc)));
    }

    @Test
    @Description("Check redirect by click on links in instruction")
    public void linksBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.airQualityMonitoringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String linkAlarmRules = solutionTemplatesInstalledView.getAlarmRulesLink();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String linkDeviceProfile = solutionTemplatesInstalledView.getDeviceProfileLink();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP, AIR_QUALITY_MONITORING_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, AIR_QUALITY_MONITORING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(urls).hasSize(1).as("All dashboard links btn redirect to dashboard")
                .contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertThat(linkHttpApi).as("HTTP API link").isEqualTo(HTTP_API_DOCS_URL);
        assertThat(linkConnectionDevices).as("Connection devices link").isEqualTo(CONNECTIVITY_DOCS_URL);
        assertThat(linkAlarmRules).as("Alarm rule link").isEqualTo(ALARM_RULES_DOCS_URL);
        assertThat(linkDeviceProfile).as("Device profile link").isEqualTo(getBaseUiUrl() + "/profiles/deviceProfiles");
    }
}
