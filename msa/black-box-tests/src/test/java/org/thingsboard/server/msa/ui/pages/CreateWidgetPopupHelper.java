// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

public class CreateWidgetPopupHelper extends CreateWidgetPopupElements {
    public CreateWidgetPopupHelper(WebDriver driver) {
        super(driver);
    }

    public void goToCreateEntityAliasPopup(String aliasName) {
        entityAlias().sendKeys(aliasName + RandomStringUtils.randomAlphanumeric(7));
        createNewAliasBtn().click();
    }

    public void selectFilterType(String filterType) {
        filterTypeFiled().click();
        optionFromDropdown(filterType).click();
    }

    public void selectType(String entityType) {
        typeFiled().click();
        optionFromDropdown(entityType).click();
    }

    public void selectEntity(String entityName) {
        entityFiled().sendKeys(entityName);
        entityFromDropdown(entityName);
        entityFiled().sendKeys(Keys.ARROW_DOWN);
        entityFiled().sendKeys(Keys.ENTER);
    }
}
