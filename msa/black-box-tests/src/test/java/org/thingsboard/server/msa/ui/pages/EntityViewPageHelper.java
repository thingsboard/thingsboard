// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;

public class EntityViewPageHelper extends EntityViewPageElements {
    public EntityViewPageHelper(WebDriver driver) {
        super(driver);
    }

    public void openEntityViewAlarms(String customerName) {
        if (!entityViewDetailsView().isDisplayed()) {
            entity(customerName).click();
        }
        entityViewDetailsAlarmsBtn().click();
    }

}
