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
package org.thingsboard.server.msa.ui.utils;

import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.rule.RuleChain;

public class EntityPrototypes {

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

    public static RuleChain defaultRuleChainPrototype(String entityName) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(entityName);
        return ruleChain;
    }

    public static RuleChain defaultRuleChainPrototype(String entityName, boolean debugMode) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(entityName);
        ruleChain.setDebugMode(debugMode);
        return ruleChain;
    }

    public static RuleChain defaultRuleChainPrototype(String entityName, String description) {
        RuleChain ruleChain = new RuleChain();
        ruleChain.setName(entityName);
        ruleChain.setAdditionalInfo(JacksonUtil.newObjectNode().put("description", description));
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
}
