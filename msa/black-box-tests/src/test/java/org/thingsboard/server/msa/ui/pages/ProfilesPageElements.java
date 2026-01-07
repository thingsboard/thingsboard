/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class ProfilesPageElements extends OtherPageElementsHelper {
    public ProfilesPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String CREATE_DEVICE_PROFILE_BTN = "//span[text()='Create new device profile']";
    private static final String CREATE_ASSET_PROFILE_BTN = "//span[text()='Create new asset profile']";
    private static final String IMPORT_DEVICE_PROFILE_BTN = "//span[text()='Import device profile']";
    private static final String IMPORT_ASSET_PROFILE_BTN = "//span[text()='Import asset profile']";
    private static final String ADD_DEVICE_PROFILE_VIEW = "//tb-add-device-profile-dialog";
    private static final String ADD_ASSET_PROFILE_VIEW = "//tb-add-entity-dialog";
    private static final String DEVICE_PROFILE_VIEW = "//tb-entity-details-panel";
    private static final String NAME_FIELD = "//input[@formcontrolname='name']";
    private static final String RULE_CHAIN_FIELD = "//input[@formcontrolname='ruleChainId']";
    private static final String DASHBOARD_FIELD = "//input[@formcontrolname='dashboard']";
    private static final String QUEUE_FIELD = "//input[@formcontrolname='queueName']";
    private static final String DESCRIPTION_FIELD = "//textarea[@formcontrolname='description']";
    private static final String ADD_DEVICE_PROFILE_ADD_BTN = "//span[text()='Add']";
    private static final String ADD_ASSET_PROFILE_ADD_BTN = "//button[@type='submit']";
    private static final String DEVICE_PROFILE_VIEW_DELETE_BTN = "//tb-device-profile//span[contains(text(),'Delete')]";
    private static final String ASSET_PROFILE_VIEW_DELETE_BTN = "//tb-entity-details-panel//span[contains(text(),'Delete')]";
    private static final String PROFILE_NAMES = "//tbody/mat-row/mat-cell[contains(@class,'name')]";
    private static final String MAKE_DEFAULT_BTN = ENTITY + "/../..//mat-icon[contains(text(),' flag')]/../..";
    private static final String DEFAULT = ENTITY + "/../..//mat-icon[text() = 'check_box']";
    private static final String DEVICE_PROFILE_VIEW_MAKE_DEFAULT_BTN = "//span[text() = ' Make device profile default ']/..";
    private static final String ASSET_PROFILE_VIEW_MAKE_DEFAULT_BTN = "//span[text() = ' Make asset profile default ']/..";

    protected String getDeviseProfileViewDeleteBtn() {
        return DEVICE_PROFILE_VIEW_DELETE_BTN;
    }

    protected String getAssetProfileViewDeleteBtn() {
        return ASSET_PROFILE_VIEW_DELETE_BTN;
    }

    public WebElement createNewDeviceProfileBtn() {
        return waitUntilElementToBeClickable(CREATE_DEVICE_PROFILE_BTN);
    }

    public WebElement createNewAssetProfileBtn() {
        return waitUntilElementToBeClickable(CREATE_ASSET_PROFILE_BTN);
    }

    public WebElement importDeviceProfileBtn() {
        return waitUntilElementToBeClickable(IMPORT_DEVICE_PROFILE_BTN);
    }

    public WebElement importAssetProfileBtn() {
        return waitUntilElementToBeClickable(IMPORT_ASSET_PROFILE_BTN);
    }

    public WebElement addDeviceProfileView() {
        return waitUntilElementToBeClickable(ADD_DEVICE_PROFILE_VIEW);
    }

    public WebElement addAssetProfileView() {
        return waitUntilElementToBeClickable(ADD_ASSET_PROFILE_VIEW);
    }

    public WebElement addDeviceProfileNameField() {
        return waitUntilElementToBeClickable(ADD_DEVICE_PROFILE_VIEW + NAME_FIELD);
    }

    public WebElement addAssetProfileNameField() {
        return waitUntilElementToBeClickable(ADD_ASSET_PROFILE_VIEW + NAME_FIELD);
    }

    public WebElement profileViewNameField() {
        return waitUntilVisibilityOfElementLocated(DEVICE_PROFILE_VIEW + NAME_FIELD);
    }

    public WebElement addDeviceProfileRuleChainField() {
        return waitUntilElementToBeClickable(ADD_DEVICE_PROFILE_VIEW + RULE_CHAIN_FIELD);
    }

    public WebElement addAssetProfileRuleChainField() {
        return waitUntilElementToBeClickable(ADD_ASSET_PROFILE_VIEW + RULE_CHAIN_FIELD);
    }

    public WebElement profileViewRuleChainField() {
        return waitUntilVisibilityOfElementLocated(DEVICE_PROFILE_VIEW + RULE_CHAIN_FIELD);
    }

    public WebElement addDeviceProfileMobileDashboardField() {
        return waitUntilElementToBeClickable(ADD_DEVICE_PROFILE_VIEW + DASHBOARD_FIELD);
    }

    public WebElement addAssetProfileMobileDashboardField() {
        return waitUntilElementToBeClickable(ADD_ASSET_PROFILE_VIEW + DASHBOARD_FIELD);
    }

    public WebElement profileViewMobileDashboardField() {
        return waitUntilVisibilityOfElementLocated(DEVICE_PROFILE_VIEW + DASHBOARD_FIELD);
    }

    public WebElement addDeviceProfileQueueField() {
        return waitUntilElementToBeClickable(ADD_DEVICE_PROFILE_VIEW + QUEUE_FIELD);
    }

    public WebElement addAssetProfileQueueField() {
        return waitUntilElementToBeClickable(ADD_ASSET_PROFILE_VIEW + QUEUE_FIELD);
    }

    public WebElement profileViewQueueField() {
        return waitUntilVisibilityOfElementLocated(DEVICE_PROFILE_VIEW + QUEUE_FIELD);
    }

    public WebElement addDeviceDescriptionField() {
        return waitUntilElementToBeClickable(ADD_DEVICE_PROFILE_VIEW + DESCRIPTION_FIELD);
    }

    public WebElement addAssetDescriptionField() {
        return waitUntilElementToBeClickable(ADD_ASSET_PROFILE_VIEW + DESCRIPTION_FIELD);
    }

    public WebElement profileViewDescriptionField() {
        return waitUntilVisibilityOfElementLocated(DEVICE_PROFILE_VIEW + DESCRIPTION_FIELD);
    }

    public WebElement addDeviceProfileAddBtn() {
        return waitUntilElementToBeClickable(ADD_DEVICE_PROFILE_ADD_BTN);
    }

    public WebElement addAssetProfileAddBtn() {
        return waitUntilElementToBeClickable(ADD_ASSET_PROFILE_ADD_BTN);
    }

    public WebElement deviceProfileViewDeleteBtn() {
        return waitUntilElementToBeClickable(DEVICE_PROFILE_VIEW_DELETE_BTN);
    }

    public WebElement assetProfileViewDeleteBtn() {
        return waitUntilElementToBeClickable(ASSET_PROFILE_VIEW_DELETE_BTN);
    }

    public List<WebElement> profileNames() {
        return waitUntilElementsToBeClickable(PROFILE_NAMES);
    }

    public WebElement makeProfileDefaultBtn(String profileName) {
        return waitUntilElementToBeClickable(String.format(MAKE_DEFAULT_BTN, profileName));
    }

    public WebElement defaultCheckbox(String profileName) {
        return waitUntilElementToBeClickable(String.format(DEFAULT, profileName));
    }

    public WebElement deviceProfileViewMakeDefaultBtn() {
        return waitUntilElementToBeClickable(DEVICE_PROFILE_VIEW_MAKE_DEFAULT_BTN);
    }

    public WebElement assetProfileViewMakeDefaultBtn() {
        return waitUntilElementToBeClickable(ASSET_PROFILE_VIEW_MAKE_DEFAULT_BTN);
    }
}