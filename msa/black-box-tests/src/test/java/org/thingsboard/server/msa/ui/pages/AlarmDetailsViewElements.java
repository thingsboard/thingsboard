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
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

public class AlarmDetailsViewElements extends AbstractBasePage {
    public AlarmDetailsViewElements(WebDriver driver) {
        super(driver);
    }

    private static final String ASSIGN_FIELD = "//mat-label[text()='Assignee']/parent::label/parent::div//input";
    private static final String USER_FROM_DROP_DOWN = "//div[@class='user-display-name']/span[text() = '%s']";
    private static final String CLOSE_ALARM_DETAILS_VIEW_BTN = "//mat-dialog-container//mat-icon[contains(text(),'close')]/parent::button";
    private static final String UNASSIGNED_BTN = "//div[@role='listbox']//mat-icon[text() = 'account_circle']/following-sibling::span";

    public WebElement assignField() {
        return waitUntilElementToBeClickable(ASSIGN_FIELD);
    }

    public WebElement userFromAssignDropdown(String emailOrName) {
        return waitUntilElementToBeClickable(String.format(USER_FROM_DROP_DOWN, emailOrName));
    }

    public WebElement closeAlarmDetailsViewBtn() {
        return waitUntilElementToBeClickable(CLOSE_ALARM_DETAILS_VIEW_BTN);
    }

    public WebElement unassignedBtn() {
        return waitUntilElementToBeClickable(UNASSIGNED_BTN);
    }
}
