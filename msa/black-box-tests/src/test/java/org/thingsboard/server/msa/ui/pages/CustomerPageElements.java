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

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class CustomerPageElements extends OtherPageElementsHelper {
    public CustomerPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String CUSTOMER = "//mat-row//span[contains(text(),'%s')]";
    private static final String EMAIL = ENTITY + "/../..//mat-cell[contains(@class,'email')]/span";
    private static final String COUNTRY = ENTITY + "/../..//mat-cell[contains(@class,'country')]/span";
    private static final String CITY = ENTITY + "/../..//mat-cell[contains(@class,'city')]/span";
    private static final String TITLES = "//mat-cell[contains(@class,'cdk-column-title')]/span";
    protected static final String EDIT_MENU_DASHBOARD_FIELD = "//input[@formcontrolname='dashboard']";
    private static final String EDIT_MENU_DASHBOARD = "//div[@class='cdk-overlay-pane']//span/span[contains(text(),'%s')]";
    private static final String MANAGE_CUSTOMERS_USERS_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),' account_circle')]/parent::button";
    private static final String MANAGE_CUSTOMERS_ASSETS_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),' domain')]/parent::button";
    private static final String MANAGE_CUSTOMERS_DEVICES_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),'devices_other')]/parent::button";
    private static final String MANAGE_CUSTOMERS_DASHBOARDS_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),'dashboard')]/parent::button";
    private static final String MANAGE_CUSTOMERS_EDGE_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),'router')]/parent::button";
    private static final String ADD_USER_EMAIL = "//tb-add-user-dialog//input[@formcontrolname='email']";
    private static final String ACTIVATE_WINDOW_OK_BTN = "//span[contains(text(),'OK')]";
    private static final String USER_LOGIN_BTN = "//mat-icon[@data-mat-icon-name='login']/parent::button";
    private static final String USER_LOGIN_BTN_BY_EMAIL = "//mat-cell[contains(@class,'email')]/span[contains(text(),'%s')]" +
            "/ancestor::mat-row//mat-icon[@data-mat-icon-name='login']/parent::button";
    private static final String USERS_WIDGET = "//tb-widget";
    private static final String SELECT_COUNTRY_MENU = "//mat-form-field//mat-select[@formcontrolname='country']";
    private static final String COUNTRIES = "//span[@class='mdc-list-item__primary-text']";
    protected static final String INPUT_FIELD = "//input[@formcontrolname='%s']";
    protected static final String INPUT_FIELD_NAME_TITLE = "title";
    private static final String INPUT_FIELD_NAME_CITY = "city";
    private static final String INPUT_FIELD_NAME_STATE = "state";
    private static final String INPUT_FIELD_NAME_ZIP = "zip";
    private static final String INPUT_FIELD_NAME_ADDRESS = "address";
    private static final String INPUT_FIELD_NAME_ADDRESS2 = "address2";
    private static final String INPUT_FIELD_NAME_EMAIL = "email";
    private static final String INPUT_FIELD_NAME_NUMBER = "phoneNumber";
    private static final String INPUT_FIELD_NAME_ASSIGNED_LIST = "entity";
    private static final String ASSIGNED_BTN = "//button[@type='submit']";
    private static final String HIDE_HOME_DASHBOARD_TOOLBAR = "//mat-checkbox[@formcontrolname='homeDashboardHideToolbar']//label";
    private static final String FILTER_BTN = "//tb-filters-edit";
    private static final String TIME_BTN = "//tb-timewindow[not(@hidelabel)]";
    private static final String CUSTOMER_ICON_HEADER = "//tb-breadcrumb//span[contains(text(),'Customer %s')]";
    private static final String CUSTOMER_USER_ICON_HEADER = "Users";
    private static final String CUSTOMER_ASSETS_ICON_HEADER = "Assets";
    private static final String CUSTOMER_DEVICES_ICON_HEADER = "Devices";
    private static final String CUSTOMER_DASHBOARD_ICON_HEADER = "Dashboards";
    private static final String CUSTOMER_EDGE_ICON_HEADER = "edge instances";
    private static final String CUSTOMER_USER_ICON_HEAD = "(//mat-drawer-content//span[contains(@class,'tb-entity-table')])[1]";
    private static final String MANAGE_BTN_VIEW = "//span[contains(text(),'%s')]";
    private static final String MANAGE_CUSTOMERS_USERS_BTN_VIEW = "Manage users";
    private static final String MANAGE_CUSTOMERS_ASSETS_BTN_VIEW = "Manage assets";
    private static final String MANAGE_CUSTOMERS_DEVICE_BTN_VIEW = "Manage devices";
    private static final String MANAGE_CUSTOMERS_DASHBOARD_BTN_VIEW = "Manage dashboards";
    private static final String MANAGE_CUSTOMERS_EDGE_BTN_VIEW = "Manage edges ";
    private static final String DELETE_FROM_VIEW_BTN = "//tb-customer//span[contains(text(),' Delete')]";
    private static final String CUSTOMER_DETAILS_VIEW = "//tb-details-panel";
    private static final String CUSTOMER_DETAILS_ALARMS = CUSTOMER_DETAILS_VIEW + "//span[text()='Alarms']";

    public WebElement titleFieldAddEntityView() {
        return waitUntilElementToBeClickable(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_TITLE));
    }

    public WebElement titleFieldEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_TITLE));
    }

    public WebElement customer(String entityName) {
        return waitUntilElementToBeClickable(String.format(CUSTOMER, entityName));
    }

    public WebElement email(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(EMAIL, entityName));
    }

    public WebElement country(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(COUNTRY, entityName));
    }

    public WebElement city(String entityName) {
        return waitUntilVisibilityOfElementLocated(String.format(CITY, entityName));
    }

    public List<WebElement> entityTitles() {
        return waitUntilVisibilityOfElementsLocated(TITLES);
    }

    public WebElement editMenuDashboardField() {
        return waitUntilVisibilityOfElementLocated(EDIT_MENU_DASHBOARD_FIELD);
    }

    public WebElement editMenuDashboard(String dashboardName) {
        return waitUntilElementToBeClickable(String.format(EDIT_MENU_DASHBOARD, dashboardName));
    }

    public WebElement phoneNumberEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_NUMBER));
    }

    public WebElement phoneNumberAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_NUMBER));
    }

    public WebElement manageCustomersUserBtn(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_CUSTOMERS_USERS_BTN, title));
    }

    public WebElement manageCustomersAssetsBtn(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_CUSTOMERS_ASSETS_BTN, title));
    }

    public WebElement manageCustomersDevicesBtn(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_CUSTOMERS_DEVICES_BTN, title));
    }

    public WebElement manageCustomersDashboardsBtn(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_CUSTOMERS_DASHBOARDS_BTN, title));
    }

    public WebElement manageCustomersEdgeBtn(String title) {
        return waitUntilElementToBeClickable(String.format(MANAGE_CUSTOMERS_EDGE_BTN, title));
    }

    public WebElement addUserEmailField() {
        return waitUntilElementToBeClickable(ADD_USER_EMAIL);
    }

    public WebElement activateWindowOkBtn() {
        return waitUntilElementToBeClickable(ACTIVATE_WINDOW_OK_BTN);
    }

    public WebElement userLoginBtn() {
        return waitUntilElementToBeClickable(USER_LOGIN_BTN);
    }

    public WebElement getUserLoginBtnByEmail(String email) {
        return waitUntilElementToBeClickable(String.format(USER_LOGIN_BTN_BY_EMAIL, email));
    }

    public WebElement usersWidget() {
        return waitUntilVisibilityOfElementLocated(USERS_WIDGET);
    }

    public WebElement countrySelectMenuEntityView() {
        return waitUntilElementToBeClickable(SELECT_COUNTRY_MENU);
    }

    public WebElement countrySelectMenuAddEntityView() {
        return waitUntilElementToBeClickable(ADD_ENTITY_VIEW + SELECT_COUNTRY_MENU);
    }

    public List<WebElement> countries() {
        return waitUntilElementsToBeClickable(COUNTRIES);
    }

    public WebElement cityEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_CITY));
    }

    public WebElement cityAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_CITY));
    }

    public WebElement stateEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_STATE));
    }

    public WebElement stateAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_STATE));
    }

    public WebElement zipEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_ZIP));
    }

    public WebElement zipAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_ZIP));
    }

    public WebElement addressEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_ADDRESS));
    }

    public WebElement addressAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_ADDRESS));
    }

    public WebElement address2EntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_ADDRESS2));
    }

    public WebElement address2AddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_ADDRESS2));
    }

    public WebElement emailEntityView() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_EMAIL));
    }

    public WebElement emailAddEntityView() {
        return waitUntilVisibilityOfElementLocated(ADD_ENTITY_VIEW + String.format(INPUT_FIELD, INPUT_FIELD_NAME_EMAIL));
    }

    public WebElement assignedField() {
        return waitUntilVisibilityOfElementLocated(String.format(INPUT_FIELD, INPUT_FIELD_NAME_ASSIGNED_LIST));
    }

    public WebElement submitAssignedBtn() {
        return waitUntilElementToBeClickable(ASSIGNED_BTN);
    }

    public WebElement hideHomeDashboardToolbarCheckbox() {
        return waitUntilElementToBeClickable(HIDE_HOME_DASHBOARD_TOOLBAR);
    }

    public WebElement filterBtn() {
        return waitUntilVisibilityOfElementLocated(FILTER_BTN);
    }

    public WebElement timeBtn() {
        return waitUntilVisibilityOfElementLocated(TIME_BTN);
    }

    public WebElement customerUserIconHeader() {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_ICON_HEADER, CUSTOMER_USER_ICON_HEADER));
    }

    public WebElement customerAssetsIconHeader() {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_ICON_HEADER, CUSTOMER_ASSETS_ICON_HEADER));
    }

    public WebElement customerDevicesIconHeader() {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_ICON_HEADER, CUSTOMER_DEVICES_ICON_HEADER));
    }

    public WebElement customerDashboardIconHeader() {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_ICON_HEADER, CUSTOMER_DASHBOARD_ICON_HEADER));
    }

    public WebElement customerEdgeIconHeader() {
        return waitUntilVisibilityOfElementLocated(String.format(CUSTOMER_ICON_HEADER, CUSTOMER_EDGE_ICON_HEADER));
    }

    public WebElement customerManageWindowIconHead() {
        return waitUntilVisibilityOfElementLocated(CUSTOMER_USER_ICON_HEAD);
    }

    public WebElement manageCustomersUserBtnView() {
        return waitUntilElementToBeClickable(String.format(MANAGE_BTN_VIEW, MANAGE_CUSTOMERS_USERS_BTN_VIEW));
    }

    public WebElement manageCustomersAssetsBtnView() {
        return waitUntilElementToBeClickable(String.format(MANAGE_BTN_VIEW, MANAGE_CUSTOMERS_ASSETS_BTN_VIEW));
    }

    public WebElement manageCustomersDeviceBtnView() {
        return waitUntilElementToBeClickable(String.format(MANAGE_BTN_VIEW, MANAGE_CUSTOMERS_DEVICE_BTN_VIEW));
    }

    public WebElement manageCustomersDashboardsBtnView() {
        return waitUntilElementToBeClickable(String.format(MANAGE_BTN_VIEW, MANAGE_CUSTOMERS_DASHBOARD_BTN_VIEW));
    }

    public WebElement manageCustomersEdgeBtnView() {
        return waitUntilElementToBeClickable(String.format(MANAGE_BTN_VIEW, MANAGE_CUSTOMERS_EDGE_BTN_VIEW));
    }

    public WebElement customerViewDeleteBtn() {
        return waitUntilElementToBeClickable(DELETE_FROM_VIEW_BTN);
    }

    public WebElement customerDetailsView() {
        return waitUntilPresenceOfElementLocated(CUSTOMER_DETAILS_VIEW);
    }

    public WebElement customerDetailsAlarmsBtn() {
        return waitUntilElementToBeClickable(CUSTOMER_DETAILS_ALARMS);
    }
}
