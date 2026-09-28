// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.tests.solutiontemplates.temperaturehumiditysensors;

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
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.CUSTOMER_D;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.CUSTOMER_DASHBOARD_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.READ_ONLY_ROLES;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.REMOTE_FACILITY_RI_EDGE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SENSOR_C1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SENSOR_T1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.TEMPERATURE_HUMIDITY_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.TEMPERATURE_HUMIDITY_SENSORS_REMOTE_FACILITY_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.TEMPERATURE_HUMIDITY_SENSORS_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.TEMPERATURE_SENSOR_DEVICE_PROFILE;

@Feature("Installation")
@Story("Temperature & Humidity Sensors")
public class TemperatureHumiditySensorsInstallTest extends AbstractSolutionTemplateTest {

    @AfterMethod
    public void delete() {
        testRestClient.deleteTemperatureHumidity();
    }

    @Test
    @Description("Redirect to page with short description (with screenshots) and corresponds to the selected template" +
            " by click on details button from general page")
    public void temperatureHumiditySensorsOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        assertThat(getUrl()).as("Redirected URL equals to details ST URL").isEqualTo(Const.URL + "/solutionTemplates/temperature_sensors");
        assertThat(solutionTemplateDetailsPage.getHeadOfTitleCardName()).as("Title of page").isEqualTo("Temperature & Humidity Sensors");
        assertThat(solutionTemplateDetailsPage.getTitleCardParagraphText()).as("Title of ST card").contains("Temperature & Humidity sensors");
        assertThat(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText()).as("Solution description").contains("Temperature & Humidity template");
        solutionTemplateDetailsPage.assertTemperatureHumiditySensorsScreenshotsAreCorrected();
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from general page")
    public void installTemperatureHumiditySensorsFromGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP, TEMPERATURE_HUMIDITY_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        assertThat(solutionTemplatesInstalledView.solutionInstructionFirstParagraphTemperatureHumiditySensor().getText())
                .as("First paragraph of solution instruction").contains(TEMPERATURE_HUMIDITY_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.solutionInstructionThirdParagraphTemperatureHumiditySensor().getText())
                .as("Third paragraph of solution instruction").contains(TEMPERATURE_HUMIDITY_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.alarmFirstParagraphTemperatureHumiditySensor().getText())
                .as("First paragraph of alarm section in solution instruction").contains(TEMPERATURE_HUMIDITY_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.customersFirstParagraphTemperatureHumiditySensor().getText())
                .as("First paragraph of customers section in solution instruction").contains(TEMPERATURE_HUMIDITY_DASHBOARD);
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from details page")
    public void installTemperatureHumiditySensorsFromDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP, TEMPERATURE_HUMIDITY_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        assertThat(solutionTemplatesInstalledView.solutionInstructionFirstParagraphTemperatureHumiditySensor().getText())
                .as("First paragraph of solution instruction").contains(TEMPERATURE_HUMIDITY_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.solutionInstructionThirdParagraphTemperatureHumiditySensor().getText())
                .as("Third paragraph of solution instruction").contains(TEMPERATURE_HUMIDITY_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.alarmFirstParagraphTemperatureHumiditySensor().getText())
                .as("First paragraph of alarm section in solution instruction").contains(TEMPERATURE_HUMIDITY_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.customersFirstParagraphTemperatureHumiditySensor().getText())
                .as("First paragraph of customers section in solution instruction").contains(TEMPERATURE_HUMIDITY_DASHBOARD);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on button the close (x mark)")
    public void closeInstallTemperatureHumiditySensorsPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on bottom button")
    public void closeInstallTemperatureHumiditySensorsPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Check entity installation after solution template installation")
    public void installEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.setFirstUserName();
        solutionTemplatesInstalledView.setSecondUserName();
        String user1 = solutionTemplatesInstalledView.getFirstUserName();
        String user2 = solutionTemplatesInstalledView.getSecondUserName();
        solutionTemplatesInstalledView.closeBtn().click();

        sideBarMenuView.ruleChainsBtn().click();
        assertIsDisplayed(ruleChainsPage.entity(TEMPERATURE_HUMIDITY_SENSORS_RULE_CHAIN));

        sideBarMenuView.goToRoles();
        assertIsDisplayed(rolesPage.entity(READ_ONLY_ROLES));

        sideBarMenuView.goToAllCustomers();
        assertIsDisplayed(customerPage.entity(CUSTOMER_D));

        customerPage.manageCustomersDeviceGroupsBtn(CUSTOMER_D).click();
        assertIsDisplayed(devicePage.entity(SENSOR_C1_DEVICE));

        devicePage.groupsBtn().click();
        assertIsDisplayed(devicePage.entity(TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP));

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersUserBtn(CUSTOMER_D).click();
        List.of(user1, user2)
                .forEach(u -> assertIsDisplayed(usersPage.entity(u)));

        sideBarMenuView.openDeviceProfiles();
        assertIsDisplayed(profilesPage.entity(TEMPERATURE_SENSOR_DEVICE_PROFILE));

        sideBarMenuView.goToDeviceGroups();
        assertIsDisplayed(devicePage.entity(TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP));

        devicePage.entity(TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP).click();
        assertIsDisplayed(devicePage.entity(SENSOR_T1_DEVICE));

        sideBarMenuView.goToAllDashboards();
        assertIsDisplayed(dashboardPage.entity(TEMPERATURE_HUMIDITY_DASHBOARD));

        sideBarMenuView.goToInstances();
        assertIsDisplayed(instancesPage.entity(REMOTE_FACILITY_RI_EDGE));

        sideBarMenuView.goToRuleChainTemplates();
        assertIsDisplayed(ruleChainTemplatesPage.entity(TEMPERATURE_HUMIDITY_SENSORS_REMOTE_FACILITY_RULE_CHAIN));
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from general solution templates page)")
    public void temperatureHumiditySensorsDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn();
        testRestClient.postTemperatureHumidity();
        refreshPage();

        assertInvisibilityOfElement(element);
        assertIsDisplayed(solutionTemplatesHomePage.temperatureHumiditySensorsDeleteBtn());
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from details solution templates page)")
    public void temperatureHumiditySensorsDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postTemperatureHumidity();
        refreshPage();

        assertInvisibilityOfElement(element);
        assertIsDisplayed(solutionTemplateDetailsPage.deleteBtn());
    }

    @Test
    @Description("After install open instruction by click on instruction button (from general solution templates page)")
    public void temperatureHumiditySensorsOpenInstruction() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstructionBtn().click();

        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark)")
    public void temperatureHumiditySensorsCloseInstruction() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button")
    public void temperatureHumiditySensorsCloseInstructionByCloseBtn() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from details solution templates page)")
    public void temperatureHumiditySensorsInstructionGoToMainDashboard() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboard();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP, TEMPERATURE_HUMIDITY_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("After install open instruction by click on instruction button (from details solution templates page)")
    public void temperatureHumiditySensorsDetailsPageOpenInstruction() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark) (from details solution templates page)")
    public void temperatureHumiditySensorsDetailsPageCloseInstruction() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button (from details solution templates page)")
    public void temperatureHumiditySensorsDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from general solution templates page)")
    public void temperatureHumiditySensorsDetailsPageInstructionGoToMainDashboard() {
        testRestClient.postTemperatureHumidity();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP, TEMPERATURE_HUMIDITY_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("Check delete entity after delete solution template")
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteTemperatureHumidity();

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.assertEntityIsNotPresent(TEMPERATURE_HUMIDITY_SENSORS_RULE_CHAIN);

        sideBarMenuView.goToRoles();
        rolesPage.assertEntityIsNotPresent(READ_ONLY_ROLES);

        sideBarMenuView.goToAllCustomers();
        customerPage.assertEntityIsNotPresent(CUSTOMER_D);

        sideBarMenuView.openDeviceProfiles();
        profilesPage.assertEntityIsNotPresent(TEMPERATURE_SENSOR_DEVICE_PROFILE);

        sideBarMenuView.goToDeviceGroups();
        devicePage.assertEntityIsNotPresent(TEMPERATURE_HUMIDITY_SENSORS_DEVICE_GROUP);

        devicePage.entity("All").click();
        devicePage.assertEntityIsNotPresent(SENSOR_T1_DEVICE);

        sideBarMenuView.goToAllDashboards();
        dashboardPage.assertEntityIsNotPresent(TEMPERATURE_HUMIDITY_DASHBOARD);

        sideBarMenuView.goToInstances();
        instancesPage.assertEntityIsNotPresent(REMOTE_FACILITY_RI_EDGE);

        sideBarMenuView.goToRuleChainTemplates();
        ruleChainTemplatesPage.assertEntityIsNotPresent(TEMPERATURE_HUMIDITY_SENSORS_REMOTE_FACILITY_RULE_CHAIN);
    }

    @Test
    @Description("Check redirect by click on links in instruction")
    public void linksBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.temperatureHumiditySensorsInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String guide = solutionTemplatesInstalledView.getGuideLink();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String linkAlarmRule = solutionTemplatesInstalledView.getAlarmRuleLink();
        String linkDeviceProfile = solutionTemplatesInstalledView.getDeviceProfileLink();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP, TEMPERATURE_HUMIDITY_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, CUSTOMER_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(urls).hasSize(1).as("All dashboard links btn redirect to dashboard")
                .contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertThat(guide).as("Dashboard guide link").isEqualTo(DASHBOARD_GIDE_DOCS_URL);
        assertThat(linkHttpApi).as("HTTP API link").isEqualTo(HTTP_API_DOCS_URL);
        assertThat(linkConnectionDevices).as("Connection devices link").isEqualTo(CONNECTIVITY_DOCS_URL);
        assertThat(linkAlarmRule).as("Alarm rule link").isEqualTo(ALARM_RULES_DOCS_URL);
        assertThat(linkDeviceProfile).as("Device profile link").isEqualTo(getBaseUiUrl() + "/profiles/deviceProfiles");
    }
}
