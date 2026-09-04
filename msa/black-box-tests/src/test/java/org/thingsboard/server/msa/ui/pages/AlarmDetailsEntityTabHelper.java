// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.stream.Collectors;

public class AlarmDetailsEntityTabHelper extends AlarmDetailsEntityTabElements {
    public AlarmDetailsEntityTabHelper(WebDriver driver) {
        super(driver);
    }

    public void assignAlarmTo(String alarmType, String user) {
        jsClick(assignBtn(alarmType));
        userFromAssignDropDown(user).click();
    }

    public void unassignedAlarm(String alarmType) {
        jsClick(assignBtn(alarmType));
        unassignedBtn().click();
    }

    public void searchAlarm(String alarmType, String emailOrName) {
        jsClick(assignBtn(alarmType));
        searchUserField().sendKeys(emailOrName);
    }

    private List<String> users;

    public void setUsers() {
        users = assignUsers()
                .stream()
                .map(WebElement::getText)
                .collect(Collectors.toList());
    }

    public List<String> getUsers() {
        return users;
    }

    public void assertUsersForAssignIsNotPresent() {
        sleep(1);
        elementsIsNotPresent(ASSIGN_USERS_DISPLAY_NAME);
    }
}
