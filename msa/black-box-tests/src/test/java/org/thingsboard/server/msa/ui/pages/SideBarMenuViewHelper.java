// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SideBarMenuViewHelper extends SideBarMenuViewElements {
    public SideBarMenuViewHelper(WebDriver driver) {
        super(driver);
    }

    public void openDeviceProfiles() {
        openProfilesDropDown();
        deviceProfileBtn().click();
    }

    public void openAssetProfiles() {
        openProfilesDropDown();
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

    public void openProfilesDropDown() {
        if (profilesIsClose()) {
            profilesDropdown().click();
        }
    }

    public boolean entitiesDropdownIsClose() {
        return dropdownIsClose(entitiesDropdown());
    }

    public boolean profilesIsClose() {
        return dropdownIsClose(profilesDropdown());
    }

    private boolean dropdownIsClose(WebElement dropdown) {
        return !dropdown.getAttribute("class").contains("tb-toggled");
    }
}