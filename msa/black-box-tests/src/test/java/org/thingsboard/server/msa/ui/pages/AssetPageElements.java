// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class AssetPageElements extends OtherPageElementsHelper {
    public AssetPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String ALL_GROUP_NAMES = "//mat-icon[contains(text(),'check')]/ancestor::mat-row/mat-cell[contains(@class,'name')]/span";
    private static final String ALL_NAMES = "//mat-cell[contains(@class,'cdk-column-column1')]/span";
    private static final String GROUPS_BTN = "//a[contains(@href,'/entities/assets/groups')]/span[@class='mdc-tab__content']";
    private static final String ASSET_DETAILS_VIEW = "//tb-details-panel";
    private static final String ASSET_DETAILS_ALARMS = ASSET_DETAILS_VIEW + "//span[text()='Alarms']";

    public List<WebElement> allGroupNames() {
        return waitUntilElementsToBeClickable(ALL_GROUP_NAMES);
    }

    public List<WebElement> allNames() {
        return waitUntilElementsToBeClickable(ALL_NAMES);
    }

    public WebElement groupsBtn() {
        return waitUntilElementToBeClickable(GROUPS_BTN);
    }

    public WebElement assetDetailsView() {
        return waitUntilPresenceOfElementLocated(ASSET_DETAILS_VIEW);
    }

    public WebElement assetDetailsAlarmsBtn() {
        return waitUntilElementToBeClickable(ASSET_DETAILS_ALARMS);
    }
}
