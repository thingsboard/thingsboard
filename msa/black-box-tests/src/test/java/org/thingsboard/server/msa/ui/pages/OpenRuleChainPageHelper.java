// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class OpenRuleChainPageHelper extends OpenRuleChainPageElements {
    public OpenRuleChainPageHelper(WebDriver driver) {
        super(driver);
    }

    private String headName;

    public void setHeadName() {
        this.headName = headRuleChainName().getText().split(" ")[1];
    }

    public String getHeadName() {
        return headName;
    }

    public void waitUntilBtnDisable(WebElement element) {
        waitUntilAttributeContains(element, "disabled", "true");
    }
}
