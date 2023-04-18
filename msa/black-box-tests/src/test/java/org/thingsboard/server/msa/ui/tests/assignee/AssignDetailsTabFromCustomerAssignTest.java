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

    private AlarmId tenantAlarmId;
    private DeviceId tenantDeviceId;
    private String tenantDeviceName;

    private String tenantAlarmType;

    @BeforeMethod
    public void generateTenantEntity() {
        if (getJwtTokenFromLocalStorage() == null) {
            new LoginPageHelper(driver).authorizationTenant();
        }
        tenantAlarmType = "Test tenant alarm " + random();

        tenantDeviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("")).getName();
        tenantDeviceId = testRestClient.getDeviceByName(tenantDeviceName).getId();
        tenantAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(tenantDeviceId, tenantAlarmType)).getId();
    }

    @AfterMethod
    public void deleteTenantEntity() {
        testRestClient.deleteAlarm(tenantAlarmId);
        testRestClient.deleteDevice(tenantDeviceId);
        clearStorage();
    }

    @Test
    public void assignAlarmToYourselfCustomer() {
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(alarmType, userEmail);

        assertIsDisplayed(alarmPage.assignedUser(userEmail));
    }

    @Test
    public void reassignAlarmByCustomerFromAnotherCustomerUser() {
        loginByUser(userWithNameEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(assignedAlarmType, userName);

        assertIsDisplayed(alarmPage.assignedUser(userName));
    }

    @Test
    public void unassignedAlarmFromCustomer() {
        loginByUser(userWithNameEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.unassignedAlarm(assignedAlarmType);

        assertIsDisplayed(alarmPage.unassigned(assignedAlarmType));
    }

    @Test
    public void unassignedAlarmFromAnotherUserFromCustomer() {
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.unassignedAlarm(assignedAlarmType);

        assertIsDisplayed(alarmPage.unassigned(assignedAlarmType));
    }

    @Test
    public void checkTheDisplayOfNamesEmailsFromCustomer() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);
        alarmPage.assignAlarmTo(tenantAlarmType, Const.TENANT_EMAIL);
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
        alarmPage.assignAlarmTo(tenantAlarmType, Const.TENANT_EMAIL);
        devicePage.closeDeviceDetailsViewBtn().click();
        devicePage.assignToCustomerBtn(tenantDeviceName).click();
        devicePage.assignToCustomer(customerTitle);
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(tenantDeviceName);
        jsClick(alarmPage.assignBtn(tenantAlarmType));

        assertIsDisplayed(alarmPage.accessForbiddenDialogView());
    }
}
