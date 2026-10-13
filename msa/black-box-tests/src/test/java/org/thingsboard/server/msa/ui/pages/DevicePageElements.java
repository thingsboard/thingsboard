// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class DevicePageElements extends OtherPageElementsHelper {
    public DevicePageElements(WebDriver driver) {
        super(driver);
    }

    private static final String ALL_GROUP_NAMES = "//mat-icon[contains(text(),'check')]/ancestor::mat-row/mat-cell[contains(@class,'name')]/span";
    private static final String ALL_NAMES = "//mat-cell[contains(@class,'cdk-column-column1')]/span";
    private static final String GROUPS_BTN = "//a[contains(@href,'/entities/devices/groups')]/span[@class='mdc-tab__content']";
    private static final String DEVICE = "//table//span[text()='%s']";
    private static final String DEVICE_DETAILS_VIEW = "//tb-details-panel";
    private static final String DEVICE_DETAILS_ALARMS = DEVICE_DETAILS_VIEW + "//span[text()='Alarms']";
    private static final String ASSIGN_TO_CUSTOMER_BTN = "//mat-cell[contains(@class,'name')]/span[text()='%s']" +
            "/ancestor::mat-row//mat-icon[contains(text(),'assignment_ind')]/parent::button";
    private static final String CHOOSE_CUSTOMER_FOR_ASSIGN_FIELD = "//input[@formcontrolname='entity']";
    private static final String ENTITY_FROM_DROPDOWN = "//div[@role = 'listbox']//span[text() = '%s']";
    private static final String CLOSE_DETAILS_VIEW = "//header//mat-icon[contains(text(),'close')]/parent::button";
    private static final String SUBMIT_BTN = "//button[@type='submit']";
    private static final String ADD_DEVICE_BTN = "//mat-icon[text() = 'insert_drive_file']/parent::button";
    private static final String HEADER_NAME_VIEW = "//header//div[@class='tb-details-title']/span";
    private static final String ADD_DEVICE_VIEW = "//tb-device-wizard";
    private static final String DELETE_BTN_DETAILS_TAB = "//span[contains(text(),'Delete device')]/parent::button";
    private static final String CHECKBOX_GATEWAY_EDIT = "//mat-checkbox[@formcontrolname='gateway']//label";
    private static final String CHECKBOX_OVERWRITE_ACTIVITY_TIME_EDIT = "//mat-checkbox[@formcontrolname='overwriteActivityTime']//label";
    private static final String CHECKBOX_GATEWAY_DETAILS = "//mat-checkbox[@formcontrolname='gateway']//input";
    private static final String CHECKBOX_GATEWAY_PAGE = DEVICE + "/ancestor::mat-row//mat-cell[contains(@class,'cdk-column-gateway')]//mat-icon[text() = 'check_box']";
    private static final String CHECKBOX_OVERWRITE_ACTIVITY_TIME_DETAILS = "//mat-checkbox[@formcontrolname='overwriteActivityTime']//input";
    private static final String CLEAR_PROFILE_FIELD_BTN = "//tb-device-profile-autocomplete//button[@aria-label='Clear']";
    private static final String DEVICE_PROFILE_REDIRECTED_BTN = "//a[@aria-label='Open device profile']";
    private static final String DEVICE_LABEL_PAGE = DEVICE + "/ancestor::mat-row//mat-cell[contains(@class,'cdk-column-label')]/span";
    private static final String DEVICE_OWNER_PAGE = DEVICE + "/ancestor::mat-row//mat-cell[contains(@class,'cdk-column-ownerName')]/span";
    private static final String OWNER_FIELD = "//input[@formcontrolname='owner']";
    private static final String DEVICE_LABEL_EDIT = "//input[@formcontrolname='label']";
    private static final String DEVICE_DEVICE_PROFILE_PAGE = DEVICE + "/ancestor::mat-row//mat-cell[contains(@class,'cdk-column-type')]/span";
    protected static final String CHANGE_OWNER_BTN = "//mat-icon[text() = 'assignment_ind']/parent::button";
    private static final String FILTER_BTN = "//tb-device-info-filter/button";
    private static final String DEVICE_PROFILE_FIELD = "(//input[@formcontrolname='deviceProfile'])[2]";
    private static final String DEVICE_STATE_SELECT = "//div[contains(@class,'tb-filter-panel')]//mat-select[@role='combobox']";
    private static final String LIST_OF_DEVICES_STATE = "//div[@class='status']";
    private static final String LIST_OF_DEVICES_PROFILE = "//mat-cell[contains(@class,'type')]";
    private static final String MAKE_DEVICE_GROUP_PUBLIC_BTN = DEVICE + "/ancestor::mat-row//mat-icon[contains(text(),'share')]/parent::button";
    private static final String DEVICE_GROUP_IS_PUBLIC_CHECKBOX = DEVICE + "/ancestor::mat-row//mat-icon[contains(text(),'check_box')]";
    private static final String MAKE_DEVICE_GROUP_PUBLIC_BTN_DETAILS_TAB = "//span[contains(text(),' Make entity group public ')]/parent::button";
    private static final String MAKE_DEVICE_GROUP_PRIVATE_BTN = DEVICE + "/ancestor::mat-row//mat-icon[contains(text(),'reply')]/parent::button";
    private static final String DEVICE_IS_PRIVATE_CHECKBOX = DEVICE + "/ancestor::mat-row//mat-icon[contains(text(),'check_box_outline_blank')]";
    private static final String MAKE_DEVICE_GROUP_PRIVATE_BTN_DETAILS_TAB = "//span[contains(text(),'Make entity group private')]/parent::button";

    public List<WebElement> allGroupNames() {
        return waitUntilElementsToBeClickable(ALL_GROUP_NAMES);
    }

    public List<WebElement> allNames() {
        return waitUntilElementsToBeClickable(ALL_NAMES);
    }

    public WebElement groupsBtn() {
        return waitUntilElementToBeClickable(GROUPS_BTN);
    }

    public WebElement device(String deviceName) {
        return waitUntilElementToBeClickable(String.format(DEVICE, deviceName));
    }

    public WebElement deviceDetailsAlarmsBtn() {
        return waitUntilElementToBeClickable(DEVICE_DETAILS_ALARMS);
    }

    public WebElement deviceDetailsView() {
        return waitUntilPresenceOfElementLocated(DEVICE_DETAILS_VIEW);
    }

    public WebElement assignToCustomerBtn(String deviceName) {
        return waitUntilElementToBeClickable(String.format(ASSIGN_TO_CUSTOMER_BTN, deviceName));
    }

    public WebElement chooseCustomerForAssignField() {
        return waitUntilElementToBeClickable(CHOOSE_CUSTOMER_FOR_ASSIGN_FIELD);
    }

    public WebElement entityFromDropdown(String customerTitle) {
        return waitUntilElementToBeClickable(String.format(ENTITY_FROM_DROPDOWN, customerTitle));
    }

    public WebElement closeDetailsViewBtn() {
        return waitUntilElementToBeClickable(CLOSE_DETAILS_VIEW);
    }

    public WebElement submitBtn() {
        return waitUntilElementToBeClickable(SUBMIT_BTN);
    }

    public WebElement addDeviceBtn() {
        return waitUntilElementToBeClickable(ADD_DEVICE_BTN);
    }

    public WebElement headerNameView() {
        return waitUntilVisibilityOfElementLocated(HEADER_NAME_VIEW);
    }

    public WebElement addDeviceView() {
        return waitUntilPresenceOfElementLocated(ADD_DEVICE_VIEW);
    }

    public WebElement deleteBtnDetailsTab() {
        return waitUntilElementToBeClickable(DELETE_BTN_DETAILS_TAB);
    }

    public WebElement checkboxGatewayEdit() {
        return waitUntilElementToBeClickable(CHECKBOX_GATEWAY_EDIT);
    }

    public WebElement checkboxOverwriteActivityTimeEdit() {
        return waitUntilElementToBeClickable(CHECKBOX_OVERWRITE_ACTIVITY_TIME_EDIT);
    }

    public WebElement checkboxGatewayDetailsTab() {
        return waitUntilPresenceOfElementLocated(CHECKBOX_GATEWAY_DETAILS);
    }

    public WebElement checkboxGatewayPage(String deviceName) {
        return waitUntilPresenceOfElementLocated(String.format(CHECKBOX_GATEWAY_PAGE, deviceName));
    }

    public WebElement checkboxOverwriteActivityTimeDetails() {
        return waitUntilPresenceOfElementLocated(CHECKBOX_OVERWRITE_ACTIVITY_TIME_DETAILS);
    }

    public WebElement clearProfileFieldBtn() {
        return waitUntilElementToBeClickable(CLEAR_PROFILE_FIELD_BTN);
    }

    public WebElement deviceProfileRedirectedBtn() {
        return waitUntilElementToBeClickable(DEVICE_PROFILE_REDIRECTED_BTN);
    }

    public WebElement deviceLabelOnPage(String deviceName) {
        return waitUntilVisibilityOfElementLocated(String.format(DEVICE_LABEL_PAGE, deviceName));
    }

    public WebElement ownerField() {
        return waitUntilElementToBeClickable(OWNER_FIELD);
    }

    public WebElement deviceOwnerOnPage(String deviceName) {
        return waitUntilVisibilityOfElementLocated(String.format(DEVICE_OWNER_PAGE, deviceName));
    }

    public WebElement deviceLabelEditField() {
        return waitUntilElementToBeClickable(DEVICE_LABEL_EDIT);
    }

    public WebElement deviceLabelDetailsField() {
        return waitUntilVisibilityOfElementLocated(DEVICE_LABEL_EDIT);
    }

    public WebElement deviceDeviceProfileOnPage(String deviceProfileTitle) {
        return waitUntilVisibilityOfElementLocated(String.format(DEVICE_DEVICE_PROFILE_PAGE, deviceProfileTitle));
    }

    public WebElement changeOwnerDeviceBtn() {
        return waitUntilVisibilityOfElementLocated(CHANGE_OWNER_BTN);
    }

    public WebElement filterBtn() {
        return waitUntilElementToBeClickable(FILTER_BTN);
    }

    public WebElement deviceProfileField() {
        return waitUntilElementToBeClickable(DEVICE_PROFILE_FIELD);
    }

    public WebElement deviceStateSelect() {
        return waitUntilElementToBeClickable(DEVICE_STATE_SELECT);
    }

    public List<WebElement> listOfDevicesState() {
        return waitUntilVisibilityOfElementsLocated(LIST_OF_DEVICES_STATE);
    }

    public List<WebElement> listOfDevicesProfile() {
        return waitUntilVisibilityOfElementsLocated(LIST_OF_DEVICES_PROFILE);
    }

    public WebElement makeDeviceGroupPublicBtn(String deviceName) {
        return waitUntilElementToBeClickable(String.format(MAKE_DEVICE_GROUP_PUBLIC_BTN, deviceName));
    }

    public WebElement deviceGroupIsPublicCheckbox(String deviceName) {
        return waitUntilVisibilityOfElementLocated(String.format(DEVICE_GROUP_IS_PUBLIC_CHECKBOX, deviceName));
    }

    public WebElement makeDeviceGroupPublicBtnDetailsTab() {
        return waitUntilElementToBeClickable(MAKE_DEVICE_GROUP_PUBLIC_BTN_DETAILS_TAB);
    }

    public WebElement makeDeviceGroupPrivateBtn(String deviceName) {
        return waitUntilElementToBeClickable(String.format(MAKE_DEVICE_GROUP_PRIVATE_BTN, deviceName));
    }

    public WebElement deviceIsPrivateCheckbox(String deviceName) {
        return waitUntilVisibilityOfElementLocated(String.format(DEVICE_IS_PRIVATE_CHECKBOX, deviceName));
    }

    public WebElement makeDeviceGroupPrivateBtnDetailsTab() {
        return waitUntilElementToBeClickable(MAKE_DEVICE_GROUP_PRIVATE_BTN_DETAILS_TAB);
    }
}
