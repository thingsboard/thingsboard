/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
import org.thingsboard.server.msa.ui.base.BasePage;

import java.util.List;

public class OtherPageElements extends BasePage {
    public OtherPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String ENTITY_COUNT = "//div[@class='mat-paginator-range-label']";
    private static final String WARNING_DELETE_POPUP_YES = "//tb-confirm-dialog//button[2]";
    private static final String REFRESH_BTN = "//mat-icon[contains(text(),'refresh')]/..";
    private static final String HELP_BTN = "//mat-icon[contains(text(),'help')]/..";
    private static final String CHECKBOX = "//mat-row//span[contains(text(),'%s')]/../..//mat-checkbox";
    private static final String CHECKBOXES = "//tbody//mat-checkbox";
    private static final String DELETE_SELECTED_BTN = "//span[contains(text(),'selected')]//..//mat-icon/../..";
    private static final String DELETE_BTNS = "//mat-icon[contains(text(),' delete')]/../..";
    private static final String MARKS_CHECKBOX = "//mat-row[contains (@class,'mat-selected')]//mat-checkbox[contains(@class, 'checked')]";
    private static final String SELECT_ALL_CHECKBOX = "//thead//mat-checkbox";
    private static final String ALL_ENTITY = "//mat-row[@class='mat-row cdk-row mat-row-select ng-star-inserted']";
    private static final String EDIT_PENCIL_BTN = "//mat-icon[contains(text(),'edit')]/ancestor::button";
    private static final String NAME_FIELD_EDIT_VIEW = "//input[@formcontrolname='name']";
    private static final String HEADER_NAME_VIEW = "//header//div[@class='tb-details-title']/span";
    private static final String DONE_BTN_EDIT_VIEW = "//mat-icon[contains(text(),'done')]/ancestor::button";
    private static final String DESCRIPTION = "//textarea";
    private static final String DEBUG_CHECKBOX_EDIT = "//mat-checkbox[@formcontrolname='debugMode']";
    private static final String DEBUG_CHECKBOX_VIEW = "//mat-checkbox[@formcontrolname='debugMode']//input";
    private static final String CLOSE_ENTITY_VIEW_BTN = "//header//mat-icon[contains(text(),'close')]/../..";
    private static final String SEARCH_BTN = "//mat-toolbar//mat-icon[contains(text(),'search')]/.." +
            "/parent::button[@class='mat-focus-indicator mat-tooltip-trigger mat-icon-button mat-button-base ng-star-inserted']";

    public String getDeleteBtns() {
        return DELETE_BTNS;
    }

    public String getCheckbox(String entityName) {
        return String.format(CHECKBOX, entityName);
    }

    public String getCheckboxes() {
        return String.format(CHECKBOXES);
    }

    public WebElement warningPopUpYesBtn() {
        return waitUntilElementToBeClickable(WARNING_DELETE_POPUP_YES);
    }

    public WebElement entityCount() {
        return waitUntilVisibilityOfElementLocated(ENTITY_COUNT);
    }

    public WebElement refreshBtn() {
        return waitUntilElementToBeClickable(REFRESH_BTN);
    }

    public WebElement helpBtn() {
        return waitUntilElementToBeClickable(HELP_BTN);
    }

    public WebElement checkBox(String entityName) {
        return waitUntilElementToBeClickable(String.format(CHECKBOX, entityName));
    }

    public WebElement deleteSelectedBtn() {
        return waitUntilElementToBeClickable(DELETE_SELECTED_BTN);
    }

    public WebElement selectAllCheckBox() {
        return waitUntilElementToBeClickable(SELECT_ALL_CHECKBOX);
    }

    public WebElement editPencilBtn() {
        return waitUntilElementToBeClickable(EDIT_PENCIL_BTN);
    }

    public WebElement nameFieldEditMenu() {
        return waitUntilElementToBeClickable(NAME_FIELD_EDIT_VIEW);
    }

    public WebElement headerNameView() {
        return waitUntilVisibilityOfElementLocated(HEADER_NAME_VIEW);
    }

    public WebElement doneBtnEditView() {
        return waitUntilElementToBeClickable(DONE_BTN_EDIT_VIEW);
    }

    public WebElement description() {
        return waitUntilVisibilityOfElementLocated(DESCRIPTION);
    }

    public WebElement debugCheckboxEdit() {
        return waitUntilElementToBeClickable(DEBUG_CHECKBOX_EDIT);
    }

    public WebElement debugCheckboxView() {
        return waitUntilVisibilityOfElementLocated(DEBUG_CHECKBOX_VIEW);
    }

    public WebElement closeEntityViewBtn() {
        sleep(1);
        return waitUntilElementToBeClickable(CLOSE_ENTITY_VIEW_BTN);
    }

    public WebElement searchBtn() {
        return waitUntilElementToBeClickable(SEARCH_BTN);
    }

    public List<WebElement> deleteBtns() {
        return waitUntilVisibilityOfElementsLocated(DELETE_BTNS);
    }

    public List<WebElement> checkBoxes() {
        return waitUntilElementsToBeClickable(CHECKBOXES);
    }

    public List<WebElement> markCheckbox() {
        return waitUntilVisibilityOfElementsLocated(MARKS_CHECKBOX);
    }

    public List<WebElement> allEntity() {
        return waitUntilVisibilityOfElementsLocated(ALL_ENTITY);
    }

    public WebElement doneBtnEditViewVisible() {
        return waitUntilVisibilityOfElementLocated(DONE_BTN_EDIT_VIEW);
    }
}
