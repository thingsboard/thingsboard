package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class DashboardPageElementsAbstract extends OtherPageElementsHelperAbstract {
    public DashboardPageElementsAbstract(WebDriver driver) {
        super(driver);
    }

    private static final String TITLES = "//mat-cell[contains(@class,'cdk-column-title')]/span";
    private static final String ASSIGNED_BTN = ENTITY + "/../..//mat-icon[contains(text(),' assignment_ind')]/../..";
    private static final String MANAGE_ASSIGNED_ENTITY_LIST_FIELD = "//input[@formcontrolname='entity']";
    private static final String MANAGE_ASSIGNED_ENTITY = "//mat-option//span[contains(text(),'%s')]";
    private static final String MANAGE_ASSIGNED_UPDATE_BTN = "//button[@type='submit']";

    public List<WebElement> entityTitles() {
        return waitUntilVisibilityOfElementsLocated(TITLES);
    }

    public WebElement assignedBtn(String title) {
        return waitUntilElementToBeClickable(String.format(ASSIGNED_BTN, title));
    }

    public WebElement manageAssignedEntityListField() {
        return waitUntilElementToBeClickable(MANAGE_ASSIGNED_ENTITY_LIST_FIELD);
    }

    public WebElement manageAssignedEntity(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_ASSIGNED_ENTITY, title));
    }

    public WebElement manageAssignedUpdateBtn() {
        return waitUntilElementToBeClickable(MANAGE_ASSIGNED_UPDATE_BTN);
    }
}
