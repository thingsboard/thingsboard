// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.tests.alarmassignee;

import io.qameta.allure.Epic;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.AlarmDetailsEntityTabHelper;
import org.thingsboard.server.msa.ui.pages.AlarmDetailsViewHelper;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.DevicePageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;

@Epic("Alarm assign")
abstract public class AbstractAssignTest extends AbstractDriverBaseTest {

    protected AlarmId alarmId;
    protected AlarmId assignedAlarmId;
    protected DeviceId deviceId;
    protected UserId userId;
    protected UserId userWithNameId;
    protected CustomerId customerId;
    protected String deviceName;
    protected String userName;
    protected String customerTitle;
    protected String userEmail;
    protected String userWithNameEmail;
    protected String alarmType;
    protected String assignedAlarmType;
    protected SideBarMenuViewHelper sideBarMenuView;
    protected AlarmDetailsEntityTabHelper alarmPage;
    protected DevicePageHelper devicePage;
    protected CustomerPageHelper customerPage;
    protected AlarmDetailsViewHelper alarmDetailsView;

    @BeforeClass
    public void generateCommonTestEntity() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        alarmPage = new AlarmDetailsEntityTabHelper(driver);
        devicePage = new DevicePageHelper(driver);
        customerPage = new CustomerPageHelper(driver);
        alarmDetailsView = new AlarmDetailsViewHelper(driver);

        userName = "User " + random();
        customerTitle = "Customer " + random();
        userEmail = random() + "@thingsboard.org";
        userWithNameEmail = random() + "@thingsboard.org";
        alarmType = "Test alarm " + random();
        assignedAlarmType = "Test assigned alarm " + random();

        customerId = testRestClient.postCustomer(EntityPrototypes.defaultCustomerPrototype(customerTitle)).getId();
        userId = testRestClient.postUser(EntityPrototypes.defaultUser(userEmail, getCustomerByName(customerTitle).getId())).getId();
        userWithNameId = testRestClient.postUser(EntityPrototypes.defaultUser(userWithNameEmail, getCustomerByName(customerTitle).getId(), userName)).getId();
        deviceName = testRestClient.postDevice("", EntityPrototypes.defaultDevicePrototype("Device ", customerId)).getName();
        deviceId = testRestClient.getDeviceByName(deviceName).getId();
    }

    @AfterClass
    public void deleteCommonEntities() {
        deleteCustomerById(customerId);
        deleteDeviceById(deviceId);
    }

    @BeforeMethod
    public void createCommonTestAlarms() {
        alarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, alarmType)).getId();
        assignedAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, assignedAlarmType, userId)).getId();
    }

    @AfterMethod
    public void deleteCommonCreatedAlarms() {
        deleteAlarmsByIds(alarmId, assignedAlarmId);
    }

    public void loginByUser(String userEmail) {
        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersUserBtn(customerTitle).click();
        customerPage.getUserLoginBtnByEmail(userEmail).click();
    }
}
