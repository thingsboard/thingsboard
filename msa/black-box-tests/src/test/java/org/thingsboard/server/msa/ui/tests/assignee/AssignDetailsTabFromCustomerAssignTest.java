/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.msa.ui.tests.assignee;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.utils.Const;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;

public class AssignDetailsTabFromCustomerAssignTest extends AbstractAssignTest {

    AlarmId tenantAlarmId;
    DeviceId tenantDeviceId;
    String tenantDeviceName;
    String tenantAlarm = "Test tenant alarm " + random();

    public void loginByUser(String userEmail) {
        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersUserBtn(customerTitle).click();
        customerPage.getUserLoginBtnByEmail(userEmail).click();
    }

    @BeforeMethod
    public void generateTenantEntity() {
        if (getJwtTokenFromLocalStorage() == null) {
            new LoginPageHelper(driver).authorizationTenant();
        }
        tenantDeviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("")).getName();
        tenantDeviceId = testRestClient.getDeviceByName(tenantDeviceName).getId();
        tenantAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(tenantDeviceId, tenantAlarm)).getId();
    }

    @AfterMethod
    public void clear() {
        testRestClient.deleteAlarm(tenantAlarmId);
        testRestClient.deleteDevice(tenantDeviceId);
        clearStorage();
    }

    @Test
    public void assignAlarmToYourselfCustomer() {
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(alarm, userEmail);

        assertIsDisplayed(alarmPage.assignedUser(userEmail));
    }

    @Test
    public void reassignAlarmByCustomerFromAnotherCustomerUser() {
        loginByUser(userWithNameEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(assignedAlarm, userName);

        assertIsDisplayed(alarmPage.assignedUser(userName));
    }

    @Test
    public void unassignedAlarmFromCustomer() {
        loginByUser(userWithNameEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.unassignedAlarm(assignedAlarm);

        assertIsDisplayed(alarmPage.unassigned(assignedAlarm));
    }

    @Test
    public void unassignedAlarmFromAnotherUserFromCustomer() {
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.unassignedAlarm(assignedAlarm);

        assertIsDisplayed(alarmPage.unassigned(assignedAlarm));
    }

    @Test
    public void checkTheDisplayOfNamesEmailsFromCustomer() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);
        alarmPage.assignAlarmTo(tenantAlarm, Const.TENANT_EMAIL);
        devicePage.closeDeviceDetailsViewBtn().click();
        devicePage.assignToCustomerBtn(tenantDeviceName).click();
        devicePage.assignToCustomer(customerTitle);
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    @Test
    public void reassignTenantForOldAlarm() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);
        alarmPage.assignAlarmTo(tenantAlarm, Const.TENANT_EMAIL);
        devicePage.closeDeviceDetailsViewBtn().click();
        devicePage.assignToCustomerBtn(tenantDeviceName).click();
        devicePage.assignToCustomer(customerTitle);
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);
        alarmPage.assignBtn(tenantAlarm).click();

        assertIsDisplayed(alarmPage.accessForbiddenDialogView());
    }
}
