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

public class AlarmWidgetElements extends AlarmDetailsEntityTabHelper {
    public AlarmWidgetElements(WebDriver driver) {
        super(driver);
    }

    private static final String ASSIGN_USER_DISPLAY_NAME = "//span[contains(@class,'assigned-container')]/span[contains(text(),'%s')]";
    private static final String UNASSIGNED = "//span[text() = '%s']/ancestor::mat-row//span[contains(@class,'assigned-container')]" +
            "//mat-icon[text() = 'account_circle']/following-sibling::span";

    @Override
    public WebElement assignedUser(String userEmail) {
        return waitUntilElementToBeClickable(String.format(ASSIGN_USER_DISPLAY_NAME, userEmail));
    }

    @Override
    public WebElement unassigned(String alarmType) {
        return waitUntilVisibilityOfElementLocated(String.format(UNASSIGNED, alarmType));
    }
}
