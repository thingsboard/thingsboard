// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class SchedulerPageElements extends OtherPageElements {
    public SchedulerPageElements(WebDriver driver) {
        super(driver);
    }

    protected static final String SCHEDULER = "//mat-row//mat-cell[contains(text(),'%s')]";

    public List<WebElement> schedulers(String schedulerName) {
        return waitUntilElementsToBeClickable(String.format(SCHEDULER, schedulerName));
    }

    public WebElement scheduler(String schedulerName) {
        return waitUntilElementToBeClickable(String.format(SCHEDULER, schedulerName));
    }
}
