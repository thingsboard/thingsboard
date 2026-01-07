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

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

import java.util.List;

public class OtherPageElements extends AbstractBasePage {
    public OtherPageElements(WebDriver driver) {
        super(driver);
    }

    protected static final String ENTITY = "//mat-row//span[contains(text(),'%s')]";
    protected static final String DELETE_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),'delete')]/ancestor::button";
    protected static final String DETAILS_BTN = ENTITY + "/../..//mat-icon[contains(text(),'edit')]/../..";
    private static final String ENTITY_COUNT = "//div[@class='mat-paginator-range-label']";
    private static final String WARNING_DELETE_POPUP_YES = "//tb-confirm-dialog//button[2]";
    private static final String WARNING_DELETE_POPUP_TITLE = "//tb-confirm-dialog/h2";
    private static final String REFRESH_BTN = "//mat-icon[contains(text(),'refresh')]/parent::button";
    private static final String HELP_BTN = "//mat-icon[contains(text(),'help')]/ancestor::button";
    private static final String CHECKBOX = "//mat-row//span[contains(text(),'%s')]/../..//mat-checkbox";
    private static final String CHECKBOXES = "//tbody//mat-checkbox";
    private static final String DELETE_SELECTED_BTN = "//div[@class='mat-toolbar-tools']//mat-icon[contains(text(),'delete')]/parent::button";
    private static final String DELETE_BTNS = "//mat-icon[contains(text(),' delete')]/../..";
    private static final String MARKS_CHECKBOX = "//mat-row[contains (@class,'mat-selected')]//mat-checkbox[contains(@class, 'checked')]";
    private static final String SELECT_ALL_CHECKBOX = "//thead//mat-checkbox";
    private static final String ALL_ENTITY = "//mat-row[@class='mat-mdc-row mdc-data-table__row cdk-row mat-row-select ng-star-inserted']";
    private static final String EDIT_PENCIL_BTN = "//tb-details-panel//mat-icon[contains(text(),'edit')]/ancestor::button";
    private static final String NAME_FIELD_EDIT_VIEW = "//input[@formcontrolname='name']";
    private static final String HEADER_NAME_VIEW = "//header//div[@class='tb-details-title']/span";
    private static final String DONE_BTN_EDIT_VIEW = "//mat-icon[contains(text(),'done')]/ancestor::button";
    private static final String DESCRIPTION_ENTITY_VIEW = "//textarea";
    private static final String DESCRIPTION_ADD_ENTITY_VIEW = "//tb-add-entity-dialog//textarea";
    private static final String DEBUG_CHECKBOX_EDIT = "//mat-checkbox[@formcontrolname='debugMode']";
    private static final String DEBUG_CHECKBOX_VIEW = "//mat-checkbox[@formcontrolname='debugMode']//input";
    private static final String CLOSE_ENTITY_VIEW_BTN = "//header//mat-icon[contains(text(),'close')]/parent::button";
    private static final String SEARCH_BTN = "//mat-toolbar//mat-icon[contains(text(),'search')]/ancestor::button[contains(@class,'ng-star')]";
    private static final String SORT_BY_NAME_BTN = "//div[contains(text(),'Name')]";
    private static final String SORT_BY_TITLE_BTN = "//div[contains(text(),'Title')]";
    private static final String SORT_BY_TIME_BTN = "//div[contains(text(),'Created time')]/..";
    private static final String CREATED_TIME = "//tbody[@role='rowgroup']//mat-cell[2]/span";
    private static final String PLUS_BTN = "//mat-icon[contains(text(),'add')]/ancestor::button";
    private static final String CREATE_VIEW_ADD_BTN = "//span[contains(text(),'Add')]/..";
    private static final String WARNING_MESSAGE = "//tb-snack-bar-component/div/div";
    private static final String ERROR_MESSAGE = "//mat-error";
    private static final String ENTITY_VIEW_TITLE = "//div[@class='tb-details-title']//span";
    private static final String LIST_OF_ENTITY = "//div[@role='listbox']/mat-option";
    private static final String ENTITY_FROM_LIST = "//div[@role='listbox']/mat-option//span[contains(text(),'%s')]";
    protected static final String ADD_ENTITY_VIEW = "//tb-add-entity-dialog";
    protected static final String STATE_CONTROLLER = "//tb-entity-state-controller";
    private static final String SEARCH_FIELD = "//input[contains (@placeholder,'Search')]";
    private static final String BROWSE_FILE = "//input[@class='file-input']";
    private static final String IMPORT_BROWSE_FILE = "//mat-dialog-container//span[contains(text(),'Import')]/..";
    private static final String IMPORTING_FILE = "//div[contains(text(),'%s')]";
    private static final String CLEAR_IMPORT_FILE_BTN = "//div[@class='tb-file-clear-container']//button";

    public String getEntity(String entityName) {
        return String.format(ENTITY, entityName);
    }

    public String getWarningMessage() {
        return WARNING_MESSAGE;
    }

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

    public WebElement warningPopUpTitle() {
        return waitUntilElementToBeClickable(WARNING_DELETE_POPUP_TITLE);
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

    public WebElement presentCheckBox(String name) {
        return waitUntilPresenceOfElementLocated(getCheckbox(name));
    }

    public WebElement deleteSelectedBtn() {
        return waitUntilElementToBeClickable(DELETE_SELECTED_BTN);
    }

    public WebElement selectAllCheckBox() {
        return waitUntilElementToBeClickable(SELECT_ALL_CHECKBOX);
    }

    public WebElement editPencilBtn() {
        waitUntilVisibilityOfElementsLocated(EDIT_PENCIL_BTN);
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

    public WebElement descriptionEntityView() {
        return waitUntilVisibilityOfElementLocated(DESCRIPTION_ENTITY_VIEW);
    }

    public WebElement descriptionAddEntityView() {
        return waitUntilVisibilityOfElementLocated(DESCRIPTION_ADD_ENTITY_VIEW);
    }

    public WebElement debugCheckboxEdit() {
        return waitUntilElementToBeClickable(DEBUG_CHECKBOX_EDIT);
    }

    public WebElement debugCheckboxView() {
        return waitUntilPresenceOfElementLocated(DEBUG_CHECKBOX_VIEW);
    }

    public WebElement closeEntityViewBtn() {
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

    public WebElement sortByNameBtn() {
        return waitUntilElementToBeClickable(SORT_BY_NAME_BTN);
    }

    public WebElement sortByTitleBtn() {
        return waitUntilElementToBeClickable(SORT_BY_TITLE_BTN);
    }

    public WebElement sortByTimeBtn() {
        return waitUntilElementToBeClickable(SORT_BY_TIME_BTN);
    }

    public List<WebElement> createdTime() {
        return waitUntilVisibilityOfElementsLocated(CREATED_TIME);
    }

    public WebElement plusBtn() {
        return waitUntilElementToBeClickable(PLUS_BTN);
    }

    public WebElement addBtnC() {
        return waitUntilElementToBeClickable(CREATE_VIEW_ADD_BTN);
    }

    public WebElement addBtnV() {
        return waitUntilVisibilityOfElementLocated(CREATE_VIEW_ADD_BTN);
    }

    public WebElement warningMessage() {
        return waitUntilVisibilityOfElementLocated(WARNING_MESSAGE);
    }

    public WebElement deleteBtn(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(DELETE_BTN, entityName));
    }

    public WebElement detailsBtn(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(DETAILS_BTN, entityName));
    }

    public WebElement entity(String entityName) {
        return waitUntilElementToBeClickable(String.format(ENTITY, entityName));
    }

    public WebElement errorMessage() {
        return waitUntilVisibilityOfElementLocated(ERROR_MESSAGE);
    }

    public WebElement entityViewTitle() {
        return waitUntilVisibilityOfElementLocated(ENTITY_VIEW_TITLE);
    }

    public List<WebElement> listOfEntity() {
        return waitUntilElementsToBeClickable(LIST_OF_ENTITY);
    }

    public WebElement entityFromList(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(ENTITY_FROM_LIST, entityName));
    }

    public WebElement addEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW);
    }

    public WebElement stateController() {
        return waitUntilVisibilityOfElementLocated(STATE_CONTROLLER);
    }

    public WebElement searchField() {
        return waitUntilElementToBeClickable(SEARCH_FIELD);
    }

    public WebElement browseFile() {
        waitUntilElementToBeClickable(BROWSE_FILE + "/preceding-sibling::button");
        return driver.findElement(By.xpath(BROWSE_FILE));
    }

    public WebElement importBrowseFileBtn() {
        return waitUntilElementToBeClickable(IMPORT_BROWSE_FILE);
    }

    public WebElement importingFile(String fileName) {
        return waitUntilVisibilityOfElementLocated(String.format(IMPORTING_FILE, fileName));
    }

    public WebElement clearImportFileBtn() {
        return waitUntilElementToBeClickable(CLEAR_IMPORT_FILE_BTN);
    }
}
