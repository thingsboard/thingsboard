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
package org.thingsboard.server.msa.ui.utils;

import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.security.Authority;

public class EntityPrototypes {

    public static Tenant defaultTenantPrototype(String tenantName) {
        Tenant tenant = new Tenant();
        tenant.setTitle(tenantName);
        return tenant;
    }

    public static Customer defaultCustomer(TenantId tenantId, String title) {
        Customer customer = new Customer();
        customer.setTenantId(tenantId);
        customer.setTitle(title);
        return customer;
    }

    public static Customer defaultCustomerPrototype(String entityName) {
        Customer customer = new Customer();
        customer.setTitle(entityName);
        return customer;
    }

    public static Customer defaultCustomerPrototype(String entityName, String description) {
        Customer customer = new Customer();
        customer.setTitle(entityName);
        customer.setAdditionalInfo(JacksonUtil.newObjectNode().put("description", description));
        return customer;
    }

    public static Customer defaultCustomerPrototype(String entityName, int number) {
        Customer customer = new Customer();
        customer.setTitle(entityName);
        customer.setPhone("+1" + number);
        return customer;
    }

    public static RuleChain defaultRuleChainPrototype(String entityName) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(entityName);
        return ruleChain;
    }

    public static RuleChain defaultRuleChainPrototype(String entityName, String description) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(entityName);
        ruleChain.setAdditionalInfo(JacksonUtil.newObjectNode().put("description", description));
        return ruleChain;
    }

    public static RuleChain defaultRuleChainPrototype(String entityName, boolean debugMode) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(entityName);
        ruleChain.setDebugMode(debugMode);
        return ruleChain;
    }

    public static DeviceProfile defaultDeviceProfile(String entityName) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(entityName);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        deviceProfileData.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));
        deviceProfileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        deviceProfile.setProfileData(deviceProfileData);
        return deviceProfile;
    }

    public static DeviceProfile defaultDeviceProfile(String entityName, String description) {
        DeviceProfile deviceProfile = new DeviceProfile();
        deviceProfile.setName(entityName);
        deviceProfile.setDescription(description);
        deviceProfile.setType(DeviceProfileType.DEFAULT);
        deviceProfile.setTransportType(DeviceTransportType.DEFAULT);
        deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        DeviceProfileData deviceProfileData = new DeviceProfileData();
        deviceProfileData.setConfiguration(new DefaultDeviceProfileConfiguration());
        deviceProfileData.setProvisionConfiguration(new DisabledDeviceProfileProvisionConfiguration(null));
        deviceProfileData.setTransportConfiguration(new DefaultDeviceProfileTransportConfiguration());
        deviceProfile.setProfileData(deviceProfileData);
        return deviceProfile;
    }

    public static AssetProfile defaultAssetProfile(String entityName) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setName(entityName);
        return assetProfile;
    }

    public static AssetProfile defaultAssetProfile(String entityName, String description) {
        AssetProfile assetProfile = new AssetProfile();
        assetProfile.setName(entityName);
        assetProfile.setDescription(description);
        return assetProfile;
    }

    public static Alarm defaultAlarm(EntityId id, String type) {
        Alarm alarm = new Alarm();
        alarm.setType(type);
        alarm.setOriginator(id);
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        return alarm;
    }

    public static Alarm defaultAlarm(EntityId id, String type, boolean propagate) {
        Alarm alarm = new Alarm();
        alarm.setType(type);
        alarm.setOriginator(id);
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        alarm.setPropagate(propagate);
        return alarm;
    }

    public static Alarm defaultAlarm(EntityId id, String type, UserId userId) {
        Alarm alarm = new Alarm();
        alarm.setType(type);
        alarm.setOriginator(id);
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        alarm.setAssigneeId(userId);
        return alarm;
    }

    public static Alarm defaultAlarm(EntityId id, String type, UserId userId, boolean propagate) {
        Alarm alarm = new Alarm();
        alarm.setType(type);
        alarm.setOriginator(id);
        alarm.setSeverity(AlarmSeverity.CRITICAL);
        alarm.setAssigneeId(userId);
        alarm.setPropagate(propagate);
        return alarm;
    }

    public static User defaultUser(String email, CustomerId customerId) {
        User user = new User();
        user.setEmail(email);
        user.setCustomerId(customerId);
        user.setAuthority(Authority.CUSTOMER_USER);
        return user;
    }

    public static User defaultTenantAdmin(TenantId tenantId, String email) {
        User user = new User();
        user.setTenantId(tenantId);
        user.setEmail(email);
        user.setAuthority(Authority.TENANT_ADMIN);
        return user;
    }

    public static User defaultCustomerAdmin(TenantId tenantId, CustomerId customerId, String email) {
        User user = new User();
        user.setTenantId(tenantId);
        user.setCustomerId(customerId);
        user.setEmail(email);
        user.setAuthority(Authority.CUSTOMER_USER);
        return user;
    }

    public static User defaultUser(String email, CustomerId customerId, String name) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName(name);
        user.setCustomerId(customerId);
        user.setAuthority(Authority.CUSTOMER_USER);
        return user;
    }

    public static Device defaultDevicePrototype(String name) {
        Device device = new Device();
        device.setName(name + RandomStringUtils.randomAlphanumeric(7));
        device.setType("DEFAULT");
        return device;
    }

    public static Device defaultDevicePrototype(String name, CustomerId id) {
        Device device = new Device();
        device.setName(name + RandomStringUtils.randomAlphanumeric(7));
        device.setCustomerId(id);
        device.setType("DEFAULT");
        return device;
    }

    public static Device defaultDevicePrototype(String name, String description) {
        Device device = new Device();
        device.setName(name + RandomStringUtils.randomAlphanumeric(7));
        device.setType("DEFAULT");
        device.setAdditionalInfo(JacksonUtil.newObjectNode().put("description", description));
        return device;
    }

    public static Device defaultDevicePrototype(String name, String description, String label) {
        Device device = new Device();
        device.setName(name + RandomStringUtils.randomAlphanumeric(7));
        device.setType("DEFAULT");
        device.setAdditionalInfo(JacksonUtil.newObjectNode().put("description", description));
        device.setLabel(label);
        return device;
    }

    public static Device defaultDevicePrototype(String name, boolean gateway) {
        Device device = new Device();
        device.setName(name + RandomStringUtils.randomAlphanumeric(7));
        device.setType("DEFAULT");
        device.setAdditionalInfo(JacksonUtil.newObjectNode().put("gateway", gateway));
        return device;
    }

    public static Device defaultDevicePrototype(String name, boolean gateway, boolean overwriteActivityTime) {
        Device device = new Device();
        device.setName(name + RandomStringUtils.randomAlphanumeric(7));
        device.setType("DEFAULT");
        device.setAdditionalInfo(JacksonUtil.newObjectNode()
                .put("gateway", gateway)
                .put("overwriteActivityTime", overwriteActivityTime));
        return device;
    }

    public static Device defaultDevicePrototype(String name, DeviceProfileId deviceProfileId) {
        Device device = new Device();
        device.setName(name + RandomStringUtils.randomAlphanumeric(7));
        device.setType("DEFAULT");
        device.setDeviceProfileId(deviceProfileId);
        return device;
    }

    public static Asset defaultAssetPrototype(String name, CustomerId id) {
        Asset asset = new Asset();
        asset.setName(name + RandomStringUtils.randomAlphanumeric(7));
        asset.setCustomerId(id);
        asset.setType("DEFAULT");
        return asset;
    }

    public static EntityView defaultEntityViewPrototype(String name, String type, String entityType) {
        EntityView entityView = new EntityView();
        entityView.setName(name + RandomStringUtils.randomAlphanumeric(7));
        entityView.setType(type + RandomStringUtils.randomAlphanumeric(7));
        entityView.setAdditionalInfo(JacksonUtil.newObjectNode().put("entityType", entityType));
        return entityView;
    }

    public static Dashboard defaultDashboardPrototype(String title) {
        Dashboard dashboard = new Dashboard();
        dashboard.setTitle(title + RandomStringUtils.randomAlphanumeric(7));
        return dashboard;
    }
}
