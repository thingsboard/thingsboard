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

import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.AlarmDetailsViewHelper;
import org.thingsboard.server.msa.ui.pages.AlarmHelper;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.DevicePageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;

abstract public class AbstractAssignTest extends AbstractDriverBaseTest {

    protected AlarmId alarmId;
    protected AlarmId assignedAlarmId;
    protected DeviceId deviceId;

    protected UserId userId;
    protected UserId userWithNameId;
    protected CustomerId customerId;
    protected String deviceName;

    protected SideBarMenuViewHelper sideBarMenuView;
    protected AlarmHelper alarmPage;
    protected DevicePageHelper devicePage;
    protected CustomerPageHelper customerPage;
    protected AlarmDetailsViewHelper alarmDetailsView;

    protected String userName = "User " + random();
    protected String customerTitle = "Customer " + random();
    protected String userEmail = random() + "@thingsboard.org";
    protected String userWithNameEmail = random() + "@thingsboard.org";
    protected String alarm = "Test alarm " + random();
    protected String assignedAlarm = "Test assigned alarm " + random();

    @BeforeClass
    public void generateTestEntity() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        alarmPage = new AlarmHelper(driver);
        devicePage = new DevicePageHelper(driver);
        customerPage = new CustomerPageHelper(driver);
        alarmDetailsView = new AlarmDetailsViewHelper(driver);

        customerId = testRestClient.postCustomer(EntityPrototypes.defaultCustomerPrototype(customerTitle)).getId();
        userId = testRestClient.postUser(EntityPrototypes.defaultUser(userEmail, getCustomerByName(customerTitle).getId())).getId();
        userWithNameId = testRestClient.postUser(EntityPrototypes.defaultUser(userWithNameEmail, getCustomerByName(customerTitle).getId(), userName)).getId();
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("", customerId)).getName();
        deviceId = testRestClient.getDeviceByName(deviceName).getId();
    }

    @AfterClass
    public void deleteEntities() {
        testRestClient.deleteCustomer(customerId);
        testRestClient.deleteDevice(deviceId);
    }

    @BeforeMethod
    public void createTestAlarms() {
        alarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, alarm)).getId();
        assignedAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, assignedAlarm, userId)).getId();
    }

    @AfterMethod
    public void deleteCreatedAlarms() {
        testRestClient.deleteAlarm(alarmId);
        testRestClient.deleteAlarm(assignedAlarmId);
    }
}
