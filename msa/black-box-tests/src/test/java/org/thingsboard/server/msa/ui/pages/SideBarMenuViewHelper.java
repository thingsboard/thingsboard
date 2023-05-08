/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

public class SideBarMenuViewHelper extends SideBarMenuViewElements {
    public SideBarMenuViewHelper(WebDriver driver) {
        super(driver);
    }

    public void openDeviceProfiles() {
        profilesBtn().click();
        deviceProfileBtn().click();
    }

    public void openAssetProfiles() {
        profilesBtn().click();
        assetProfileBtn().click();
    }

    public void goToDevicesPage() {
        openEntitiesDropdown();
        devicesBtn().click();
    }

    public void goToAssetsPage() {
        openEntitiesDropdown();
        assetsBtn().click();
    }

    public void goToEntityViewsPage() {
        openEntitiesDropdown();
        entityViewsBtn().click();
    }

    public void openEntitiesDropdown() {
        if (entitiesDropdownIsClose()) {
            entitiesDropdown().click();
        }
    }

    public boolean entitiesDropdownIsClose() {
        return dropdownIsClose(entitiesDropdown());
    }

    private boolean dropdownIsClose(WebElement dropdown) {
        return !dropdown.getAttribute("class").contains("tb-toggled");
    }
}