// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class EntityViewPageElements extends OtherPageElementsHelper {
    public EntityViewPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String ENTITY_VIEW_DETAILS_VIEW = "//tb-details-panel";
    private static final String ENTITY_VIEW_DETAILS_ALARMS = ENTITY_VIEW_DETAILS_VIEW + "//span[text()='Alarms']";

    public WebElement entityViewDetailsView() {
        return waitUntilPresenceOfElementLocated(ENTITY_VIEW_DETAILS_VIEW);
    }

    public WebElement entityViewDetailsAlarmsBtn() {
        return waitUntilElementToBeClickable(ENTITY_VIEW_DETAILS_ALARMS);
    }

}
