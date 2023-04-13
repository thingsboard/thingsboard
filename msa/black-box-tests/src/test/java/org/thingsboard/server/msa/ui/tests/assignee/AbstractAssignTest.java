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
import org.testng.annotations.DataProvider;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityViewId;
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
    protected AlarmId propageteAlarmId;
    protected AlarmId propageteAssigneAlarmId;
    protected AlarmId tenantAlarmId;
    protected AlarmId onCustomerAlarmId;
    protected AlarmId assetAlarmId;
    protected AlarmId entityViewAlarmId;
    protected DeviceId deviceId;
    protected DeviceId tenantDeviceId;
    protected UserId userId;
    protected UserId userWithNameId;
    protected CustomerId customerId;
    protected String deviceName;
    protected String assetName;
    protected String entityViewName;
    protected String tenantDeviceName;
    protected SideBarMenuViewHelper sideBarMenuView;
    protected AlarmHelper alarmPage;
    protected DevicePageHelper devicePage;
    protected AlarmDetailsViewHelper alarmDetailsView;
    CustomerPageHelper customerPage;

    protected String name = "User";
    protected String customerTitle = "Customer" + random();
    protected String userEmail = random() + "@thingsboard.org";
    protected String userWithNameEmail = random() + "@thingsboard.org";
    protected String alarm = "Test alarm 1";
    protected String assignedAlarm = "Test alarm 2";
    protected String propagateAlarm = "Test propagated alarm 1";
    protected String propagateAssignedAlarm = "Test propagated alarm 2";
    protected String tenantAlarm = "Test tenant alarm";
    protected String customerAlarm = "Test customer alarm";
    protected String assetAlarm = "Test asset alarm";
    protected String entityViewAlarm = "Test entity view alarm";
    protected AssetId assetId;
    protected EntityViewId entityViewId;

    @BeforeClass
    public void generateTestEntities() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        alarmPage = new AlarmHelper(driver);
        devicePage = new DevicePageHelper(driver);
        alarmDetailsView = new AlarmDetailsViewHelper(driver);
        customerPage = new CustomerPageHelper(driver);

        customerId = testRestClient.postCustomer(EntityPrototypes.defaultCustomerPrototype(customerTitle)).getId();
        userId = testRestClient.postUser(EntityPrototypes.defaultUser(userEmail, getCustomerByName(customerTitle).getId())).getId();
        userWithNameId = testRestClient.postUser(EntityPrototypes.defaultUser(userWithNameEmail, getCustomerByName(customerTitle).getId(), name)).getId();
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("", customerId)).getName();
        tenantDeviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("")).getName();
        deviceId = testRestClient.getDeviceByName(deviceName).getId();
        tenantDeviceId = testRestClient.getDeviceByName(tenantDeviceName).getId();
        onCustomerAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(customerId, customerAlarm)).getId();
        assetId = testRestClient.postAsset(EntityPrototypes.defaultAssetPrototype("", customerId)).getId();
        assetName = testRestClient.getAssetById(assetId).getName();
        assetAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(assetId, assetAlarm)).getId();
        entityViewId = testRestClient.postEntityView(EntityPrototypes.defaultEntityViewPrototype("", "", "DEVICE")).getId();
        entityViewName = testRestClient.getEntityViewById(entityViewId).getName();
        entityViewAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(entityViewId, entityViewAlarm)).getId();
    }

    @BeforeMethod
    public void generateTestAlarms() {
        if (getJwtTokenFromLocalStorage() == null) {
            new LoginPageHelper(driver).authorizationTenant();
        }
        alarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, alarm)).getId();
        assignedAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, assignedAlarm, userId)).getId();
        propageteAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, propagateAlarm, true)).getId();
        propageteAssigneAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, propagateAssignedAlarm, userId, true)).getId();

        tenantAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(tenantDeviceId, tenantAlarm)).getId();

    }

    @AfterClass
    public void deleteTestEntities() {
        testRestClient.deleteDevice(deviceId);
        testRestClient.deleteDevice(tenantDeviceId);
        testRestClient.deleteCustomer(customerId);
        testRestClient.deleteAlarm(onCustomerAlarmId);
        testRestClient.deleteAlarm(assetAlarmId);
        testRestClient.deleteAlarm(entityViewAlarmId);
    }

    @AfterMethod
    public void deleteTestAlarms() {
        testRestClient.deleteAlarm(alarmId);
        testRestClient.deleteAlarm(assignedAlarmId);
        testRestClient.deleteAlarm(propageteAlarmId);
        testRestClient.deleteAlarm(propageteAssigneAlarmId);
        testRestClient.deleteAlarm(tenantAlarmId);
    }

    public void loginByUser(String userEmail) {
        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersUserBtn(customerTitle).click();
        customerPage.getUserLoginBtnByEmail(userEmail).click();
    }

    @DataProvider
    public Object[][] alarms() {
        return new Object[][]{
                {alarm},
                {propagateAlarm}};
    }

    @DataProvider
    public Object[][] assignedAlarms() {
        return new Object[][]{
                {assignedAlarm},
                {propagateAssignedAlarm}};
    }
}
