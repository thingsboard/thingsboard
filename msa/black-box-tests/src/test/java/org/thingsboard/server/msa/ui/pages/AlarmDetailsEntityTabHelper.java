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
