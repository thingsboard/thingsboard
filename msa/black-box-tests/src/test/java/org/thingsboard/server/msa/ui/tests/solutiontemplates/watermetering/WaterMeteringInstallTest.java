// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.tests.solutiontemplates.watermetering;

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
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.REMOTE_FACILITY_WI_EDGE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_CUSTOMER_A_CUSTOMER;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_CUSTOMER_B_CUSTOMER;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_CUSTOMER_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_DASHBOARD_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_READ_ONLY_ROLES;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_SHARED_DASHBOARD_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_SOLUTION_ALARM_ROUTING_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_SOLUTION_MAIN_REMOTE_FACILITY_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_SOLUTION_MAIN_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_SOLUTION_TENANT_ALARM_ROUTING_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_TENANT_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_USER_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERING_USER_ROLES;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METERS_DEVICE_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WATER_METER_DEVICE_PROFILE_WM;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WM0000123_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WM0000124_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.WM0000125_DEVICE;

@Feature("Installation")
@Story("Water metering")
public class WaterMeteringInstallTest extends AbstractSolutionTemplateTest {

    @AfterMethod
    public void delete() {
        testRestClient.deleteWaterMetering();
    }

    @Test
    @Description("Redirect to page with short description (with screenshots) and corresponds to the selected template" +
            " by click on details button from general page")
    public void waterMeteringOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        assertThat(getUrl()).as("Redirected URL equals to details ST URL").isEqualTo(Const.URL + "/solutionTemplates/water_metering");
        assertThat(solutionTemplateDetailsPage.getHeadOfTitleCardName()).as("Title of page").isEqualTo("Water metering");
        assertThat(solutionTemplateDetailsPage.getTitleCardParagraphText()).as("Title of ST card").contains("Water Metering template");
        assertThat(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText()).as("Solution description")
                .contains("Water Metering template");
        solutionTemplateDetailsPage.assertWaterMeteringScreenshotsAreCorrected();
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from general page")
    public void installWaterMeteringFromGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP, WATER_METERING_TENANT_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        assertThat(solutionTemplatesInstalledView.solutionInstructionFirstParagraphWaterMetering().getText())
                .as("First paragraph of dashboard section in solution instruction").contains(WATER_METERING_TENANT_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.solutionInstructionSecondParagraphWaterMetering().getText())
                .as("Second paragraph of dashboard section in solution instruction").contains(WATER_METERING_TENANT_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.customersFirstParagraphWaterMetering().getText())
                .as("First paragraph of customers section in solution instruction").contains(WATER_METERING_TENANT_DASHBOARD);
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from details page")
    public void installWaterMeteringFromDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP, WATER_METERING_TENANT_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        assertThat(solutionTemplatesInstalledView.solutionInstructionFirstParagraphWaterMetering().getText())
                .as("First paragraph of dashboard section in solution instruction ").contains(WATER_METERING_TENANT_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.solutionInstructionSecondParagraphWaterMetering().getText())
                .as("Second paragraph of dashboard section in solution instruction").contains(WATER_METERING_TENANT_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.customersFirstParagraphWaterMetering().getText())
                .as("First paragraph of customers section in solution instruction").contains(WATER_METERING_TENANT_DASHBOARD);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on button the close (x mark)")
    public void closeInstallWaterMeteringPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on bottom button")
    public void closeInstallWaterMeteringPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Check entity installation after solution template installation")
    public void installEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.setFirstUserName();
        solutionTemplatesInstalledView.setSecondUserName();
        String userCustomerA = solutionTemplatesInstalledView.getFirstUserName();
        String userCustomerB = solutionTemplatesInstalledView.getSecondUserName();
        solutionTemplatesInstalledView.closeBtn().click();

        sideBarMenuView.ruleChainsBtn().click();
        List.of(WATER_METERING_SOLUTION_MAIN_RULE_CHAIN, WATER_METERING_SOLUTION_ALARM_ROUTING_RULE_CHAIN,
                        WATER_METERING_SOLUTION_TENANT_ALARM_ROUTING_RULE_CHAIN)
                .forEach(rc -> assertIsDisplayed(ruleChainsPage.entity(rc)));

        sideBarMenuView.goToRoles();
        List.of(WATER_METERING_READ_ONLY_ROLES, WATER_METERING_USER_ROLES)
                .forEach(r -> assertIsDisplayed(rolesPage.entity(r)));

        sideBarMenuView.goToCustomerGroups();
        assertIsDisplayed(customerPage.entity(WATER_METERING_CUSTOMER_GROUP));

        customerPage.entity("All").click();
        List.of(WATER_METERING_CUSTOMER_A_CUSTOMER, WATER_METERING_CUSTOMER_B_CUSTOMER)
                .forEach(c -> assertIsDisplayed(customerPage.entity(c)));

        customerPage.manageCustomersUserBtn(WATER_METERING_CUSTOMER_A_CUSTOMER).click();
        assertIsDisplayed(usersPage.entity(userCustomerA));

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersUserBtn(WATER_METERING_CUSTOMER_B_CUSTOMER).click();
        assertIsDisplayed(usersPage.entity(userCustomerB));

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(WATER_METERING_CUSTOMER_A_CUSTOMER).click();
        devicePage.groupsBtn().click();
        assertIsDisplayed(devicePage.entity(WATER_METERS_DEVICE_GROUP));

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(WATER_METERING_CUSTOMER_B_CUSTOMER).click();
        devicePage.groupsBtn().click();
        assertIsDisplayed(devicePage.entity(WATER_METERS_DEVICE_GROUP));

        sideBarMenuView.openDeviceProfiles();
        assertIsDisplayed(profilesPage.entity(WATER_METER_DEVICE_PROFILE_WM));

        sideBarMenuView.goToAllDevices();
        List.of(WM0000123_DEVICE, WM0000124_DEVICE, WM0000125_DEVICE)
                .forEach(d -> assertIsDisplayed(devicePage.entity(d)));

        sideBarMenuView.goToDashboardGroups();
        List.of(WATER_METERING_DASHBOARD_GROUP, WATER_METERING_SHARED_DASHBOARD_GROUP)
                .forEach(dg -> assertIsDisplayed(dashboardPage.entity(dg)));

        dashboardPage.entity("All").click();
        List.of(WATER_METERING_USER_DASHBOARD, WATER_METERING_TENANT_DASHBOARD)
                .forEach(d -> assertIsDisplayed(dashboardPage.entity(d)));

        sideBarMenuView.goToInstances();
        assertIsDisplayed(instancesPage.entity(REMOTE_FACILITY_WI_EDGE));

        sideBarMenuView.goToRuleChainTemplates();
        assertIsDisplayed(ruleChainTemplatesPage.entity(WATER_METERING_SOLUTION_MAIN_REMOTE_FACILITY_RULE_CHAIN));
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from general solution templates page)")
    public void waterMeteringDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.waterMeteringInstallBtn();
        testRestClient.postWaterMetering();
        refreshPage();
        scrollToElement(solutionTemplatesHomePage.waterMetering());

        assertInvisibilityOfElement(element);
        assertIsDisplayed(solutionTemplatesHomePage.waterMeteringDeleteBtn());
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from details solution templates page)")
    public void waterMeteringDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postWaterMetering();
        refreshPage();

        assertInvisibilityOfElement(element);
        assertIsDisplayed(solutionTemplateDetailsPage.deleteBtn());
    }

    @Test
    @Description("After install open instruction by click on instruction button (from general solution templates page)")
    public void waterMeteringOpenInstruction() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstructionBtn().click();

        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark)")
    public void waterMeteringCloseInstruction() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button")
    public void waterMeteringCloseInstructionByCloseBtn() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from details solution templates page)")
    public void waterMeteringInstructionGoToMainDashboard() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboard();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP, WATER_METERING_TENANT_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("After install open instruction by click on instruction button (from details solution templates page)")
    public void waterMeteringDetailsPageOpenInstruction() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark) (from details solution templates page)")
    public void waterMeteringDetailsPageCloseInstruction() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button (from details solution templates page)")
    public void waterMeteringDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from general solution templates page)")
    public void waterMeteringDetailsPageInstructionGoToMainDashboard() {
        testRestClient.postWaterMetering();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP, WATER_METERING_TENANT_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("Check delete entity after delete solution template")
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteWaterMetering();

        sideBarMenuView.ruleChainsBtn().click();
        List.of(WATER_METERING_SOLUTION_MAIN_RULE_CHAIN, WATER_METERING_SOLUTION_ALARM_ROUTING_RULE_CHAIN,
                        WATER_METERING_SOLUTION_TENANT_ALARM_ROUTING_RULE_CHAIN)
                .forEach(rc -> ruleChainsPage.assertEntityIsNotPresent(rc));

        sideBarMenuView.goToRoles();
        List.of(WATER_METERING_READ_ONLY_ROLES, WATER_METERING_USER_ROLES)
                .forEach(r -> rolesPage.assertEntityIsNotPresent(r));

        sideBarMenuView.goToCustomerGroups();
        customerPage.assertEntityIsNotPresent(WATER_METERING_CUSTOMER_GROUP);
        customerPage.entity("All").click();
        List.of(WATER_METERING_CUSTOMER_A_CUSTOMER, WATER_METERING_CUSTOMER_B_CUSTOMER)
                .forEach(c -> customerPage.assertEntityIsNotPresent(c));

        sideBarMenuView.openDeviceProfiles();
        profilesPage.assertEntityIsNotPresent(WATER_METER_DEVICE_PROFILE_WM);

        sideBarMenuView.goToDeviceGroups();
        devicePage.assertEntityIsNotPresent(WATER_METERS_DEVICE_GROUP);

        devicePage.entity("All").click();
        List.of(WM0000123_DEVICE, WM0000124_DEVICE, WM0000125_DEVICE)
                .forEach(d -> devicePage.assertEntityIsNotPresent(d));

        sideBarMenuView.goToDashboardGroups();
        List.of(WATER_METERING_DASHBOARD_GROUP, WATER_METERING_SHARED_DASHBOARD_GROUP)
                .forEach(db -> dashboardPage.assertEntityIsNotPresent(db));

        dashboardPage.entity("All").click();
        List.of(WATER_METERING_USER_DASHBOARD, WATER_METERING_TENANT_DASHBOARD)
                .forEach(db -> dashboardPage.assertEntityIsNotPresent(db));

        sideBarMenuView.goToInstances();
        instancesPage.assertEntityIsNotPresent(REMOTE_FACILITY_WI_EDGE);

        sideBarMenuView.goToRuleChainTemplates();
        ruleChainTemplatesPage.assertEntityIsNotPresent(WATER_METERING_SOLUTION_MAIN_REMOTE_FACILITY_RULE_CHAIN);
    }

    @Test
    @Description("Check redirect by click on links in instruction")
    public void linksBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.waterMeteringInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String guide = solutionTemplatesInstalledView.getGuideLink();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String linkAlarmRule = solutionTemplatesInstalledView.getAlarmRuleLink();
        String linkDeviceProfile = solutionTemplatesInstalledView.getDeviceProfileLink();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP, WATER_METERING_TENANT_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, WATER_METERING_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(urls).hasSize(1).as("All dashboard links btn redirect to dashboard")
                .contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertThat(guide).as("Dashboard guide link").isEqualTo(DASHBOARD_GIDE_DOCS_URL);
        assertThat(linkHttpApi).as("HTTP API link").isEqualTo(HTTP_API_DOCS_URL);
        assertThat(linkConnectionDevices).as("Connection devices link").isEqualTo(CONNECTIVITY_DOCS_URL);
        assertThat(linkAlarmRule).as("Alarm rule link").isEqualTo(ALARM_RULES_DOCS_URL);
        assertThat(linkDeviceProfile).as("Device profile link").isEqualTo(getBaseUiUrl() + "/profiles/deviceProfiles");
    }
}
