/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.device;

import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.SneakyThrows;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.device.credentials.BasicMqttCredentials;
import org.thingsboard.server.common.data.device.credentials.lwm2m.LwM2MClientCredentials;
import org.thingsboard.server.common.data.device.profile.DefaultDeviceProfileConfiguration;
import org.thingsboard.server.common.data.device.profile.DeviceProfileData;
import org.thingsboard.server.common.data.device.profile.DeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.device.profile.DisabledDeviceProfileProvisionConfiguration;
import org.thingsboard.server.common.data.device.profile.Lwm2mDeviceProfileTransportConfiguration;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.data.security.DeviceCredentialsType;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.exception.DeviceCredentialsValidationException;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.action.EntityActionService;
import org.thingsboard.server.service.importing.AbstractBulkImportService;
import org.thingsboard.server.service.importing.BulkImportColumnType;
import org.thingsboard.server.service.importing.BulkImportRequest;
import org.thingsboard.server.service.importing.ImportedEntityInfo;
import org.thingsboard.server.service.security.AccessValidator;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@TbCoreComponent
public class DeviceBulkImportService extends AbstractBulkImportService<Device> {
    protected final DeviceService deviceService;
    protected final DeviceCredentialsService deviceCredentialsService;
    protected final DeviceProfileService deviceProfileService;

    public DeviceBulkImportService(TelemetrySubscriptionService tsSubscriptionService, TbTenantProfileCache tenantProfileCache,
                                   AccessControlService accessControlService, AccessValidator accessValidator,
                                   EntityActionService entityActionService, TbClusterService clusterService,
                                   DeviceService deviceService, DeviceCredentialsService deviceCredentialsService,
                                   DeviceProfileService deviceProfileService) {
        super(tsSubscriptionService, tenantProfileCache, accessControlService, accessValidator, entityActionService, clusterService);
        this.deviceService = deviceService;
        this.deviceCredentialsService = deviceCredentialsService;
        this.deviceProfileService = deviceProfileService;
    }

    @Override
    protected ImportedEntityInfo<Device> saveEntity(BulkImportRequest importRequest, Map<BulkImportRequest.ColumnMapping, String> entityData, SecurityUser user) {
        ImportedEntityInfo<Device> importedEntityInfo = new ImportedEntityInfo<>();

        Device device = new Device();
        device.setTenantId(user.getTenantId());
        setDeviceFields(device, entityData);

        Device existingDevice = deviceService.findDeviceByTenantIdAndName(user.getTenantId(), device.getName());
        if (existingDevice != null && importRequest.getMapping().getUpdate()) {
            importedEntityInfo.setOldEntity(new Device(existingDevice));
            importedEntityInfo.setUpdated(true);
            existingDevice.updateDevice(device);
            device = existingDevice;
        }

        DeviceCredentials deviceCredentials = createDeviceCredentials(entityData);
        if (deviceCredentials.getCredentialsType() != null) {
            if (deviceCredentials.getCredentialsType() == DeviceCredentialsType.LWM2M_CREDENTIALS) {
                setUpLwM2mDeviceProfile(user.getTenantId(), device);
            }
            try {
                device = deviceService.saveDeviceWithCredentials(device, deviceCredentials);
            } catch (DeviceCredentialsValidationException e) {
                if (deviceCredentials.getId() == null) {
                    device.setId(deviceCredentials.getDeviceId());
                    importedEntityInfo.setRelatedError("Failed to create " + deviceCredentials.getCredentialsType() + " credentials: "
                            + e.getMessage() + ". Falling back to access token creds");
                    deviceService.createAccessTokenCredentials(device, null);
                } else {
                    importedEntityInfo.setRelatedError("Failed to update credentials: " + e.getMessage());
                }
            }
        } else {
            device = deviceService.saveDevice(device);
        }

        importedEntityInfo.setEntity(device);

        return importedEntityInfo;
    }

    private void setDeviceFields(Device device, Map<BulkImportRequest.ColumnMapping, String> data) {
        ObjectNode additionalInfo = (ObjectNode) Optional.ofNullable(device.getAdditionalInfo()).orElseGet(JacksonUtil::newObjectNode);
        data.forEach((columnMapping, value) -> {
            switch (columnMapping.getType()) {
                case NAME:
                    device.setName(value);
                    break;
                case TYPE:
                    device.setType(value);
                    break;
                case LABEL:
                    device.setLabel(value);
                    break;
                case DESCRIPTION:
                    additionalInfo.set("description", new TextNode(value));
                    break;
                case IS_GATEWAY:
                    additionalInfo.set("gateway", BooleanNode.valueOf(Boolean.parseBoolean(value)));
                    break;
            }
            device.setAdditionalInfo(additionalInfo);
        });
    }

    @SneakyThrows
    private DeviceCredentials createDeviceCredentials(Map<BulkImportRequest.ColumnMapping, String> data) {
        Set<BulkImportColumnType> columns = data.keySet().stream().map(BulkImportRequest.ColumnMapping::getType).collect(Collectors.toSet());

        DeviceCredentials credentials = new DeviceCredentials();

        if (columns.contains(BulkImportColumnType.ACCESS_TOKEN)) {
            credentials.setCredentialsType(DeviceCredentialsType.ACCESS_TOKEN);
            credentials.setCredentialsId(getByColumnType(BulkImportColumnType.ACCESS_TOKEN, data));
        } else if (CollectionUtils.containsAny(columns, EnumSet.of(BulkImportColumnType.MQTT_CLIENT_ID, BulkImportColumnType.MQTT_USER_NAME, BulkImportColumnType.MQTT_PASSWORD))) {
            credentials.setCredentialsType(DeviceCredentialsType.MQTT_BASIC);

            BasicMqttCredentials basicMqttCredentials = new BasicMqttCredentials();
            basicMqttCredentials.setClientId(getByColumnType(BulkImportColumnType.MQTT_CLIENT_ID, data));
            basicMqttCredentials.setUserName(getByColumnType(BulkImportColumnType.MQTT_USER_NAME, data));
            basicMqttCredentials.setPassword(getByColumnType(BulkImportColumnType.MQTT_PASSWORD, data));
            credentials.setCredentialsValue(JacksonUtil.toString(basicMqttCredentials));
        } else if (columns.contains(BulkImportColumnType.X509)) {
            credentials.setCredentialsType(DeviceCredentialsType.X509_CERTIFICATE);
            credentials.setCredentialsValue(getByColumnType(BulkImportColumnType.X509, data));
        } else if (columns.contains(BulkImportColumnType.LWM2M_CLIENT_ENDPOINT)) {
            credentials.setCredentialsType(DeviceCredentialsType.LWM2M_CREDENTIALS);
            ObjectNode lwm2mCredentials = JacksonUtil.newObjectNode();

            ObjectNode client = JacksonUtil.newObjectNode();
            Stream.of(BulkImportColumnType.LWM2M_CLIENT_ENDPOINT, BulkImportColumnType.LWM2M_CLIENT_SECURITY_CONFIG_MODE,
                    BulkImportColumnType.LWM2M_CLIENT_IDENTITY, BulkImportColumnType.LWM2M_CLIENT_KEY, BulkImportColumnType.LWM2M_CLIENT_CERT)
                    .forEach(lwm2mClientProperty -> {
                        String value = getByColumnType(lwm2mClientProperty, data);
                        if (value != null) {
                            client.set(lwm2mClientProperty.getKey(), new TextNode(value));
                        }
                    });

            LwM2MClientCredentials lwM2MClientCredentials = JacksonUtil.treeToValue(client, LwM2MClientCredentials.class);
            // so that only fields needed for specific type of lwM2MClientCredentials were saved in json
            lwm2mCredentials.set("client", JacksonUtil.valueToTree(lwM2MClientCredentials));

            ObjectNode bootstrapServer = JacksonUtil.newObjectNode();
            Stream.of(BulkImportColumnType.LWM2M_BOOTSTRAP_SERVER_SECURITY_MODE, BulkImportColumnType.LWM2M_BOOTSTRAP_SERVER_PUBLIC_KEY_OR_ID,
                    BulkImportColumnType.LWM2M_BOOTSTRAP_SERVER_SECRET_KEY)
                    .forEach(lwm2mBootstrapServerProperty -> {
                        String value = getByColumnType(lwm2mBootstrapServerProperty, data);
                        if (value != null) {
                            bootstrapServer.set(lwm2mBootstrapServerProperty.getKey(), new TextNode(value));
                        }
                    });

            ObjectNode lwm2mServer = JacksonUtil.newObjectNode();
            Stream.of(BulkImportColumnType.LWM2M_SERVER_SECURITY_MODE, BulkImportColumnType.LWM2M_SERVER_CLIENT_PUBLIC_KEY_OR_ID,
                    BulkImportColumnType.LWM2M_SERVER_CLIENT_SECRET_KEY)
                    .forEach(lwm2mServerProperty -> {
                        String value = getByColumnType(lwm2mServerProperty, data);
                        if (value != null) {
                            lwm2mServer.set(lwm2mServerProperty.getKey(), new TextNode(value));
                        }
                    });

            ObjectNode bootstrap = JacksonUtil.newObjectNode();
            bootstrap.set("bootstrapServer", bootstrapServer);
            bootstrap.set("lwm2mServer", lwm2mServer);
            lwm2mCredentials.set("bootstrap", bootstrap);

            credentials.setCredentialsValue(lwm2mCredentials.toString());
        }

        return credentials;
    }

    private void setUpLwM2mDeviceProfile(TenantId tenantId, Device device) {
        DeviceProfile deviceProfile = deviceProfileService.findDeviceProfileByName(tenantId, device.getType());
        if (deviceProfile != null) {
            if (deviceProfile.getTransportType() != DeviceTransportType.LWM2M) {
                deviceProfile.setTransportType(DeviceTransportType.LWM2M);
                deviceProfile.getProfileData().setTransportConfiguration(new Lwm2mDeviceProfileTransportConfiguration());
                deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
                device.setDeviceProfileId(deviceProfile.getId());
            }
        } else {
            deviceProfile = new DeviceProfile();
            deviceProfile.setTenantId(tenantId);
            deviceProfile.setType(DeviceProfileType.DEFAULT);
            deviceProfile.setName(device.getType());
            deviceProfile.setTransportType(DeviceTransportType.LWM2M);
            deviceProfile.setProvisionType(DeviceProfileProvisionType.DISABLED);

            DeviceProfileData deviceProfileData = new DeviceProfileData();
            DefaultDeviceProfileConfiguration configuration = new DefaultDeviceProfileConfiguration();
            DeviceProfileTransportConfiguration transportConfiguration = new Lwm2mDeviceProfileTransportConfiguration();
            DisabledDeviceProfileProvisionConfiguration provisionConfiguration = new DisabledDeviceProfileProvisionConfiguration(null);

            deviceProfileData.setConfiguration(configuration);
            deviceProfileData.setTransportConfiguration(transportConfiguration);
            deviceProfileData.setProvisionConfiguration(provisionConfiguration);
            deviceProfile.setProfileData(deviceProfileData);

            deviceProfile = deviceProfileService.saveDeviceProfile(deviceProfile);
            device.setDeviceProfileId(deviceProfile.getId());
        }
    }

}
