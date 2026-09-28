// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.tests.solutiontemplates.smartretail;

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
import static org.thingsboard.server.msa.ui.utils.Const.THINGSBOARD_INTEGRATION_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.Const.THINGSBOARD_IOT_GATEWAY_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.Const.THINGSBOARD_MQTT_GATEWAY_DOCS_URL;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.CHILLER3_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.CHILLER65644_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.CHILLER_378876_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.CHILLER_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.DOOR_SENSOR_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.DOOR_SENSOR_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.DOOR_SENSOR_3456_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.DOOR_SENSOR_3_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.DOOR_SENSOR_4534_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.DOOR_SENSOR_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.FREEZER_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.FREEZER_43545_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.FREEZER_67478;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.FREEZER_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.LIQUID_LEVEL_SENSOR_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.LIQUID_LEVEL_SENSOR_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.LIQUID_LEVEL_SENSOR_3_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.LIQUID_LEVEL_SENSOR_4_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.LIQUID_LEVEL_SENSOR_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.MOTION_SENSOR_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.MOTION_SENSOR_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.OCCUPANCY_SENSOR_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.OCCUPANCY_SENSOR_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.REMOTE_SUPERMARKET_EDGE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.RETAIL_COMPANY_A_CUSTOMER;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.RETAIL_COMPANY_B_CUSTOMER;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_BIN_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_BIN_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_BIN_3_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_BIN_4_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_BIN_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_RETAIL_ADMINISTRATORS_USER_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_RETAIL_ADMINISTRATOR_ROLE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_RETAIL_CUSTOMER_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_RETAIL_READ_ONLY_ROLE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_RETAIL_USERS_USER_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_RETAIL_USER_ROLE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SHELF_457321_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SHELF_557322_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SHELF_765765_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SHELF_89546_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SHELF_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SUPERMARKET_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMART_SUPERMARKET_USER_MANAGEMENT_DASHBOARD;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_1_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_2_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_3_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_4_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_5_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_6_DEVICE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SMOKE_SENSOR_DEVICE_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKETS_ASSET_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKETS_S1_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKETS_S2_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKETS_S3_ASSET;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKET_ADMINISTRATORS_SHARED_DASHBOARD_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKET_ASSET_PROFILE;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKET_DEVICES_DEVICE_GROUP;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKET_DEVICES_REMOTE_FACILITY_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKET_DEVICES_RULE_CHAIN;
import static org.thingsboard.server.msa.ui.utils.SolutionTemplatesConstants.SUPERMARKET_USER_SHARED_DASHBOARD_GROUP;

@Feature("Installation")
@Story("Smart Retail")
public class SmartRetailInstallTest extends AbstractSolutionTemplateTest {

    @AfterMethod
    public void delete() {
        testRestClient.deleteSmartRetail();
    }

    @Test
    @Description("Redirect to page with short description (with screenshots) and corresponds to the selected template" +
            " by click on details button from general page")
    public void smartRetailOpenDetailsByBtnOnGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.setHeadOfTitleName();
        solutionTemplateDetailsPage.setTitleCardParagraphText();
        solutionTemplateDetailsPage.setSolutionDescriptionParagraphText();

        assertThat(getUrl()).as("Redirected URL equals to details ST URL").isEqualTo(Const.URL + "/solutionTemplates/smart_retail");
        assertThat(solutionTemplateDetailsPage.getHeadOfTitleCardName()).as("Title of page").isEqualTo("Smart Retail");
        assertThat(solutionTemplateDetailsPage.getTitleCardParagraphText()).as("Title of ST card").contains("Smart Retail template");
        assertThat(solutionTemplateDetailsPage.getSolutionDescriptionParagraphText()).as("Solution description").contains("Smart Retail template");
        solutionTemplateDetailsPage.assertSmartRetailScreenshotsAreCorrected();
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from general page")
    public void installSmartRetailFromGeneralPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP, SMART_SUPERMARKET_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        assertThat(solutionTemplatesInstalledView.dashboardsSmartSupermarketAdministrationFirstParagraphSmartRetail().getText())
                .as("First paragraph of SSA dashboard section solution instruction").contains(SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.dashboardsSmartSupermarketFirstParagraphSmartRetail().getText())
                .as("First paragraph of SS dashboard section solution instruction").contains(SMART_SUPERMARKET_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.customersSmartSupermarketAdministrationThirdParagraphSmartRetail().getText())
                .as("Third paragraph of customer section in solution instruction").contains(SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD);
    }

    @Test
    @Description("Redirects to a new dashboard page and opens a pop-up window (Solution template successfully installed)" +
            " with a detailed description (description corresponds to the selected template) by click on install button from details page")
    public void installSmartRetailFromDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.installBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP, SMART_SUPERMARKET_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
        assertThat(solutionTemplatesInstalledView.dashboardsSmartSupermarketAdministrationFirstParagraphSmartRetail().getText())
                .as("First paragraph of SSA dashboard section solution instruction").contains(SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.dashboardsSmartSupermarketFirstParagraphSmartRetail().getText())
                .as("First paragraph of SS dashboard section solution instruction").contains(SMART_SUPERMARKET_DASHBOARD);
        assertThat(solutionTemplatesInstalledView.customersSmartSupermarketAdministrationThirdParagraphSmartRetail().getText())
                .as("Third paragraph of customer section in solution instruction").contains(SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on button the close (x mark)")
    public void closeInstallSmartRetailPopUp() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("After install close pop-up window the Solution template successfully installed by click on bottom button")
    public void closeInstallSmartRetailPopUpByBottomBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Check entity installation after solution template installation")
    public void installEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        String user1RetailCompanyA = solutionTemplatesInstalledView.users(RETAIL_COMPANY_A_CUSTOMER).get(0).getText();
        String user2RetailCompanyA = solutionTemplatesInstalledView.users(RETAIL_COMPANY_A_CUSTOMER).get(1).getText();
        String user1RetailCompanyB = solutionTemplatesInstalledView.users(RETAIL_COMPANY_B_CUSTOMER).get(0).getText();
        String user2RetailCompanyB = solutionTemplatesInstalledView.users(RETAIL_COMPANY_B_CUSTOMER).get(1).getText();
        solutionTemplatesInstalledView.closeBtn().click();

        sideBarMenuView.ruleChainsBtn().click();
        assertIsDisplayed(ruleChainsPage.entity(SUPERMARKET_DEVICES_RULE_CHAIN));

        sideBarMenuView.goToRoles();
        List.of(SMART_RETAIL_READ_ONLY_ROLE, SMART_RETAIL_USER_ROLE, SMART_RETAIL_ADMINISTRATOR_ROLE)
                .forEach(r -> assertIsDisplayed(rolesPage.entity(r)));

        sideBarMenuView.goToCustomerGroups();
        assertIsDisplayed(customerPage.entity(SMART_RETAIL_CUSTOMER_GROUP));

        customerPage.entity("All").click();
        customerPage.manageCustomersUserBtn(RETAIL_COMPANY_A_CUSTOMER).click();
        List.of(user1RetailCompanyA, user2RetailCompanyA)
                .forEach(u -> assertIsDisplayed(usersPage.entity(u)));

        usersPage.groupsBtn().click();
        List.of(SMART_RETAIL_USERS_USER_GROUP, SMART_RETAIL_ADMINISTRATORS_USER_GROUP)
                .forEach(ug -> assertIsDisplayed(usersPage.entity(ug)));

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersUserBtn(RETAIL_COMPANY_B_CUSTOMER).click();
        List.of(user1RetailCompanyB, user2RetailCompanyB)
                .forEach(u -> assertIsDisplayed(usersPage.entity(u)));

        usersPage.groupsBtn().click();
        List.of(SMART_RETAIL_USERS_USER_GROUP, SMART_RETAIL_ADMINISTRATORS_USER_GROUP)
                .forEach(ug -> assertIsDisplayed(usersPage.entity(ug)));

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(RETAIL_COMPANY_A_CUSTOMER).click();
        devicePage.changeItemsCountPerPage(30);
        List.of(CHILLER3_DEVICE, CHILLER65644_DEVICE, DOOR_SENSOR_1_DEVICE, DOOR_SENSOR_2_DEVICE, DOOR_SENSOR_3_DEVICE,
                        DOOR_SENSOR_4534_DEVICE, FREEZER_1_DEVICE, FREEZER_43545_DEVICE, LIQUID_LEVEL_SENSOR_1_DEVICE,
                        LIQUID_LEVEL_SENSOR_2_DEVICE, LIQUID_LEVEL_SENSOR_3_DEVICE, LIQUID_LEVEL_SENSOR_4_DEVICE, MOTION_SENSOR_1_DEVICE,
                        OCCUPANCY_SENSOR_DEVICE, SMART_BIN_1_DEVICE, SMART_BIN_2_DEVICE, SMART_BIN_3_DEVICE, SMART_BIN_4_DEVICE,
                        SMART_SHELF_457321_DEVICE, SMART_SHELF_557322_DEVICE, SMART_SHELF_765765_DEVICE, SMOKE_SENSOR_1_DEVICE,
                        SMOKE_SENSOR_2_DEVICE, SMOKE_SENSOR_3_DEVICE, SMOKE_SENSOR_4_DEVICE, SMOKE_SENSOR_5_DEVICE, SMOKE_SENSOR_6_DEVICE)
                .forEach(d -> assertIsDisplayed(devicePage.entity(d)));

        devicePage.groupsBtn().click();
        assertIsDisplayed(devicePage.entity(SUPERMARKET_DEVICES_DEVICE_GROUP));

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersDeviceGroupsBtn(RETAIL_COMPANY_B_CUSTOMER).click();
        List.of(SMART_SHELF_89546_DEVICE, CHILLER_378876_DEVICE, FREEZER_67478, DOOR_SENSOR_3456_DEVICE)
                .forEach(d -> assertIsDisplayed(devicePage.entity(d)));

        devicePage.groupsBtn().click();
        assertIsDisplayed(devicePage.entity(SUPERMARKET_DEVICES_DEVICE_GROUP));

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersAssetGroupsBtn(RETAIL_COMPANY_A_CUSTOMER).click();
        List.of(SUPERMARKETS_S1_ASSET, SUPERMARKETS_S2_ASSET)
                .forEach(a -> assertIsDisplayed(assetPage.entity(a)));

        assetPage.groupsBtn().click();
        assertIsDisplayed(assetPage.entity(SUPERMARKETS_ASSET_GROUP));

        sideBarMenuView.goToAllCustomers();
        customerPage.manageCustomersAssetGroupsBtn(RETAIL_COMPANY_B_CUSTOMER).click();
        assertIsDisplayed(assetPage.entity(SUPERMARKETS_S3_ASSET));

        assetPage.groupsBtn().click();
        assertIsDisplayed(assetPage.entity(SUPERMARKETS_ASSET_GROUP));

        sideBarMenuView.openDeviceProfiles();
        List.of(DOOR_SENSOR_DEVICE_PROFILE, SMOKE_SENSOR_DEVICE_PROFILE, SMART_SHELF_DEVICE_PROFILE, CHILLER_DEVICE_PROFILE,
                        MOTION_SENSOR_DEVICE_PROFILE, FREEZER_DEVICE_PROFILE, SMART_BIN_DEVICE_PROFILE, OCCUPANCY_SENSOR_DEVICE_PROFILE,
                        LIQUID_LEVEL_SENSOR_DEVICE_PROFILE)
                .forEach(d -> assertIsDisplayed(devicePage.entity(d)));

        sideBarMenuView.openAssetProfiles();
        assertIsDisplayed(profilesPage.entity(SUPERMARKET_ASSET_PROFILE));

        sideBarMenuView.goToDashboardGroups();
        List.of(SUPERMARKET_USER_SHARED_DASHBOARD_GROUP, SUPERMARKET_ADMINISTRATORS_SHARED_DASHBOARD_GROUP)
                .forEach(db -> assertIsDisplayed(dashboardPage.entity(db)));

        dashboardPage.entity("All").click();
        List.of(SMART_SUPERMARKET_USER_MANAGEMENT_DASHBOARD, SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD, SMART_SUPERMARKET_DASHBOARD)
                .forEach(db -> assertIsDisplayed(dashboardPage.entity(db)));

        sideBarMenuView.goToInstances();
        assertIsDisplayed(instancesPage.entity(REMOTE_SUPERMARKET_EDGE));

        sideBarMenuView.goToRuleChainTemplates();
        assertIsDisplayed(ruleChainTemplatesPage.entity(SUPERMARKET_DEVICES_REMOTE_FACILITY_RULE_CHAIN));
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from general solution templates page)")
    public void smartRetailDeleteBtn() {
        sideBarMenuView.solutionTemplates().click();
        WebElement element = solutionTemplatesHomePage.smartRetailInstallBtn();
        testRestClient.postSmartRetail();
        refreshPage();
        scrollToElement(solutionTemplatesHomePage.smartRetail());

        assertInvisibilityOfElement(element);
        assertIsDisplayed(solutionTemplatesHomePage.smartRetailDeleteBtn());
    }

    @Test
    @Description("After install the Install button changed to the Delete button (from details solution templates page)")
    public void smartRetailDeleteBtnDetailsPage() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        WebElement element = solutionTemplateDetailsPage.installBtn();
        testRestClient.postSmartRetail();
        refreshPage();

        assertInvisibilityOfElement(element);
        assertIsDisplayed(solutionTemplateDetailsPage.deleteBtn());
    }


    @Test
    @Description("After install open instruction by click on instruction button (from general solution templates page)")
    public void smartRetailOpenInstruction() {
        testRestClient.postSmartRetail();
        sideBarMenuView.solutionTemplates().click();
        refreshPage();
        solutionTemplatesHomePage.smartRetailInstructionBtn().click();

        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark)")
    public void smartRetailCloseInstruction() {
        testRestClient.postSmartRetail();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button")
    public void smartRetailCloseInstructionByCloseBtn() {
        testRestClient.postSmartRetail();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from details solution templates page)")
    public void smartRetailInstructionGoToMainDashboard() {
        testRestClient.postSmartRetail();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboard();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP, SMART_SUPERMARKET_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("After install open instruction by click on instruction button (from details solution templates page)")
    public void smartRetailDetailsPageOpenInstruction() {
        testRestClient.postSmartRetail();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();

        assertIsDisplayed(solutionTemplatesInstalledView.solutionTemplateInstalledPopUp());
    }

    @Test
    @Description("Close instruction pop-up window installed by click on button the close (x mark) (from details solution templates page)")
    public void smartRetailDetailsPageCloseInstruction() {
        testRestClient.postSmartRetail();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.closeBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Close instruction pop-up window installed by click on close bottom button (from details solution templates page)")
    public void smartRetailDetailsPageCloseInstructionByCloseBtn() {
        testRestClient.postSmartRetail();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        WebElement element = solutionTemplatesInstalledView.solutionTemplateInstalledPopUp();
        solutionTemplatesInstalledView.bottomCloseBtn().click();

        assertInvisibilityOfElement(element);
    }

    @Test
    @Description("Redirect to main dashboard of solution template by click the 'Go to main dashboard' button (from general solution templates page)")
    public void smartRetailDetailsPageInstructionGoToMainDashboard() {
        testRestClient.postSmartRetail();
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailDetailsBtn().click();
        solutionTemplateDetailsPage.instructionBtn().click();
        solutionTemplatesInstalledView.goToMainDashboardPageBtn().click();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP, SMART_SUPERMARKET_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(getUrl()).as("Redirected URL equals to main dashboard URL")
                .isEqualTo(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId);
    }

    @Test
    @Description("Check delete entity after delete solution template")
    public void deleteEntities() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        solutionTemplatesInstalledView.closeBtn().click();
        testRestClient.deleteSmartRetail();

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.assertEntityIsNotPresent(SUPERMARKET_DEVICES_RULE_CHAIN);

        sideBarMenuView.goToRoles();
        List.of(SMART_RETAIL_READ_ONLY_ROLE, SMART_RETAIL_USER_ROLE, SMART_RETAIL_ADMINISTRATOR_ROLE)
                .forEach(r -> rolesPage.assertEntityIsNotPresent(r));

        sideBarMenuView.goToCustomerGroups();
        customerPage.assertEntityIsNotPresent(SMART_RETAIL_CUSTOMER_GROUP);

        customerPage.entity("All").click();
        List.of(RETAIL_COMPANY_A_CUSTOMER, RETAIL_COMPANY_B_CUSTOMER)
                .forEach(c -> customerPage.assertEntityIsNotPresent(c));

        sideBarMenuView.openDeviceProfiles();
        List.of(DOOR_SENSOR_DEVICE_PROFILE, SMOKE_SENSOR_DEVICE_PROFILE, SMART_SHELF_DEVICE_PROFILE, CHILLER_DEVICE_PROFILE,
                        MOTION_SENSOR_DEVICE_PROFILE, FREEZER_DEVICE_PROFILE, SMART_BIN_DEVICE_PROFILE, OCCUPANCY_SENSOR_DEVICE_PROFILE,
                        LIQUID_LEVEL_SENSOR_DEVICE_PROFILE)
                .forEach(dp -> profilesPage.assertEntityIsNotPresent(dp));

        sideBarMenuView.openAssetProfiles();
        profilesPage.assertEntityIsNotPresent(SUPERMARKET_ASSET_PROFILE);

        sideBarMenuView.goToDashboardGroups();
        List.of(SUPERMARKET_USER_SHARED_DASHBOARD_GROUP, SUPERMARKET_ADMINISTRATORS_SHARED_DASHBOARD_GROUP)
                .forEach(dg -> dashboardPage.assertEntityIsNotPresent(dg));

        dashboardPage.entity("All").click();
        List.of(SMART_SUPERMARKET_USER_MANAGEMENT_DASHBOARD, SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD, SMART_SUPERMARKET_DASHBOARD)
                .forEach(db -> dashboardPage.assertEntityIsNotPresent(db));

        sideBarMenuView.goToInstances();
        instancesPage.assertEntityIsNotPresent(REMOTE_SUPERMARKET_EDGE);

        sideBarMenuView.goToRuleChainTemplates();
        ruleChainTemplatesPage.assertEntityIsNotPresent(SUPERMARKET_DEVICES_REMOTE_FACILITY_RULE_CHAIN);
    }

    @Test
    @Description("Check redirect by click on links in instruction")
    public void linksBtn() {
        sideBarMenuView.solutionTemplates().click();
        solutionTemplatesHomePage.smartRetailInstallBtn().click();
        solutionTemplatesInstalledView.waitUntilInstallFinish();
        Set<String> urls = solutionTemplatesInstalledView.getDashboardLinks();
        String linkHttpApi = solutionTemplatesInstalledView.getHttpApiLink();
        String linkConnectionDevices = solutionTemplatesInstalledView.getConnectionDevicesLink();
        String linkThingsBoardIoTGateway = solutionTemplatesInstalledView.getThingsBoardIoTGatewayLink();
        String linkThingsBoardMQTTGateway = solutionTemplatesInstalledView.getThingsBoardMQTTGatewayLink();
        String linkThingsBoardIntegration = solutionTemplatesInstalledView.getThingsBoardIntegration();
        String dashboardId = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP,
                SMART_SUPERMARKET_DASHBOARD).getUuidId().toString();
        String dashboard1Id = getDashboardByName(EntityType.DASHBOARD, SUPERMARKET_ADMINISTRATORS_SHARED_DASHBOARD_GROUP,
                SMART_SUPERMARKET_ADMINISTRATION_DASHBOARD).getUuidId().toString();
        String entityGroupId = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_USER_SHARED_DASHBOARD_GROUP).getUuidId().toString();
        String entityGroup1Id = getEntityGroupByName(EntityType.DASHBOARD, SUPERMARKET_ADMINISTRATORS_SHARED_DASHBOARD_GROUP).getUuidId().toString();

        assertThat(urls).hasSize(2).as("Dashboard links btns redirect to two dashboards")
                .as("Redirect to " + SUPERMARKET_USER_SHARED_DASHBOARD_GROUP)
                .contains(Const.URL + "/dashboards/groups/" + entityGroupId + "/" + dashboardId)
                .as("Redirect to " + SUPERMARKET_ADMINISTRATORS_SHARED_DASHBOARD_GROUP)
                .contains(Const.URL + "/dashboards/groups/" + entityGroup1Id + "/" + dashboard1Id);
        assertThat(linkHttpApi).as("HTTP API link").isEqualTo(HTTP_API_DOCS_URL);
        assertThat(linkConnectionDevices).as("Connection devices link").isEqualTo(CONNECTIVITY_DOCS_URL);
        assertThat(linkThingsBoardIoTGateway).as("ThingsBoard IoT Gateway link").isEqualTo(THINGSBOARD_IOT_GATEWAY_DOCS_URL);
        assertThat(linkThingsBoardMQTTGateway).as("ThingsBoard MQTT Gateway link").isEqualTo(THINGSBOARD_MQTT_GATEWAY_DOCS_URL);
        assertThat(linkThingsBoardIntegration).as("ThingsBoard Integration link").isEqualTo(THINGSBOARD_INTEGRATION_DOCS_URL);
    }
}
