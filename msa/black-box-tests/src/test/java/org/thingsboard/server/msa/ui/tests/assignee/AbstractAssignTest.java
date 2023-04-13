package org.thingsboard.server.msa.ui.tests.assignee;

import org.openqa.selenium.WebElement;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.AlarmDetailsViewHelper;
import org.thingsboard.server.msa.ui.pages.AlarmHelper;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.DevicePageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import java.util.List;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;

abstract public class AbstractAssignTest extends AbstractDriverBaseTest {

    protected AlarmId alarmId;
    protected AlarmId assignedAlarmId;
    protected AlarmId propageteAlarmId;
    protected AlarmId propageteAssigneAlarmId;
    protected AlarmId tenantAssigneAlarmId;
    protected DeviceId deviceId;
    protected UserId userId;
    protected UserId userWithNameId;
//    protected TenantId tenantId = testRestClient.getTenants(pageLink).getData().get(0).getId();
    protected CustomerId customerId;
    protected String deviceName;
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
        deviceId = testRestClient.getDeviceByName(deviceName).getId();
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


    }

    @AfterClass
    public void deleteTestEntities() {
        testRestClient.deleteDevice(deviceId);
        testRestClient.deleteCustomer(customerId);
    }

    @AfterMethod
    public void deleteTestAlarms() {
        testRestClient.deleteAlarm(alarmId);
        testRestClient.deleteAlarm(assignedAlarmId);
        testRestClient.deleteAlarm(propageteAlarmId);
        testRestClient.deleteAlarm(propageteAssigneAlarmId);
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
