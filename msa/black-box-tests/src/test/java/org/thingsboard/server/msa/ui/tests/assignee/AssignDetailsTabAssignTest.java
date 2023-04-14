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
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.msa.ui.pages.AssetPageElements;
import org.thingsboard.server.msa.ui.pages.EntityViewPage;
import org.thingsboard.server.msa.ui.utils.Const;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;

public class AssignDetailsTabAssignTest extends AbstractAssignTest {

    private AlarmId propageteAlarmId;
    private AlarmId propageteAssigneAlarmId;
    private AlarmId customerAlarmId;
    private AlarmId assetAlarmId;
    private AlarmId entityViewAlarmId;
    private String assetName;
    private String entityViewName;
    private final String propagateAlarm = "Test propagated alarm 1 " + random();
    private final String propagateAssignedAlarm = "Test propagated alarm 2 " + random();
    private final String customerAlarm = "Test customer alarm" + random();
    private final String assetAlarm = "Test asset alarm" + random();
    private final String entityViewAlarm = "Test entity view alarm" + random();
    private AssetId assetId;
    private EntityViewId entityViewId;

    @BeforeClass
    public void generateTestEntities() {
        customerAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(customerId, customerAlarm)).getId();
        assetId = testRestClient.postAsset(EntityPrototypes.defaultAssetPrototype("", customerId)).getId();
        assetName = testRestClient.getAssetById(assetId).getName();
        assetAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(assetId, assetAlarm)).getId();
        entityViewId = testRestClient.postEntityView(EntityPrototypes.defaultEntityViewPrototype("", "", "DEVICE")).getId();
        entityViewName = testRestClient.getEntityViewById(entityViewId).getName();
        entityViewAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(entityViewId, entityViewAlarm)).getId();
    }

    @BeforeMethod
    public void generateTestAlarms() {
        propageteAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, propagateAlarm, true)).getId();
        propageteAssigneAlarmId = testRestClient.postAlarm(EntityPrototypes.defaultAlarm(deviceId, propagateAssignedAlarm, userId, true)).getId();
    }

    @AfterClass
    public void deleteTestEntities() {
        testRestClient.deleteAlarm(customerAlarmId);
        testRestClient.deleteAlarm(assetAlarmId);
        testRestClient.deleteAlarm(entityViewAlarmId);
        testRestClient.deleteAsset(assetId);
        testRestClient.deleteEntityView(entityViewId);
    }

    @AfterMethod
    public void deleteTestAlarms() {
        testRestClient.deleteAlarm(propageteAlarmId);
        testRestClient.deleteAlarm(propageteAssigneAlarmId);
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

    @Test(dataProvider = "alarms")
    public void assignAlarmToYourself(String alarm) {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(alarm, Const.TENANT_EMAIL);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    @Test(dataProvider = "alarms")
    public void assignAlarmToAnotherUser(String alarm) {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(alarm, userEmail);

        assertIsDisplayed(alarmPage.assignedUser(userEmail));
    }

    @Test(dataProvider = "assignedAlarms")
    public void unassignedAlarm(String assignedAlarm) {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.unassignedAlarm(assignedAlarm);

        assertIsDisplayed(alarmPage.unassigned(assignedAlarm));
    }

    @Test(dataProvider = "assignedAlarms")
    public void reassignAlarm(String assignedAlarm) {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.assignAlarmTo(assignedAlarm, Const.TENANT_EMAIL);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    @Test
    public void searchByEmail() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.searchAlarm(alarm, Const.TENANT_EMAIL);
        alarmPage.setUsers();

        assertThat(alarmPage.getUsers()).hasSize(1).as("Search result contains search input").contains(Const.TENANT_EMAIL);
        alarmPage.assignUsers().forEach(this::assertIsDisplayed);
    }

    @Test(groups = "broken")
    public void searchByName() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.searchAlarm(alarm, userName);
        alarmPage.setUsers();

        assertThat(alarmPage.getUsers()).hasSize(1).as("Search result contains search input").contains(userName);
        alarmPage.assignUsers().forEach(this::assertIsDisplayed);
    }

    @Test
    public void assignAlarmToYourselfFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.alarmDetailsBtn(alarm).click();
        alarmDetailsView.assignAlarmTo(Const.TENANT_EMAIL);
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    @Test
    public void assignAlarmToAnotherUserFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.alarmDetailsBtn(alarm).click();
        alarmDetailsView.assignAlarmTo(userEmail);
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmPage.assignedUser(userEmail));
    }

    @Test
    public void unassignedAlarmFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.alarmDetailsBtn(assignedAlarm).click();
        alarmDetailsView.unassignedAlarm();
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmPage.unassigned(assignedAlarm));
    }

    @Test
    public void reassignAlarmFromDetails() {
        sideBarMenuView.goToDevicesPage();
        devicePage.openDeviceAlarms(deviceName);
        alarmPage.alarmDetailsBtn(assignedAlarm).click();
        alarmDetailsView.assignAlarmTo(Const.TENANT_EMAIL);
        alarmDetailsView.closeAlarmDetailsViewBtn().click();

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    @Test
    public void assignCustomerAlarmToYourself() {
        sideBarMenuView.customerBtn().click();
        customerPage.openCustomerAlarms(customerTitle);
        alarmPage.assignAlarmTo(customerAlarm, Const.TENANT_EMAIL);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    @Test
    public void assignAssetAlarmToYourself() {
        AssetPageElements assetPageElements = new AssetPageElements(driver);
        sideBarMenuView.goToAssetsPage();
        assetPageElements.openAssetAlarms(assetName);
        alarmPage.assignAlarmTo(assetAlarm, Const.TENANT_EMAIL);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

    @Test
    public void assignEntityViewsAlarmToYourself() {
        EntityViewPage entityViewPage = new EntityViewPage(driver);
        sideBarMenuView.goToEntityViewsPage();
        entityViewPage.openEntityViewAlarms(entityViewName);
        alarmPage.assignAlarmTo(entityViewAlarm, Const.TENANT_EMAIL);

        assertIsDisplayed(alarmPage.assignedUser(Const.TENANT_EMAIL));
    }

}
