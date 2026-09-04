// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class DashboardPageElements extends OtherPageElementsHelper {
    public DashboardPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String TITLES = "//mat-cell[contains(@class,'cdk-column-title')]/span";
    private static final String ASSIGNED_BTN = ENTITY + "/../..//mat-icon[contains(text(),' assignment_ind')]/../..";
    private static final String MANAGE_ASSIGNED_ENTITY_LIST_FIELD = "//input[@formcontrolname='entity']";
    private static final String MANAGE_ASSIGNED_ENTITY = "//mat-option//span[contains(text(),'%s')]";
    private static final String MANAGE_ASSIGNED_UPDATE_BTN = "//button[@type='submit']";
    private static final String EDIT_BTN = "//mat-icon[text() = 'edit']/parent::button[@mat-stroked-button]";
    private static final String ADD_BTN = "//mat-fab-actions//mat-icon[text() = 'add']/parent::button";
    private static final String ALARM_WIDGET_BUNDLE = "//mat-card-title[text() = 'Alarm widgets']/ancestor::mat-card";
    private static final String ALARM_TABLE_WIDGET = "//img[@alt='Alarms table']/ancestor::mat-card";
    private static final String WIDGET_SE_CORNER = "//div[contains(@class,'handle-se')]";
    private static final String SAVE_BTN = "//mat-icon[text() = 'done']/parent::button[@fxhide.lt-lg]";

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

    public WebElement editBtn() {
        return waitUntilElementToBeClickable(EDIT_BTN);
    }

    public WebElement addBtn() {
        return waitUntilElementToBeClickable(ADD_BTN);
    }

    public WebElement alarmWidgetBundle() {
        return waitUntilElementToBeClickable(ALARM_WIDGET_BUNDLE);
    }

    public WebElement alarmTableWidget() {
        return waitUntilElementToBeClickable(ALARM_TABLE_WIDGET);
    }

    public WebElement widgetSECorner() {
        return waitUntilElementToBeClickable(WIDGET_SE_CORNER);
    }

    public WebElement saveBtn() {
        return waitUntilVisibilityOfElementLocated(SAVE_BTN);
    }
}
