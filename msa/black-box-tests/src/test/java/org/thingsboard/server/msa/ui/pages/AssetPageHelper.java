// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;

public class AssetPageHelper extends AssetPageElements {
    public AssetPageHelper(WebDriver driver) {
        super(driver);
    }

    public void openAssetAlarms(String assetName) {
        if (!assetDetailsView().isDisplayed()) {
            entity(assetName).click();
        }
        assetDetailsAlarmsBtn().click();
    }

}
