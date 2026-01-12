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
