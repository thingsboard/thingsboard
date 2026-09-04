// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;

public class DashboardPageHelper extends DashboardPageElements {
    public DashboardPageHelper(WebDriver driver) {
        super(driver);
    }

    private String dashboardTitle;

    public void setDashboardTitle() {
        this.dashboardTitle = entityTitles().get(0).getText();
    }

    public String getDashboardTitle() {
        return dashboardTitle;
    }

    public void assignedCustomer(String title) {
        jsClick(manageAssignedEntityListField());
        jsClick(manageAssignedEntity(title));
        jsClick(manageAssignedUpdateBtn());
    }

    public void openSelectWidgetsBundleMenu() {
        addBtn().click();
    }

    public void openCreateWidgetPopup() {
        alarmWidgetBundle().click();
        alarmTableWidget().click();
    }

    public void increaseSizeOfTheWidget() {
        pull(widgetSECorner(), 700, 200);
    }
}
