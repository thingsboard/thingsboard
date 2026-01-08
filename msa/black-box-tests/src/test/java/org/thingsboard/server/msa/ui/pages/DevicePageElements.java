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

public class DevicePageElements extends OtherPageElementsHelper {
    public DevicePageElements(WebDriver driver) {
        super(driver);
    }

    private static final String DEVICE = "//table//span[text()='%s']";
    private static final String DEVICE_DETAILS_VIEW = "//tb-details-panel";
    private static final String DEVICE_DETAILS_ALARMS = DEVICE_DETAILS_VIEW + "//span[text()='Alarms']";
    private static final String ASSIGN_TO_CUSTOMER_BTN = "//mat-cell[contains(@class,'name')]/span[text()='%s']" +
            "/ancestor::mat-row//mat-icon[contains(text(),'assignment_ind')]/parent::button";
    private static final String CHOOSE_CUSTOMER_FOR_ASSIGN_FIELD = "//input[@formcontrolname='entity']";
    private static final String ENTITY_FROM_DROPDOWN = "//div[@role = 'listbox']//span[text() = '%s']";
    private static final String CLOSE_DEVICE_DETAILS_VIEW = "//header//mat-icon[contains(text(),'close')]/parent::button";
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
    private static final String CLEAR_PROFILE_FIELD_BTN = "//button[@aria-label='Clear']";
    private static final String DEVICE_PROFILE_REDIRECTED_BTN = "//a[@aria-label='Open device profile']";
    private static final String DEVICE_LABEL_PAGE = DEVICE + "/ancestor::mat-row//mat-cell[contains(@class,'cdk-column-label')]/span";
    private static final String DEVICE_CUSTOMER_PAGE = DEVICE + "/ancestor::mat-row//mat-cell[contains(@class,'cdk-column-customerTitle')]/span";
    private static final String DEVICE_LABEL_EDIT = "//input[@formcontrolname='label']";
    private static final String DEVICE_DEVICE_PROFILE_PAGE = DEVICE + "/ancestor::mat-row//mat-cell[contains(@class,'cdk-column-deviceProfileName')]/span";
    private static final String ASSIGN_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),'assignment_ind')]/ancestor::button";
    private static final String UNASSIGN_BTN = ENTITY + "/ancestor::mat-row//mat-icon[contains(text(),' assignment_return')]/ancestor::button";
    private static final String ASSIGN_BTN_DETAILS_TAB = "//span[contains(text(),'Assign to customer')]/parent::button";
    private static final String UNASSIGN_BTN_DETAILS_TAB = "//span[contains(text(),'Unassign from customer')]/parent::button";
    private static final String ASSIGNED_FIELD_DETAILS_TAB = "//mat-label[text() = 'Assigned to customer']/parent::label/parent::div/input";
    private static final String ASSIGN_MARKED_DEVICE_BTN = "//mat-icon[text() = 'assignment_ind']/parent::button";
    private static final String FILTER_BTN = "//tb-device-info-filter/button";
    private static final String DEVICE_PROFILE_FIELD = "(//input[@formcontrolname='deviceProfile'])[2]";
    private static final String DEVICE_STATE_SELECT = "//div[contains(@class,'tb-filter-panel')]//mat-select[@role='combobox']";
    private static final String LIST_OF_DEVICES_STATE = "//div[@class='status']";
    private static final String LIST_OF_DEVICES_PROFILE = "//mat-cell[contains(@class,'deviceProfileName')]";
    private static final String MAKE_DEVICE_PUBLIC_BTN = DEVICE + "/ancestor::mat-row//mat-icon[contains(text(),'share')]/parent::button";
    private static final String DEVICE_IS_PUBLIC_CHECKBOX = DEVICE + "/ancestor::mat-row//mat-icon[contains(text(),'check_box')]";
    private static final String MAKE_DEVICE_PUBLIC_BTN_DETAILS_TAB = "//span[contains(text(),'Make device public')]/parent::button";
    private static final String MAKE_DEVICE_PRIVATE_BTN = DEVICE + "/ancestor::mat-row//mat-icon[contains(text(),'reply')]/parent::button";
    private static final String DEVICE_IS_PRIVATE_CHECKBOX = DEVICE + "/ancestor::mat-row//mat-icon[contains(text(),'check_box_outline_blank')]";
    private static final String MAKE_DEVICE_PRIVATE_BTN_DETAILS_TAB = "//span[contains(text(),'Make device private')]/parent::button";

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

    public WebElement closeDeviceDetailsViewBtn() {
        return waitUntilElementToBeClickable(CLOSE_DEVICE_DETAILS_VIEW);
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

    public WebElement deviceCustomerOnPage(String deviceName) {
        return waitUntilVisibilityOfElementLocated(String.format(DEVICE_CUSTOMER_PAGE, deviceName));
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

    public WebElement assignBtn(String deviceName) {
        return waitUntilElementToBeClickable(String.format(ASSIGN_BTN, deviceName));
    }

    public WebElement assignBtnVisible(String deviceName) {
        return waitUntilVisibilityOfElementLocated(String.format(ASSIGN_BTN, deviceName));
    }

    public WebElement unassignBtn(String deviceName) {
        return waitUntilElementToBeClickable(String.format(UNASSIGN_BTN, deviceName));
    }

    public WebElement assignBtnDetailsTab() {
        return waitUntilElementToBeClickable(ASSIGN_BTN_DETAILS_TAB);
    }

    public WebElement unassignBtnDetailsTab() {
        return waitUntilElementToBeClickable(UNASSIGN_BTN_DETAILS_TAB);
    }

    public WebElement assignFieldDetailsTab() {
        return waitUntilVisibilityOfElementLocated(ASSIGNED_FIELD_DETAILS_TAB);
    }

    public WebElement assignMarkedDeviceBtn() {
        return waitUntilVisibilityOfElementLocated(ASSIGN_MARKED_DEVICE_BTN);
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

    public WebElement makeDevicePublicBtn(String deviceName) {
        return waitUntilElementToBeClickable(String.format(MAKE_DEVICE_PUBLIC_BTN, deviceName));
    }

    public WebElement deviceIsPublicCheckbox(String deviceName) {
        return waitUntilVisibilityOfElementLocated(String.format(DEVICE_IS_PUBLIC_CHECKBOX, deviceName));
    }

    public WebElement makeDevicePublicBtnDetailsTab() {
        return waitUntilElementToBeClickable(MAKE_DEVICE_PUBLIC_BTN_DETAILS_TAB);
    }

    public WebElement makeDevicePrivateBtn(String deviceName) {
        return waitUntilElementToBeClickable(String.format(MAKE_DEVICE_PRIVATE_BTN, deviceName));
    }

    public WebElement deviceIsPrivateCheckbox(String deviceName) {
        return waitUntilVisibilityOfElementLocated(String.format(DEVICE_IS_PRIVATE_CHECKBOX, deviceName));
    }

    public WebElement makeDevicePrivateBtnDetailsTab() {
        return waitUntilElementToBeClickable(MAKE_DEVICE_PRIVATE_BTN_DETAILS_TAB);
    }
}
