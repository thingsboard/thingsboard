// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.tests.solutiontemplates.fleettracking;

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
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.BUS_A_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.BUS_B_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.BUS_C_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.BUS_DEVICES_DEVICE_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.BUS_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.BUS_D_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.FLEET_TRACKING_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.FLEET_TRACKING_DASHBOARD_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.REMOTE_BUS_STATION_R1_EDGE;

@Feature("Installation")
@Story("Fleet tracking")
public class FleetTrackingInstallTest extends AbstractSolutionTemplateTest {

    @AfterMethod
    public void delete() {
        testRestClient.deleteFleetTracking();
    }

    @Test
    @Description("Redirect to page with short description (with screenshots) and corresponds to the selected template" +
            " by click on details button from general page")
    public void fleetTrackingOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        assertThat(getUrl()).as("Redirected URL equals to details ST URL").isEqualTo(Const.URL + "/solutionTemplates/fleet_tracking");
        assertThat(solutionTemplateDetailsPage.getHeadOfTitleCardName()).as("Title of page").isEqualTo("Fleet tracking");
        assertThat(solutionTemplateDetailsPage.getTitleCardParagraphText()).as("Title of ST card").contains("Fleet Tracking template");
        assertThat(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText()).as("Solution description").contains("Fleet Tracking template");
        solutionTemplateDetailsPage.assertFleetTrackingScreenshotsAreCorrected();
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from general page")
    public void installFleetTrackingFromGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP, FLEET_TRACKING_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        assertThat(solutionTemplatesInstalledView.solutionInstructionFirstParagraphFleetTracking().getText().toLowerCase())
                .as("First paragraph of solution instruction").contains(FLEET_TRACKING_DASHBOARD.toLowerCase());
        assertThat(solutionTemplatesInstalledView.solutionInstructionThirdParagraphFleetTracking().getText().toLowerCase())
                .as("Third paragraph of solution instruction").contains(FLEET_TRACKING_DASHBOARD.toLowerCase());
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from details page")
    public void installFleetTrackingFromDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP, FLEET_TRACKING_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        assertThat(solutionTemplatesInstalledView.solutionInstructionFirstParagraphFleetTracking().getText())
                .as("First paragraph of solution instruction").contains("Fleet Tracking");
        assertThat(solutionTemplatesInstalledView.solutionInstructionThirdParagraphFleetTracking().getText())
                .as("Third paragraph of solution instruction").contains("Fleet Tracking");
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on button the close (x mark)")
    public void closeInstallFleetTrackingPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on bottom button")
    public void closeInstallFleetTrackingPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Check entity installation after solution template installation")
    public void installEntities() {
        testRestClient.postFleetTracking();
        sideBarMenuView.openDeviceProfiles();
        assertIsDisplayed(profilesPage.entity(BUS_DEVICE_PROFILE));

        sideBarMenuView.goToDeviceGroups();
        assertIsDisplayed(devicePage.entity(BUS_DEVICES_DEVICE_GROUP));

        devicePage.entity("All").click();
        List.of(BUS_A_DEVICE, BUS_B_DEVICE, BUS_C_DEVICE, BUS_D_DEVICE)
                .forEach(d -> assertIsDisplayed(devicePage.entity(d)));

        sideBarMenuView.goToAllDashboards();
        assertIsDisplayed(dashboardPage.entity(FLEET_TRACKING_DASHBOARD));

        sideBarMenuView.goToInstances();
        assertIsDisplayed(instancesPage.entity(REMOTE_BUS_STATION_R1_EDGE));
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from general solution templates page)")
    public void fleetTrackingDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.fleetTrackingInstallBtn();
        testRestClient.postFleetTracking();
        refreshPage();

        assertInvisibilityOfElement(element);
        assertIsDisplayed(solutionTemplatesHomePage.fleetTrackingDeleteBtn());
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from details solution templates page)")
    public void fleetTrackingDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postFleetTracking();
        refreshPage();

        assertInvisibilityOfElement(element);
        assertIsDisplayed(solutionTemplateDetailsPage.deleteBtn());
    }

    @Test
    @Description("After install open instruction by click on instruction button (from general solution templates page)")
    public void fleetTrackingOpenInstruction() {
        testRestClient.postFleetTracking();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstructionBtn().click();

        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark)")
    public void fleetTrackingCloseInstruction() {
        testRestClient.postFleetTracking();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button")
    public void fleetTrackingCloseInstructionByCloseBtn() {
        testRestClient.postFleetTracking();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from details solution templates page)")
    public void fleetTrackingDetailsPageInstructionGoToMainDashboard() {
        testRestClient.postFleetTracking();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP, FLEET_TRACKING_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("After install open instruction by click on instruction button (from details solution templates page)")
    public void fleetTrackingDetailsPageOpenInstruction() {
        testRestClient.postFleetTracking();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark) (from details solution templates page)")
    public void fleetTrackingDetailsPageCloseInstruction() {
        testRestClient.postFleetTracking();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button (from details solution templates page)")
    public void fleetTrackingDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postFleetTracking();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from general solution templates page)")
    public void fleetTrackingInstructionGoToMainDashboard() {
        testRestClient.postFleetTracking();
        refreshPage();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboard();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP, FLEET_TRACKING_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("Check delete entity after delete solution template")
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteFleetTracking();

        sideBarMenuView.openDeviceProfiles();
        profilesPage.assertEntityIsNotPresent(BUS_DEVICE_PROFILE);

        sideBarMenuView.goToDeviceGroups();
        devicePage.assertEntityIsNotPresent(BUS_DEVICES_DEVICE_GROUP);

        devicePage.entity("All").click();
        List.of(BUS_A_DEVICE, BUS_B_DEVICE, BUS_C_DEVICE, BUS_D_DEVICE)
                .forEach(d -> devicePage.assertEntityIsNotPresent(d));

        sideBarMenuView.goToAllDashboards();
        dashboardPage.assertEntityIsNotPresent(FLEET_TRACKING_DASHBOARD);

        sideBarMenuView.goToInstances();
        instancesPage.assertEntityIsNotPresent(REMOTE_BUS_STATION_R1_EDGE);
    }

    @Test
    @Description("Check redirect by click on links in instruction")
    public void linksBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.fleetTrackingInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String guide = solutionTemplatesInstalledView.getGuideLink();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String linkAlarmRule = solutionTemplatesInstalledView.getAlarmRuleLink();
        String linkDeviceProfile = solutionTemplatesInstalledView.getDeviceProfileLink();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP, FLEET_TRACKING_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, FLEET_TRACKING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(urls).hasSize(1).as("All dashboard links btn redirect to dashboard")
                .contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertThat(guide).as("Dashboard guide link").isEqualTo(DASHBOARD_GIDE_DOCS_URL);
        assertThat(linkHttpApi).as("HTTP API link").isEqualTo(HTTP_API_DOCS_URL);
        assertThat(linkConnectionDevices).as("Connection devices link").isEqualTo(CONNECTIVITY_DOCS_URL);
        assertThat(linkAlarmRule).as("Alarm rule link").isEqualTo(ALARM_RULES_DOCS_URL);
        assertThat(linkDeviceProfile).as("Device profile link").isEqualTo(getBaseUiUrl() + "/profiles/deviceProfiles");
    }
}
