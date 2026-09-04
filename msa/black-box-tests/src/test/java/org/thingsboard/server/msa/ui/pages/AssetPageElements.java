// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class AssetPageElements extends OtherPageElements {
    public AssetPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String ASSET_DETAILS_VIEW = "//tb-details-panel";
    private static final String ASSET_DETAILS_ALARMS = ASSET_DETAILS_VIEW + "//span[text()='Alarms']";

    public WebElement assetDetailsView() {
        return waitUntilPresenceOfElementLocated(ASSET_DETAILS_VIEW);
    }

    public WebElement assetDetailsAlarmsBtn() {
        return waitUntilElementToBeClickable(ASSET_DETAILS_ALARMS);
    }

}
