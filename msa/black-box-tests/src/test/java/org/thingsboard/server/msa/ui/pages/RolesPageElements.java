// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class RolesPageElements extends OtherPageElementsHelper {
    public RolesPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String ALL_NAMES = "//mat-cell[contains(@class,'name')]/span";

    public List<WebElement> allNames() {
        return waitUntilElementsToBeClickable(ALL_NAMES);
    }
}
