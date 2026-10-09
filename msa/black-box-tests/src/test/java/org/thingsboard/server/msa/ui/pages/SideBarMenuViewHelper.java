// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class SideBarMenuViewHelper extends SideBarMenuViewElements {
    public SideBarMenuViewHelper(WebDriver driver) {
        super(driver);
    }

    public void openDeviceProfiles() {
        openProfilesDropdown();
        deviceProfileBtn().click();
    }

    public void openAssetProfiles() {
        openProfilesDropdown();
        assetProfileBtn().click();
    }

    public void goToAllCustomers() {
        customersBtn().click();
    }

    public void goToCustomerGroups() {
        goToAllCustomers();
        new CustomerPageHelper(driver).groupsBtn().click();
    }

    public void goToAllDevices() {
        openEntitiesDropdown();
        devicesBtn().click();
    }

    public void goToDeviceGroups() {
        goToAllDevices();
        new DevicePageElements(driver).groupsBtn().click();
    }

    public void goToAllAssets() {
        openEntitiesDropdown();
        assetsBtn().click();
    }

    public void goToAssetGroups() {
        goToAllAssets();
        new AssetPageElements(driver).groupsBtn().click();
    }

    public void goToAllDashboards() {
        dashboardsBtn().click();
    }

    public void goToDashboardGroups() {
        goToAllDashboards();
        new DashboardPageHelper(driver).groupsBtn().click();
    }

    public void goToRoles() {
        openSecurityDropdown();
        rolesBtn().click();
    }

    public void goToScheduler() {
        openAdvancedFeaturesDropdown();
        schedulerBtn().click();
    }

    public void goToInstances() {
        openEdgeManagementDropdown();
        instancesBtn().click();
    }

    public void goToRuleChainTemplates() {
        openEdgeManagementDropdown();
        ruleChainTemplatesBtn().click();
    }

    public void openEntitiesDropdown() {
        if (entitiesDropdownIsClose()) {
            entitiesDropdown().click();
        }
    }

    public void openSecurityDropdown() {
        if (securityDropdownIsClose()) {
            securityDropdown().click();
        }
    }

    public void openProfilesDropdown() {
        if (profilesDropdownIsClose()) {
            profilesDropdown().click();
        }
    }

    public void openAdvancedFeaturesDropdown() {
        if (advancedFeaturesDropdownIsClose()) {
            advancedFeaturesDropdown().click();
        }
    }

    public void openEdgeManagementDropdown() {
        if (edgeManagementDropdownIsClose()) {
            edgeManagementDropdown().click();
        }
    }

    public boolean entitiesDropdownIsClose() {
        return dropdownIsClose(entitiesDropdown());
    }

    public boolean securityDropdownIsClose() {
        return dropdownIsClose(securityDropdown());
    }

    public boolean profilesDropdownIsClose() {
        return dropdownIsClose(profilesDropdown());
    }

    public boolean advancedFeaturesDropdownIsClose() {
        return dropdownIsClose(advancedFeaturesDropdown());
    }

    public boolean edgeManagementDropdownIsClose() {
        return dropdownIsClose(edgeManagementDropdown());
    }

    private boolean dropdownIsClose(WebElement dropdown) {
        return !dropdown.getAttribute("class").contains("tb-toggled");
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

}
