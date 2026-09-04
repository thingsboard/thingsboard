// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

public class CreateWidgetPopupElements extends AbstractBasePage {
    public CreateWidgetPopupElements(WebDriver driver) {
        super(driver);
    }

    private static final String ENTITY_ALIAS = "//input[@formcontrolname='entityAlias']";
    private static final String CREATE_NEW_ALIAS_BTN = "//a[text() = 'Create a new one!']/parent::span";
    private static final String FILTER_TYPE_FIELD = "//div[contains(@class,'tb-entity-filter')]//mat-select//span";
    private static final String TYPE_FIELD = "//mat-select[@formcontrolname='entityType']//span";
    private static final String OPTION_FROM_DROPDOWN = "//span[text() = ' %s ']";
    private static final String ENTITY_FIELD = "//input[@formcontrolname='entity']";
    private static final String ADD_ALIAS_BTN = "//tb-entity-alias-dialog//span[text() = ' Add ']/parent::button";
    private static final String ADD_WIDGET_BTN = "//tb-add-widget-dialog//span[text() = ' Add ']/parent::button";
    private static final String ENTITY_FROM_DROPDOWN = "//b[text() = '%s']";

    public WebElement entityAlias() {
        return waitUntilElementToBeClickable(ENTITY_ALIAS);
    }

    public WebElement createNewAliasBtn() {
        return waitUntilElementToBeClickable(CREATE_NEW_ALIAS_BTN);
    }

    public WebElement filterTypeFiled() {
        return waitUntilElementToBeClickable(FILTER_TYPE_FIELD);
    }

    public WebElement typeFiled() {
        return waitUntilElementToBeClickable(TYPE_FIELD);
    }

    public WebElement optionFromDropdown(String type) {
        return waitUntilElementToBeClickable(String.format(OPTION_FROM_DROPDOWN, type));
    }

    public WebElement entityFiled() {
        return waitUntilElementToBeClickable(ENTITY_FIELD);
    }

    public WebElement addAliasBtn() {
        return waitUntilElementToBeClickable(ADD_ALIAS_BTN);
    }

    public WebElement addWidgetBtn() {
        return waitUntilElementToBeClickable(ADD_WIDGET_BTN);
    }

    public WebElement entityFromDropdown(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(ENTITY_FROM_DROPDOWN, entityName));
    }
}
