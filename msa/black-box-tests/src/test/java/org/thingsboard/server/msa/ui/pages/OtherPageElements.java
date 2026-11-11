// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
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

    protected static final String ENTITY = "//mat-row//span[text() = '%s']";
    protected static final String DELETE_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),'delete')]/ancestor::button";
    protected static final String DETAILS_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),'edit')]/parent::button";
    private static final String ENTITY_COUNT = "//div[@class='mat-paginator-range-label']";
    private static final String CONFIRM_DIALOG = "//tb-confirm-dialog";
    private static final String WARNING_DELETE_POPUP_YES = CONFIRM_DIALOG + "//button[2]";
    private static final String WARNING_DELETE_POPUP_NO = CONFIRM_DIALOG + "//button[1]";
    private static final String WARNING_DELETE_POPUP_TITLE = "//tb-confirm-dialog/h2";
    private static final String REFRESH_BTN = "//mat-icon[contains(text(),'refresh')]/parent::button";
    private static final String HELP_BTN = "//span[contains(text(),'details')]/ancestor::div/following-sibling::div[@class='details-buttons']";
    private static final String HELP_BTN_ENTITY_GROUP = "//mat-icon[contains(text(),'help')]/../..";
    private static final String CHECKBOX = "//mat-row//span[contains(text(),'%s')]/../..//mat-checkbox";
    private static final String CHECKBOXES = "//tbody//mat-checkbox";
    private static final String DELETE_SELECTED_BTN = "//div[@class='mat-toolbar-tools']//mat-icon[contains(text(),'delete')]/parent::button";
    private static final String DELETE_BTNS = "//mat-icon[contains(text(),' delete')]/../..";
    private static final String MARKS_CHECKBOX = "//mat-row[contains (@class,'mat-selected')]//mat-checkbox[contains(@class, 'checked')]";
    private static final String SELECT_ALL_CHECKBOX = "//thead//mat-checkbox";
    private static final String ALL_ENTITY = "//tbody/mat-row";
    private static final String EDIT_PENCIL_BTN = "//tb-entity-details-panel//mat-icon[contains(text(),'edit')]/ancestor::button";
    private static final String ENTITY_GROUP_EDIT_PENCIL_BTN = "//tb-entity-details-panel//mat-icon[contains(text(),'edit')]/ancestor::button";
    private static final String NAME_FIELD_EDIT_VIEW = "//input[@formcontrolname='name']";
    private static final String HEADER_NAME_VIEW = "//header//div[@class='tb-details-title']/span";
    private static final String DONE_BTN_EDIT_VIEW = "//mat-icon[contains(text(),'done')]/ancestor::button";
    private static final String ENTITY_GROUP_DONE_BTN_EDIT_VIEW = "//mat-icon[contains(text(),'done')]/ancestor::button";
    private static final String DESCRIPTION_ENTITY_VIEW = "//mat-drawer-container[contains(@class,'has-open')]//textarea";
    private static final String DESCRIPTION_ADD_ENTITY_VIEW = "//mat-dialog-container//textarea";
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
    private static final String ENTITY_VIEW_TITLE = "//mat-drawer-container[contains(@class,'has-open')]//div[@class='tb-details-title']//span";
    private static final String LIST_OF_ENTITY = "//div[@role='listbox']/mat-option";
    protected static final String ADD_ENTITY_VIEW = "//mat-dialog-container";
    private static final String SEARCH_FIELD = "//input[contains (@placeholder,'Search')]";
    private static final String ADD_ENTITY_GROUP_VIEW = "//tb-entity-group-wizard";
    private static final String ADD_TO_GROUP_VIEW = "//tb-select-entity-group-dialog";
    private static final String NAME_FIELD_ADD_ENTITY_GROUP = ADD_ENTITY_GROUP_VIEW + "//input[@formcontrolname='name']";
    private static final String ENTITY_GROUP = "//mat-cell[contains(@class,'cdk-column-name')]/span";
    private static final String ENTITY_GROUP_TABLE_HEADER = "//tb-group-entity-table-header/span";
    private static final String ENTITY_GROUP_HEADER = "//tb-breadcrumb//span[contains(text(),'%s')]";
    private static final String OPEN_ENTITY_GROUP_VIEW_BTN = "//span[contains(text(),' Open entity group ')]";
    private static final String ENTITY_GROUP_VIEW_DELETE_BTN = "//tb-entity-group//span[contains(text(),' Delete')]";
    private static final String COPY_ID_BTN = "//span[contains(text(),'Id')]//ancestor::button";
    private static final String COPY_POPUP_TEXT = "//tb-snack-bar-component//div[contains (@class,'toast-text')]";
    private static final String CHANGE_OWNER_BTN = "//mat-icon[contains(text(),'assignment_ind')]//ancestor::button";
    private static final String ADD_TO_GROUP_BTN = "//mat-icon[contains(text(),'add_circle')]//ancestor::button";
    private static final String MOVE_TO_GROUP_BTN = "//mat-icon[contains(text(),'swap_vertical_circle')]//ancestor::button";
    private static final String REMOVE_FROM_GROUP_BTN = "//mat-icon[contains(text(),'remove_circle')]//ancestor::button";
    private static final String CHANGE_OWNER_VIEW_FIELD = "//input[@formcontrolname='owner']";
    private static final String ADD_GROUP_VIEW_EXIST_FIELD = "//input[@formcontrolname='entityGroup']";
    private static final String ADD_GROUP_VIEW_NEW_GROUP_FIELD = "//input[@formcontrolname='newEntityGroupName']";
    private static final String ADD_GROUP_VIEW_NEW_GROUP_RADIO_BTN = ADD_GROUP_VIEW_NEW_GROUP_FIELD + "//ancestor::section/span";
    private static final String CUSTOMER_FROM_DROPDOWN = "//div[@role='listbox']//span[contains(text(),'%s')]";
    private static final String CHANGE_OWNER_VIEW_CHANGE_BTN = "//span[contains(text(),'Change owner')]//ancestor::button";
    private static final String SELECT_GROUP_VIEW_SUBMIT_BTN = "//button[@type='submit']";
    private static final String ENTITY_FROM_LIST = "//div[@role='listbox']/mat-option//span[contains(text(),'%s')]";
    private static final String BROWSE_FILE = "//input[@class='file-input']";
    private static final String IMPORT_BROWSE_FILE = "//mat-dialog-container//span[contains(text(),'Import')]/..";
    private static final String IMPORTING_FILE = "//div[contains(text(),'%s')]";
    private static final String CLEAR_IMPORT_FILE_BTN = "//div[@class='tb-file-clear-container']//button";
    private static final String ITEMS_PER_PAGE = "//div[contains(@id,'mat-select')]";
    private static final String ITEMS_COUNT = ITEMS_PER_PAGE + "//mat-option/span[contains(text(),'%d')]";

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

    public String getConfirmDialog() {
        return CONFIRM_DIALOG;
    }

    public WebElement warningPopUpYesBtn() {
        return waitUntilElementToBeClickable(WARNING_DELETE_POPUP_YES);
    }

    public WebElement warningPopUpNoBtn() {
        return waitUntilElementToBeClickable(WARNING_DELETE_POPUP_NO);
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

    public WebElement helpBtnEntityGroup() {
        return waitUntilElementToBeClickable(HELP_BTN_ENTITY_GROUP);
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

    public WebElement entityGroupEditPencilBtn() {
        waitUntilVisibilityOfElementsLocated(ENTITY_GROUP_EDIT_PENCIL_BTN);
        return waitUntilElementToBeClickable(ENTITY_GROUP_EDIT_PENCIL_BTN);
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

    public WebElement entityGroupDoneBtnEditView() {
        return waitUntilElementToBeClickable(ENTITY_GROUP_DONE_BTN_EDIT_VIEW);
    }

    public WebElement entityGroupDoneBtnVisibleEditView() {
        return waitUntilVisibilityOfElementLocated(ENTITY_GROUP_DONE_BTN_EDIT_VIEW);
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

    public WebElement searchField() {
        return waitUntilElementToBeClickable(SEARCH_FIELD);
    }

    public WebElement addEntityGroupView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_GROUP_VIEW);
    }

    public WebElement addToEntityGroupView() {
        return waitUntilVisibilityOfElementLocated(ADD_TO_GROUP_VIEW);
    }

    public WebElement addEntityGroupViewNameField() {
        return waitUntilElementToBeClickable(NAME_FIELD_ADD_ENTITY_GROUP);
    }

    public List<WebElement> entities(String name) {
        return waitUntilVisibilityOfElementsLocated(String.format(ENTITY, name));
    }

    public List<WebElement> entityGroups() {
        return waitUntilVisibilityOfElementsLocated(ENTITY_GROUP);
    }

    public WebElement entityGroupTableHeader() {
        return waitUntilVisibilityOfElementLocated(ENTITY_GROUP_TABLE_HEADER);
    }

    public WebElement entityGroupHeader(String name) {
        return waitUntilVisibilityOfElementLocated(String.format(ENTITY_GROUP_HEADER, name));
    }

    public WebElement openEntityGroupViewBtn() {
        return waitUntilElementToBeClickable(OPEN_ENTITY_GROUP_VIEW_BTN);
    }

    public WebElement entityGroupViewDeleteBtn() {
        return waitUntilElementToBeClickable(ENTITY_GROUP_VIEW_DELETE_BTN);
    }

    public WebElement copyEntityIdBtn() {
        return waitUntilElementToBeClickable(COPY_ID_BTN);
    }

    public WebElement copyPopup() {
        return waitUntilVisibilityOfElementLocated(COPY_POPUP_TEXT);
    }

    public WebElement changeOwnerBtn() {
        return waitUntilElementToBeClickable(CHANGE_OWNER_BTN);
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

    public WebElement addToGroupBtn() {
        return waitUntilElementToBeClickable(ADD_TO_GROUP_BTN);
    }

    public WebElement moveToGroupBtn() {
        return waitUntilElementToBeClickable(MOVE_TO_GROUP_BTN);
    }

    public WebElement removeFromGroupBtn() {
        return waitUntilElementToBeClickable(REMOVE_FROM_GROUP_BTN);
    }

    public WebElement changeOwnerViewField() {
        return waitUntilElementToBeClickable(CHANGE_OWNER_VIEW_FIELD);
    }

    public WebElement selectGroupViewExistField() {
        return waitUntilElementToBeClickable(ADD_GROUP_VIEW_EXIST_FIELD);
    }

    public WebElement selectGroupViewNewGroupField() {
        return waitUntilElementToBeClickable(ADD_GROUP_VIEW_NEW_GROUP_FIELD);
    }

    public WebElement selectGroupViewNewGroupRadioBtn() {
        return waitUntilElementToBeClickable(ADD_GROUP_VIEW_NEW_GROUP_RADIO_BTN);
    }

    public WebElement entityFromDropDown(String entityName) {
        return waitUntilElementToBeClickable(String.format(CUSTOMER_FROM_DROPDOWN, entityName));
    }

    public WebElement changeOwnerViewChangeOwnerBtn() {
        return waitUntilElementToBeClickable(CHANGE_OWNER_VIEW_CHANGE_BTN);
    }

    public WebElement selectGroupViewSubmitBtn() {
        return waitUntilElementToBeClickable(SELECT_GROUP_VIEW_SUBMIT_BTN);
    }

    public WebElement changeOwnerViewChangeOwnerBtnVisible() {
        return waitUntilVisibilityOfElementLocated(CHANGE_OWNER_VIEW_CHANGE_BTN);
    }

    public WebElement selectGroupViewSubmitBtnVisible() {
        return waitUntilVisibilityOfElementLocated(SELECT_GROUP_VIEW_SUBMIT_BTN);
    }

    public WebElement itemsPerPage() {
        return waitUntilElementToBeClickable(ITEMS_PER_PAGE);
    }

    public WebElement itemsCount(int itemCount) {
        return waitUntilElementToBeClickable(String.format(ITEMS_COUNT, itemCount));
    }
}
