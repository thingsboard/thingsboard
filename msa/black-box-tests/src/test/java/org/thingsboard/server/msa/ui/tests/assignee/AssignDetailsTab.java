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

import org.junit.platform.commons.function.Try;
import org.openqa.selenium.html5.WebStorage;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.utils.Const;

import static org.assertj.core.api.Assertions.assertThat;

public class AssignDetailsTab extends AbstractAssignTest {

    /*@BeforeMethod
    public void openAlarmView() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
    }*/

    @Test(dataProvider = "alarms")
    public void assignAlarmToYourself(String alarm) {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(alarm, Const.TENANT_EMAIL);

        Assert.assertTrue(alarmPage.assignedUser(Const.TENANT_EMAIL).isDisplayed());
    }

    @Test(dataProvider = "alarms")
    public void assignAlarmToAnotherUser(String alarm) {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(alarm, userEmail);

        Assert.assertTrue(alarmPage.assignedUser(userEmail).isDisplayed());
    }

    @Test(dataProvider = "assignedAlarms")
    public void unassignedAlarm(String assignedAlarm) {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.unassignedAlarm(assignedAlarm);

        Assert.assertTrue(alarmPage.unassigned(assignedAlarm).isDisplayed());
    }

    @Test(dataProvider = "assignedAlarms")
    public void reassignAlarm(String assignedAlarm) {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(assignedAlarm, Const.TENANT_EMAIL);

        Assert.assertTrue(alarmPage.assignedUser(Const.TENANT_EMAIL).isDisplayed());
    }

    @Test
    public void searchByEmail() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.searchAlarm(alarm, Const.TENANT_EMAIL);
        alarmPage.setUsers();

        assertThat(alarmPage.getUsers()).hasSize(1).contains(Const.TENANT_EMAIL);
        alarmPage.assignUsers().forEach(u -> assertThat(u.isDisplayed()).isTrue());
    }

    @Test
    public void searchByName() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.searchAlarm(alarm, name);
        alarmPage.setUsers();

        assertThat(alarmPage.getUsers()).hasSize(1).contains(name);
        alarmPage.assignUsers().forEach(u -> assertThat(u.isDisplayed()).isTrue());
    }

    @Test
    public void assignAlarmToYourselfFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.alarmDetailsBtn(alarm).click();
        alarmDetailsView.assignAlarmTo(Const.TENANT_EMAIL);
        alarmDetailsView.closeViewBtn().click();

        Assert.assertTrue(alarmPage.assignedUser(Const.TENANT_EMAIL).isDisplayed());
    }

    @Test
    public void assignAlarmToAnotherUserFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.alarmDetailsBtn(alarm).click();
        alarmDetailsView.assignAlarmTo(userEmail);
        alarmDetailsView.closeViewBtn().click();

        Assert.assertTrue(alarmPage.assignedUser(userEmail).isDisplayed());
    }

    @Test
    public void unassignedAlarmFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.alarmDetailsBtn(assignedAlarm).click();
        alarmDetailsView.unassignedAlarm();
        alarmDetailsView.closeViewBtn().click();

        Assert.assertTrue(alarmPage.unassigned(assignedAlarm).isDisplayed());
    }

    @Test
    public void reassignAlarmFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.alarmDetailsBtn(assignedAlarm).click();
        alarmDetailsView.assignAlarmTo(Const.TENANT_EMAIL);
        alarmDetailsView.closeViewBtn().click();

        Assert.assertTrue(alarmPage.assignedUser(Const.TENANT_EMAIL).isDisplayed());
    }

    @Test
    public void assignAlarmToYourselfCustomer() {
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(alarm, userEmail);
        clearStorage();

        Assert.assertTrue(alarmPage.assignedUser(userEmail).isDisplayed());
    }

    @Test
    public void reassignAlarmByCustomerFromAnotherCustomerUser() {
        loginByUser(userWithNameEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(assignedAlarm, name);
        clearStorage();

        Assert.assertTrue(alarmPage.assignedUser(name).isDisplayed());
    }

    @Test
    public void unassignedAlarmFromCustomer() {
        loginByUser(userWithNameEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.unassignedAlarm(assignedAlarm);
        clearStorage();

        Assert.assertTrue(alarmPage.unassigned(assignedAlarm).isDisplayed());
    }

    @Test
    public void unassignedAlarmFromAnotherUserFromCustomer() {
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.unassignedAlarm(assignedAlarm);
        clearStorage();

        Assert.assertTrue(alarmPage.unassigned(assignedAlarm).isDisplayed());
    }

    @Test
    public void checkTheDisplayOfNamesEmailsFromCustomer() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(alarm, Const.TENANT_EMAIL);
        loginByUser(userEmail);
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignBtn(alarm).click();
        try {
            alarmPage.assertUsersForAssignIsNotPresent();
        } finally {
            clearStorage();
        }
    }

    public WebStorage getWebStorage() {
        if (driver instanceof WebStorage) {
            return (WebStorage) driver;
        } else {
            throw new IllegalArgumentException("This test expects the driver to implement WebStorage");
        }
    }

    public void clearStorage() {
        getWebStorage().getLocalStorage().clear();
        getWebStorage().getSessionStorage().clear();
    }

}
