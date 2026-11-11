// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

import java.util.List;

public class SolutionTemplatesInstalledViewElements extends AbstractBasePage {
    public SolutionTemplatesInstalledViewElements(WebDriver driver) {
        super(driver);
    }

    private static final String SOLUTION_TEMPLATE_INSTALLED_POPUP = "//tb-solution-install-dialog";
    private static final String SOLUTION_TEMPLATE_INSTALLED_POPUP_CLOSE_BTN = SOLUTION_TEMPLATE_INSTALLED_POPUP + "//mat-icon[text() = 'close']/parent::button";
    private static final String SOLUTION_TEMPLATE_INSTALLED_POPUP_BOTTOM_CLOSE_BTN = SOLUTION_TEMPLATE_INSTALLED_POPUP + "//span[text()=' Close ']/parent::button";
    protected static final String SOLUTION_TEMPLATE_INSTALL_PROGRESS_POPUP = "//tb-progress-dialog";
    private static final String SOLUTION_INSTRUCTION_FIRST_PARAGRAPH_TEMPERATURE_HUMIDITY_SENSOR = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[1]";
    private static final String SOLUTION_INSTRUCTION_FIRST_PARAGRAPH_SMART_OFFICE = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[1]";
    private static final String SOLUTION_INSTRUCTION_FIRST_PARAGRAPH_FLEET_TRACKING = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[1]";
    private static final String SOLUTION_INSTRUCTION_THIRD_PARAGRAPH_TEMPERATURE_HUMIDITY_SENSOR = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[3]";
    private static final String SOLUTION_INSTRUCTION_THIRD_PARAGRAPH_SMART_OFFICE = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[3]";
    private static final String SOLUTION_INSTRUCTION_THIRD_PARAGRAPH_FLEET_TRACKING = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[3]";
    private static final String ALARM_FIRST_PARAGRAPH_TEMPERATURE_HUMIDITY_SENSOR = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[12]";
    private static final String ALARM_FIRST_PARAGRAPH_AIR_QUALITY_MONITORING = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[17]";
    private static final String CUSTOMERS_FIRST_PARAGRAPH_TEMPERATURE_HUMIDITY_SENSOR = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[13]";
    private static final String SOLUTION_INSTRUCTION_FIRST_PARAGRAPH_WATER_METERING = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[1]";
    private static final String SOLUTION_INSTRUCTION_SECOND_PARAGRAPH_WATER_METERING = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[2]";
    private static final String CUSTOMERS_FIRST_PARAGRAPH_WATER_METERING = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[17]";
    private static final String DASHBOARDS_SMART_SUPERMARKET_ADMINISTRATION_FIRST_PARAGRAPH_SMART_RETAIL
            = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[3]";
    private static final String DASHBOARDS_SMART_SUPERMARKET_FIRST_PARAGRAPH_SMART_RETAIL = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[4]";
    private static final String CUSTOMERS_SMART_SUPERMARKET_ADMINISTRATION_THIRD_PARAGRAPH_SMART_RETAIL = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[13]";
    private static final String SOLUTION_INSTRUCTION_FIRST_PARAGRAPH_SMART_IRRIGATION = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[1]";
    private static final String DASHBOARDS_FIRST_PARAGRAPH_SMART_IRRIGATION = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[2]";
    private static final String SOLUTION_INSTRUCTION_FIRST_PARAGRAPH_ASSISTED_LIVING = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[1]";
    private static final String DASHBOARDS_FIRST_PARAGRAPH_ASSISTED_LIVING = "(" + SOLUTION_TEMPLATE_INSTALLED_POPUP + "//p)[3]";
    private static final String USER1 = "(//div[contains(text(),'@')])[1]";
    private static final String USER2 = "(//div[contains(text(),'@')])[2]";
    private static final String GO_TO_MAIM_DASHBOARD_BTN = "//span[text()=' Goto main dashboard ']/ancestor::button";
    private static final String LINK_DASHBOARD_BTN = SOLUTION_TEMPLATE_INSTALLED_POPUP + "//a[contains(@href,'/dashboardGroups')]";
    private static final String LINK_GUIDE_BTN = "//a[contains(text(),'guide')]";
    private static final String LINK_HTTP_API_BTN = "//a[contains(text(),'HTTP API')]";
    private static final String LINK_CONNECTION_DEVICES_BTN = "//a[contains(text(),'connecting devices')]";
    private static final String LINK_ALARM_RULE_BTN = "//a[contains(text(),'Alarm rules')]";
    private static final String LINK_DEVICE_PROFILE_BTN = "//a[contains(text(),'device profile')]";
    private static final String LINK_ALARM_RULES_BTN = "//a[contains(text(),'Alarm rules')]";
    private static final String LINK_THINGS_BOARD_IoT_GATEWAY_BTN = "//a[contains(text(),'ThingsBoard IoT Gateway')]";
    private static final String LINK_THINGS_BOARD_MQTT_GATEWAY_BTN = "//a[contains(text(),'ThingsBoard MQTT Gateway')]";
    private static final String LINK_INTEGRATION = "//a[contains(text(),'Integrations')]";
    private static final String USERS = "//th[text()='Password']/ancestor::table//tr/td[4][contains(text(),'%s')]/preceding-sibling::td[2]";

    public WebElement solutionTemplateInstalledPopUp() {
        return waitUntilVisibilityOfElementLocated(SOLUTION_TEMPLATE_INSTALLED_POPUP);
    }

    public WebElement solutionTemplateInstallProgressPopUp() {
        return waitUntilVisibilityOfElementLocated(SOLUTION_TEMPLATE_INSTALL_PROGRESS_POPUP);
    }

    public WebElement solutionInstructionFirstParagraphTemperatureHumiditySensor() {
        return waitUntilVisibilityOfElementLocated(SOLUTION_INSTRUCTION_FIRST_PARAGRAPH_TEMPERATURE_HUMIDITY_SENSOR);
    }

    public WebElement solutionInstructionFirstParagraphSmartOffice() {
        return waitUntilVisibilityOfElementLocated(SOLUTION_INSTRUCTION_FIRST_PARAGRAPH_SMART_OFFICE);
    }

    public WebElement solutionInstructionFirstParagraphFleetTracking() {
        return waitUntilVisibilityOfElementLocated(SOLUTION_INSTRUCTION_FIRST_PARAGRAPH_FLEET_TRACKING);
    }

    public WebElement solutionInstructionThirdParagraphTemperatureHumiditySensor() {
        return waitUntilVisibilityOfElementLocated(SOLUTION_INSTRUCTION_THIRD_PARAGRAPH_TEMPERATURE_HUMIDITY_SENSOR);
    }

    public WebElement solutionInstructionThirdParagraphSmartOffice() {
        return waitUntilVisibilityOfElementLocated(SOLUTION_INSTRUCTION_THIRD_PARAGRAPH_SMART_OFFICE);
    }

    public WebElement solutionInstructionThirdParagraphFleetTracking() {
        return waitUntilVisibilityOfElementLocated(SOLUTION_INSTRUCTION_THIRD_PARAGRAPH_FLEET_TRACKING);
    }

    public WebElement alarmFirstParagraphTemperatureHumiditySensor() {
        return waitUntilVisibilityOfElementLocated(ALARM_FIRST_PARAGRAPH_TEMPERATURE_HUMIDITY_SENSOR);
    }

    public WebElement alarmFirstParagraphAirQualityMonitoring() {
        return waitUntilVisibilityOfElementLocated(ALARM_FIRST_PARAGRAPH_AIR_QUALITY_MONITORING);
    }

    public WebElement customersFirstParagraphTemperatureHumiditySensor() {
        return waitUntilVisibilityOfElementLocated(CUSTOMERS_FIRST_PARAGRAPH_TEMPERATURE_HUMIDITY_SENSOR);
    }

    public WebElement solutionInstructionFirstParagraphWaterMetering() {
        return waitUntilVisibilityOfElementLocated(SOLUTION_INSTRUCTION_FIRST_PARAGRAPH_WATER_METERING);
    }

    public WebElement solutionInstructionSecondParagraphWaterMetering() {
        return waitUntilVisibilityOfElementLocated(SOLUTION_INSTRUCTION_SECOND_PARAGRAPH_WATER_METERING);
    }

    public WebElement customersFirstParagraphWaterMetering() {
        return waitUntilVisibilityOfElementLocated(CUSTOMERS_FIRST_PARAGRAPH_WATER_METERING);
    }

    public WebElement dashboardsSmartSupermarketAdministrationFirstParagraphSmartRetail() {
        return waitUntilVisibilityOfElementLocated(DASHBOARDS_SMART_SUPERMARKET_ADMINISTRATION_FIRST_PARAGRAPH_SMART_RETAIL);
    }

    public WebElement dashboardsSmartSupermarketFirstParagraphSmartRetail() {
        return waitUntilVisibilityOfElementLocated(DASHBOARDS_SMART_SUPERMARKET_FIRST_PARAGRAPH_SMART_RETAIL);
    }

    public WebElement customersSmartSupermarketAdministrationThirdParagraphSmartRetail() {
        return waitUntilVisibilityOfElementLocated(CUSTOMERS_SMART_SUPERMARKET_ADMINISTRATION_THIRD_PARAGRAPH_SMART_RETAIL);
    }

    public WebElement solutionInstructionFirstParagraphSmartIrrigation() {
        return waitUntilVisibilityOfElementLocated(SOLUTION_INSTRUCTION_FIRST_PARAGRAPH_SMART_IRRIGATION);
    }

    public WebElement dashboardsFirstParagraphSmartIrrigation() {
        return waitUntilVisibilityOfElementLocated(DASHBOARDS_FIRST_PARAGRAPH_SMART_IRRIGATION);
    }

    public WebElement solutionInstructionFirstParagraphAssistedLiving() {
        return waitUntilVisibilityOfElementLocated(SOLUTION_INSTRUCTION_FIRST_PARAGRAPH_ASSISTED_LIVING);
    }

    public WebElement dashboardsFirstParagraphAssistedLiving() {
        return waitUntilVisibilityOfElementLocated(DASHBOARDS_FIRST_PARAGRAPH_ASSISTED_LIVING);
    }

    public WebElement closeBtn() {
        return waitUntilVisibilityOfElementLocated(SOLUTION_TEMPLATE_INSTALLED_POPUP_CLOSE_BTN);
    }

    public WebElement bottomCloseBtn() {
        return waitUntilVisibilityOfElementLocated(SOLUTION_TEMPLATE_INSTALLED_POPUP_BOTTOM_CLOSE_BTN);
    }

    public WebElement user1() {
        return waitUntilVisibilityOfElementLocated(USER1);
    }

    public WebElement user2() {
        return waitUntilVisibilityOfElementLocated(USER2);
    }

    public WebElement goToMainDashboardPageBtn() {
        return waitUntilVisibilityOfElementLocated(GO_TO_MAIM_DASHBOARD_BTN);
    }

    public List<WebElement> linkDashboardBtn() {
        return waitUntilElementsToBeClickable(LINK_DASHBOARD_BTN);
    }

    public WebElement linkGuideBtn() {
        return waitUntilElementToBeClickable(LINK_GUIDE_BTN);
    }

    public WebElement linkHttpApiBtn() {
        return waitUntilElementToBeClickable(LINK_HTTP_API_BTN);
    }

    public WebElement linkConnectionDevices() {
        return waitUntilElementToBeClickable(LINK_CONNECTION_DEVICES_BTN);
    }

    public WebElement linkAlarmRuleBtn() {
        return waitUntilElementToBeClickable(LINK_ALARM_RULE_BTN);
    }

    public WebElement linkDeviceProfileBtn() {
        return waitUntilElementToBeClickable(LINK_DEVICE_PROFILE_BTN);
    }

    public WebElement linkAlarmRulesBtn() {
        return waitUntilElementToBeClickable(LINK_ALARM_RULES_BTN);
    }

    public WebElement linkThingsBoardIoTGateway() {
        return waitUntilElementToBeClickable(LINK_THINGS_BOARD_IoT_GATEWAY_BTN);
    }

    public WebElement linkThingsBoardMQTTGateway() {
        return waitUntilElementToBeClickable(LINK_THINGS_BOARD_MQTT_GATEWAY_BTN);
    }

    public WebElement linkIntegration() {
        return waitUntilElementToBeClickable(LINK_INTEGRATION);
    }

    public List<WebElement> users(String customerName) {
        return waitUntilVisibilityOfElementsLocated(String.format(USERS, customerName));
    }
}
