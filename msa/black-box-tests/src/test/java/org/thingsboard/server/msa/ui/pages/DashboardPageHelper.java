package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;

public class DashboardPageHelperAbstract extends DashboardPageElementsAbstract {
    public DashboardPageHelperAbstract(WebDriver driver) {
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
        manageAssignedEntityListField().click();
        manageAssignedEntity(title).click();
        manageAssignedUpdateBtn().click();
    }
}
