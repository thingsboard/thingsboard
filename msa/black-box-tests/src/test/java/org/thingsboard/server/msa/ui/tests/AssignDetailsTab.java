package org.thingsboard.server.msa.ui.tests;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.msa.prototypes.DevicePrototypes;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.AlarmHelper;
import org.thingsboard.server.msa.ui.pages.DevicePageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

public class AssignDetailsTab extends AbstractDriverBaseTest {

    AlarmId alarmId;
    DeviceId deviceId;
    UserId userId;
    String deviceName;
    SideBarMenuViewHelper sideBarMenuView;
    AlarmHelper alarmPage;
    DevicePageHelper devicePage;

    @BeforeClass
    public void generateAlarm() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        alarmPage = new AlarmHelper(driver);
        devicePage = new DevicePageHelper(driver);
    }

    @AfterMethod
    public void deleteAlarm() {
        testRestClient.deleteAlarm(alarmId);
        testRestClient.deleteDevice(deviceId);
        if (userId != null) {
            testRestClient.deleteUser(userId);
        }
    }

    @Test(dataProviderClass = DataProviderCredential.class, dataProvider = "assignTo")
    public void assignAlarmTo(String user) {
        deviceName = testRestClient.postDevice("", DevicePrototypes.defaultDevicePrototype("")).getName();
        deviceId = testRestClient.getDeviceByName(deviceName).getId();
        alarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId)).getId();

        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignTo(user);

        Assert.assertTrue(alarmPage.assignUserDisplayName(user).isDisplayed());
    }

    @Test
    public void reassignAlarm() {
        userId = testRestClient.postUser(EntityPrototypes.defaultUser(getCustomerByName("Customer A").getId())).getId();
        deviceName = testRestClient.postDevice("", DevicePrototypes.defaultDevicePrototype("")).getName();
        deviceId = testRestClient.getDeviceByName(deviceName).getId();
        alarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, userId)).getId();

        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignTo("customer@thingsboard.org");

        Assert.assertTrue(alarmPage.assignUserDisplayName("customer@thingsboard.org").isDisplayed());
    }
}
