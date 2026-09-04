// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class AlarmDetailsEntityTabElements extends OtherPageElements {
    public AlarmDetailsEntityTabElements(WebDriver driver) {
        super(driver);
    }

    private static final String ASSIGN_BTN = "//span[text() = '%s']/ancestor::mat-row//mat-icon[contains(text(),'keyboard_arrow_down')]/parent::button";
    private static final String USER_ASSIGN_DROPDOWN = "//div[@class='user-display-name']/span[text() = '%s']";
    protected static final String ASSIGN_USERS_DISPLAY_NAME = "//div[@class='user-display-name']/span";
    private static final String ASSIGN_USER_DISPLAY_NAME = "//span[@class='user-display-name'][contains(text(),'%s')]";
    private static final String SEARCH_FIELD = "//input[@placeholder='Search users']";
    private static final String UNASSIGNED_BTN = "//div[@role='listbox']//mat-icon[text() = 'account_circle']/following-sibling::span";
    private static final String UNASSIGNED = "//span[text() = '%s']/ancestor::mat-row//span[@class='assignee-cell']//mat-icon[text() = 'account_circle']/following-sibling::span";
    private static final String ALARM_DETAILS_BTN = "//span[text() = '%s']/ancestor::mat-row//mat-icon[contains(text(),'more_horiz')]/parent::button";
    private static final String ACCESS_FORBIDDEN_DIALOG_VIEW = "//h2[text() = 'Access Forbidden']/parent::tb-confirm-dialog";
    private static final String ALARM_ASSIGNEE_DROPDOWN = "//tb-alarm-assignee-panel";
    private static final String NO_USERS_FOUND_MESSAGE = "//div[@class='tb-not-found-content']/span";

    public WebElement assignBtn(String type) {
        return waitUntilElementToBeClickable(String.format(ASSIGN_BTN, type));
    }

    public WebElement userFromAssignDropDown(String userEmail) {
        return waitUntilElementToBeClickable(String.format(USER_ASSIGN_DROPDOWN, userEmail));
    }

    public WebElement assignedUser(String userEmail) {
        return waitUntilElementToBeClickable(String.format(ASSIGN_USER_DISPLAY_NAME, userEmail));
    }

    public List<WebElement> assignUsers() {
        return waitUntilElementsToBeClickable(ASSIGN_USERS_DISPLAY_NAME);
    }

    public WebElement searchUserField() {
        return waitUntilElementToBeClickable(SEARCH_FIELD);
    }

    public WebElement unassignedBtn() {
        return waitUntilElementToBeClickable(UNASSIGNED_BTN);
    }

    public WebElement unassigned(String alarmType) {
        return waitUntilVisibilityOfElementLocated(String.format(UNASSIGNED, alarmType));
    }

    public WebElement alarmDetailsBtn(String alarmType) {
        return waitUntilElementToBeClickable(String.format(ALARM_DETAILS_BTN, alarmType));
    }

    public WebElement accessForbiddenDialogView() {
        return waitUntilVisibilityOfElementLocated(ACCESS_FORBIDDEN_DIALOG_VIEW);
    }

    public WebElement alarmAssigneeDropdown() {
        return waitUntilVisibilityOfElementLocated(ALARM_ASSIGNEE_DROPDOWN);
    }

    public WebElement noUsersFoundMessage() {
        return waitUntilVisibilityOfElementLocated(NO_USERS_FOUND_MESSAGE);
    }
}
