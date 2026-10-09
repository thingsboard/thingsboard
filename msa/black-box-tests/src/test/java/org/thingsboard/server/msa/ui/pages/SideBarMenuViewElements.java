// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

public class SideBarMenuViewElements extends AbstractBasePage {
    public SideBarMenuViewElements(WebDriver driver) {
        super(driver);
    }

    private static final String RULE_CHAINS_BTN = "//mat-toolbar//a[@href='/ruleChains']";
    private static final String CUSTOMERS_BTN = "//mat-toolbar//a[@href='/customers']";
    private static final String DASHBOARD_BTN = "//mat-toolbar//a[@href='/dashboards']";
    private static final String PROFILES_DROPDOWN = "//mat-toolbar//mat-icon[text()='badge']/ancestor::span//span[contains(@class,'pull-right')]";
    private static final String DEVICE_PROFILE_BTN = "//mat-toolbar//a[@href='/profiles/deviceProfiles']";
    private static final String ASSET_PROFILE_BTN = "//mat-toolbar//a[@href='/profiles/assetProfiles']";
    private static final String DASHBOARDS_BTN = "//mat-toolbar//a[@href='/dashboards']";
    private static final String SOLUTION_TEMPLATES = "//mat-toolbar//a[@href='/solutionTemplates']";
    private static final String ROLES_BTN = "//mat-toolbar//a[@href='/security-settings/roles']/span[@class='mdc-button__label']";
    private static final String DEVICES_BTN = "//ul[@id='docs-menu-entity.entities']//span[text()='Devices']";
    private static final String ASSETS_BTN = "//ul[@id='docs-menu-entity.entities']//span[text()='Assets']";
    private static final String ENTITIES_DROPDOWN = "//mat-toolbar//mat-icon[text()='category']/ancestor::span//span[contains(@class,'pull-right')]";
    private static final String SECURITY_DROPDOWN = "//mat-toolbar//mat-icon[text()='security']/ancestor::span//span[contains(@class,'pull-right')]";
    private static final String ADVANCED_FEATURES_DROPDOWN = "//mat-toolbar//mat-icon[text()='construction']/ancestor::span//span[contains(@class,'pull-right')]";
    private static final String SCHEDULER_BTN = "//mat-toolbar//span[text()='Scheduler']";
    private static final String EDGE_MANAGEMENT_DROPDOWN = "//mat-toolbar//mat-icon[text()='settings_input_antenna']" +
            "/ancestor::span//span[contains(@class,'pull-right')]";
    private static final String INSTANCES_BTN = "//mat-toolbar//span[text()='Instances']";
    private static final String RULE_CHAIN_TEMPLATES_BTN = "//mat-sidenav//span[text() = 'Rule chain templates']";
    private static final String ALARMS_BTN = "//mat-toolbar//a[@href='/alarms']";
    private static final String ENTITY_VIEWS_BTN = "//ul[@id='docs-menu-entity.entities']//span[text()='Entity Views']";

    public WebElement ruleChainsBtn() {
        return waitUntilElementToBeClickable(RULE_CHAINS_BTN);
    }

    public WebElement customersBtn() {
        return waitUntilElementToBeClickable(CUSTOMERS_BTN);
    }

    public WebElement profilesDropdown() {
        return waitUntilElementToBeClickable(PROFILES_DROPDOWN);
    }

    public WebElement deviceProfileBtn() {
        return waitUntilElementToBeClickable(DEVICE_PROFILE_BTN);
    }

    public WebElement assetProfileBtn() {
        return waitUntilElementToBeClickable(ASSET_PROFILE_BTN);
    }

    public WebElement dashboardsBtn() {
        return waitUntilElementToBeClickable(DASHBOARDS_BTN);
    }

    public WebElement solutionTemplates() {
        return waitUntilElementToBeClickable(SOLUTION_TEMPLATES);
    }

    public WebElement rolesBtn() {
        return waitUntilElementToBeClickable(ROLES_BTN);
    }

    public WebElement devicesBtn() {
        return waitUntilElementToBeClickable(DEVICES_BTN);
    }

    public WebElement assetsBtn() {
        return waitUntilElementToBeClickable(ASSETS_BTN);
    }

    public WebElement entitiesDropdown() {
        return waitUntilElementToBeClickable(ENTITIES_DROPDOWN);
    }

    public WebElement securityDropdown() {
        return waitUntilElementToBeClickable(SECURITY_DROPDOWN);
    }

    public WebElement advancedFeaturesDropdown() {
        return waitUntilElementToBeClickable(ADVANCED_FEATURES_DROPDOWN);
    }

    public WebElement schedulerBtn() {
        return waitUntilElementToBeClickable(SCHEDULER_BTN);
    }

    public WebElement edgeManagementDropdown() {
        return waitUntilElementToBeClickable(EDGE_MANAGEMENT_DROPDOWN);
    }

    public WebElement instancesBtn() {
        return waitUntilElementToBeClickable(INSTANCES_BTN);
    }

    public WebElement ruleChainTemplatesBtn() {
        return waitUntilElementToBeClickable(RULE_CHAIN_TEMPLATES_BTN);
    }

    public WebElement alarmsBtn() {
        return waitUntilElementToBeClickable(ALARMS_BTN);
    }

    public WebElement entityViewsBtn() {
        return waitUntilElementToBeClickable(ENTITY_VIEWS_BTN);
    }
}
