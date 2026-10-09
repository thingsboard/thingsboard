// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.tests.solutiontemplates.smartirrigation;

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
import static org.thingsboard.server.msa.ui.utils.Const.CONNECTIVITY_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.Const.HTTP_API_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.EVENING_SCHEDULER_EVENT;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.IRRIGATION_MANAGEMENT_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.MORNING_SCHEDULER_EVENT;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.REMOTE_FARM_EDGE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_COUNT_ALARMS_REMOTE_FARM_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_COUNT_ALARMS_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_FIELD_1_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_FIELD_2_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_FIELD_ASSET_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_FIELD_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_FIELD_RULE_REMOTE_FARM_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SMART_VALVE_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SMART_VALVE_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SMART_VALVE_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SMART_VALVE_REMOTE_FARM_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SMART_VALVE_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_3_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_4_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_5_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_6_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_7_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_8_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_REMOTE_FARM_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_SOIL_MOISTURE_SENSOR_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_WATER_METER_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_WATER_METER_REMOTE_FARM_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_WATER_METER_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_WATER_WATER_METER_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SI_WATER_WATER_METER_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_IRRIGATION_ASSET_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_IRRIGATION_DASHBOARD_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_IRRIGATION_DEVICE_GROUP;

@Feature("Installation")
@Story("Smart Irrigation")
public class SmartIrrigationInstallTest extends AbstractSolutionTemplateTest {

    @AfterMethod
    public void delete() {
        testRestClient.deleteSmartIrrigation();
    }

    @Test
    @Description("Redirect to page with short description (with screenshots) and corresponds to the selected template" +
            " by click on details button from general page")
    public void smartIrrigationOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        assertThat(getUrl()).as("Redirected URL equals to details ST URL").isEqualTo(Const.URL + "/solutionTemplates/smart_irrigation");
        assertThat(solutionTemplateDetailsPage.getHeadOfTitleCardName()).as("Title of page").isEqualTo("Smart Irrigation");
        assertThat(solutionTemplateDetailsPage.getTitleCardParagraphText()).as("Title of ST card").contains("Smart Irrigation template");
        assertThat(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText()).as("Solution description").contains("Smart Irrigation template");
        solutionTemplateDetailsPage.assertSmartIrrigationScreenshotsAreCorrected();
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from general page")
    public void installSmartIrrigationFromGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP, IRRIGATION_MANAGEMENT_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        assertThat(solutionTemplatesInstalledView.solutionInstructionFirstParagraphSmartIrrigation().getText())
                .as("First paragraph of solution instruction").contains(IRRIGATION_MANAGEMENT_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.dashboardsFirstParagraphSmartIrrigation().getText())
                .as("First paragraph of dashboard section in solution instruction").contains(IRRIGATION_MANAGEMENT_DASHBOARD);
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from details page")
    public void installSmartIrrigationFromDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP, IRRIGATION_MANAGEMENT_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        assertThat(solutionTemplatesInstalledView.solutionInstructionFirstParagraphSmartIrrigation().getText())
                .as("First paragraph of solution instruction").contains(IRRIGATION_MANAGEMENT_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.dashboardsFirstParagraphSmartIrrigation().getText())
                .as("First paragraph of dashboard section in solution instruction").contains(IRRIGATION_MANAGEMENT_DASHBOARD);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on button the close (x mark)")
    public void closeInstallIrrigationOfficePopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on bottom button")
    public void closeInstallSmartIrrigationPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Check entity installation after solution template installation")
    public void installEntities() {
        testRestClient.postSmartIrrigation();
        sideBarMenuView.ruleChainsBtn().click();
        List.of(SI_COUNT_ALARMS_RULE_CHAIN, SI_FIELD_RULE_CHAIN, SI_SOIL_MOISTURE_RULE_CHAIN, SI_WATER_METER_RULE_CHAIN, SI_SMART_VALVE_RULE_CHAIN)
                .forEach(rc -> assertIsDisplayed(ruleChainsPage.entity(rc)));

        sideBarMenuView.openDeviceProfiles();
        List.of(SI_SMART_VALVE_DEVICE_PROFILE, SI_WATER_METER_DEVICE_PROFILE, SI_SOIL_MOISTURE_SENSOR_DEVICE_PROFILE)
                .forEach(dp -> assertIsDisplayed(profilesPage.entity(dp)));

        sideBarMenuView.goToDeviceGroups();
        assertIsDisplayed(devicePage.entity(SMART_IRRIGATION_DEVICE_GROUP));

        devicePage.entity("All").click();
        devicePage.changeItemsCountPerPage(30);
        List.of(SI_WATER_WATER_METER_1_DEVICE, SI_WATER_WATER_METER_2_DEVICE, SI_SMART_VALVE_1_DEVICE, SI_SMART_VALVE_2_DEVICE,
                        SI_SOIL_MOISTURE_1_DEVICE, SI_SOIL_MOISTURE_2_DEVICE, SI_SOIL_MOISTURE_3_DEVICE, SI_SOIL_MOISTURE_4_DEVICE,
                        SI_SOIL_MOISTURE_5_DEVICE, SI_SOIL_MOISTURE_6_DEVICE, SI_SOIL_MOISTURE_7_DEVICE, SI_SOIL_MOISTURE_8_DEVICE)
                .forEach(d -> assertIsDisplayed(devicePage.entity(d)));

        sideBarMenuView.goToAssetGroups();
        assertIsDisplayed(assetPage.entity(SMART_IRRIGATION_ASSET_GROUP));

        assetPage.entity("All").click();
        List.of(SI_FIELD_1_ASSET, SI_FIELD_2_ASSET)
                .forEach(a -> assertIsDisplayed(assetPage.entity(a)));

        sideBarMenuView.openAssetProfiles();
        assertIsDisplayed(profilesPage.entity(SI_FIELD_ASSET_PROFILE));

        sideBarMenuView.goToAllDashboards();
        assertIsDisplayed(dashboardPage.entity(IRRIGATION_MANAGEMENT_DASHBOARD));

        sideBarMenuView.goToScheduler();
        assertThat(schedulerPage.schedulers(MORNING_SCHEDULER_EVENT)).hasSize(2).
                as("2" + MORNING_SCHEDULER_EVENT + "have been created").
                allSatisfy(this::assertIsDisplayed);
        assertThat(schedulerPage.schedulers(EVENING_SCHEDULER_EVENT)).hasSize(2).
                as("2" + EVENING_SCHEDULER_EVENT + "have been created").
                allSatisfy(this::assertIsDisplayed);

        sideBarMenuView.goToInstances();
        assertIsDisplayed(instancesPage.entity(REMOTE_FARM_EDGE));

        sideBarMenuView.goToRuleChainTemplates();
        List.of(SI_COUNT_ALARMS_REMOTE_FARM_RULE_CHAIN, SI_FIELD_RULE_REMOTE_FARM_CHAIN, SI_SOIL_MOISTURE_REMOTE_FARM_RULE_CHAIN,
                        SI_WATER_METER_REMOTE_FARM_RULE_CHAIN, SI_SMART_VALVE_REMOTE_FARM_RULE_CHAIN)
                .forEach(rc -> assertIsDisplayed(ruleChainTemplatesPage.entity(rc)));
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from general solution templates page)")
    public void smartIrrigationDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.smartIrrigationInstallBtn();
        testRestClient.postSmartIrrigation();
        refreshPage();

        assertInvisibilityOfElement(element);
        assertIsDisplayed(solutionTemplatesHomePage.smartIrrigationDeleteBtn());
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from details solution templates page)")
    public void smartIrrigationDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postSmartIrrigation();
        refreshPage();

        assertInvisibilityOfElement(element);
        assertIsDisplayed(solutionTemplateDetailsPage.deleteBtn());
    }

    @Test
    @Description("After install open instruction by click on instruction button (from general solution templates page)")
    public void smartIrrigationOpenInstruction() {
        testRestClient.postSmartIrrigation();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstructionBtn().click();

        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark)")
    public void smartIrrigationCloseInstruction() {
        testRestClient.postSmartIrrigation();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button")
    public void smartIrrigationCloseInstructionByCloseBtn() {
        testRestClient.postSmartIrrigation();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from details solution templates page)")
    public void smartIrrigationInstructionGoToMainDashboard() {
        testRestClient.postSmartIrrigation();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboard();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP, IRRIGATION_MANAGEMENT_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("After install open instruction by click on instruction button (from details solution templates page)")
    public void smartIrrigationDetailsPageOpenInstruction() {
        testRestClient.postSmartIrrigation();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark) (from details solution templates page)")
    public void smartIrrigationDetailsPageCloseInstruction() {
        testRestClient.postSmartIrrigation();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button (from details solution templates page)")
    public void smartIrrigationDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postSmartIrrigation();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from general solution templates page)")
    public void smartIrrigationDetailsPageInstructionGoToMainDashboard() {
        testRestClient.postSmartIrrigation();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP, IRRIGATION_MANAGEMENT_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("Check delete entity after delete solution template")
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteSmartIrrigation();

        sideBarMenuView.ruleChainsBtn().click();
        List.of(SI_COUNT_ALARMS_RULE_CHAIN, SI_FIELD_RULE_CHAIN, SI_SOIL_MOISTURE_RULE_CHAIN, SI_WATER_METER_RULE_CHAIN,
                        SI_SMART_VALVE_RULE_CHAIN)
                .forEach(rc -> ruleChainsPage.assertEntityIsNotPresent(rc));

        sideBarMenuView.openDeviceProfiles();
        List.of(SI_SMART_VALVE_DEVICE_PROFILE, SI_WATER_METER_DEVICE_PROFILE, SI_SOIL_MOISTURE_SENSOR_DEVICE_PROFILE)
                .forEach(dp -> profilesPage.assertEntityIsNotPresent(dp));

        sideBarMenuView.goToDeviceGroups();
        devicePage.assertEntityIsNotPresent(SMART_IRRIGATION_DEVICE_GROUP);

        devicePage.entity("All").click();
        List.of(SI_WATER_WATER_METER_1_DEVICE, SI_WATER_WATER_METER_2_DEVICE, SI_SMART_VALVE_1_DEVICE, SI_SMART_VALVE_2_DEVICE,
                        SI_SOIL_MOISTURE_1_DEVICE, SI_SOIL_MOISTURE_2_DEVICE, SI_SOIL_MOISTURE_3_DEVICE, SI_SOIL_MOISTURE_4_DEVICE,
                        SI_SOIL_MOISTURE_5_DEVICE, SI_SOIL_MOISTURE_6_DEVICE, SI_SOIL_MOISTURE_7_DEVICE, SI_SOIL_MOISTURE_8_DEVICE)
                .forEach(d -> devicePage.assertEntityIsNotPresent(d));

        sideBarMenuView.goToAssetGroups();
        assetPage.assertEntityIsNotPresent(SMART_IRRIGATION_ASSET_GROUP);

        assetPage.entity("All").click();
        List.of(SI_FIELD_1_ASSET, SI_FIELD_2_ASSET)
                .forEach(a -> assetPage.assertEntityIsNotPresent(a));

        sideBarMenuView.openAssetProfiles();
        profilesPage.assertEntityIsNotPresent(SI_FIELD_ASSET_PROFILE);

        sideBarMenuView.goToAllDashboards();
        dashboardPage.assertEntityIsNotPresent(IRRIGATION_MANAGEMENT_DASHBOARD);

        sideBarMenuView.goToScheduler();
        List.of(MORNING_SCHEDULER_EVENT, EVENING_SCHEDULER_EVENT)
                .forEach(sc -> schedulerPage.schedulerIsNotPresent(sc));

        sideBarMenuView.goToInstances();
        instancesPage.assertEntityIsNotPresent((REMOTE_FARM_EDGE));

        sideBarMenuView.goToRuleChainTemplates();
        List.of(SI_COUNT_ALARMS_REMOTE_FARM_RULE_CHAIN, SI_FIELD_RULE_REMOTE_FARM_CHAIN, SI_SOIL_MOISTURE_REMOTE_FARM_RULE_CHAIN,
                        SI_WATER_METER_REMOTE_FARM_RULE_CHAIN, SI_SMART_VALVE_REMOTE_FARM_RULE_CHAIN)
                .forEach(rc -> ruleChainTemplatesPage.assertEntityIsNotPresent((rc)));
    }

    @Test
    @Description("Check redirect by click on links in instruction")
    public void linksBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartIrrigationInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP, IRRIGATION_MANAGEMENT_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SMART_IRRIGATION_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(urls).hasSize(1).as("All dashboard links btn redirect to dashboard")
                .contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertThat(linkHttpApi).as("HTTP API link").isEqualTo(HTTP_API_DOCS_URL);
        assertThat(linkConnectionDevices).as("Connection devices link").isEqualTo(CONNECTIVITY_DOCS_URL);
    }
}
