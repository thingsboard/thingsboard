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
package org.thingsboard.server.service.gateway_device;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultGatewayDeviceStateService implements GatewayDeviceStateService {

    private static final String RENAMED_GATEWAY_DEVICES = "renamedGatewayDevices";
    private static final String DELETED_GATEWAY_DEVICES = "deletedGatewayDevices";
    private static final String HANDLE_DEVICE_RENAMING_PARAMETER = "handleDeviceRenaming";

    private final AttributesService attributesService;
    private final RelationService relationService;
    private final DeviceService deviceService;
    @Lazy
    @Autowired
    private TelemetrySubscriptionService tsSubService;

    @Override
    public void update(Device device, Device oldDevice) {
        List<EntityRelation> relationToGatewayList = relationService.findByFromAndType(TenantId.SYS_TENANT_ID, device.getId(), DataConstants.LAST_CONNECTED_GATEWAY, RelationTypeGroup.COMMON);
        if (!relationToGatewayList.isEmpty()) {
            if (oldDevice != null) {
                EntityRelation relationToGateway = relationToGatewayList.get(0);

                Device gatewayDevice = deviceService.findDeviceById(device.getTenantId(), (DeviceId) relationToGateway.getTo());
                if (isHandleDeviceRenamingEnabled(gatewayDevice.getAdditionalInfo())) {
                    ListenableFuture<List<AttributeKvEntry>> renamedGatewayDevicesFuture = attributesService.find(device.getTenantId(), relationToGateway.getTo(), DataConstants.SHARED_SCOPE, Collections.singletonList("renamedGatewayDevices"));
                    DonAsynchron.withCallback(renamedGatewayDevicesFuture, renamedGatewayDevicesList -> {
                                ObjectNode renamedGatewayDevicesNode;
                                KvEntry renamedGatewayDevicesKvEntry;
                                String newDeviceName = device.getName();
                                String oldDeviceName = oldDevice.getName();

                                if (renamedGatewayDevicesList.isEmpty()) {
                                    renamedGatewayDevicesNode = JacksonUtil.newObjectNode();
                                    renamedGatewayDevicesNode.put(oldDeviceName, newDeviceName);
                                } else {
                                    AttributeKvEntry receivedRenamedGatewayDevicesAttribute = renamedGatewayDevicesList.get(0);
                                    renamedGatewayDevicesNode = (ObjectNode) JacksonUtil.toJsonNode(receivedRenamedGatewayDevicesAttribute.getValueAsString());
                                    if (renamedGatewayDevicesNode.findValue(newDeviceName) != null && oldDeviceName.equals(renamedGatewayDevicesNode.get(newDeviceName).asText())) {
                                        // If a new device name is the same like the first name or another device was renamed like some existing device
                                        renamedGatewayDevicesNode.remove(newDeviceName);
                                    } else {

                                        AtomicBoolean renamedFirstTime = new AtomicBoolean(true);

                                        renamedGatewayDevicesNode.fields().forEachRemaining(entry -> {
                                            // If device was renamed earlier
                                            if (oldDeviceName.equals(entry.getValue().asText())) {
                                                renamedGatewayDevicesNode.put(entry.getKey(), newDeviceName);
                                                renamedFirstTime.set(false);
                                            }
                                        });
                                        if (renamedFirstTime.get()) {
                                            renamedGatewayDevicesNode.put(oldDeviceName, newDeviceName);
                                        }
                                    }
                                }

                                renamedGatewayDevicesKvEntry = new JsonDataEntry(RENAMED_GATEWAY_DEVICES, JacksonUtil.toString(renamedGatewayDevicesNode));
                                saveGatewayDevicesAttribute(device, relationToGateway, renamedGatewayDevicesKvEntry);
                            },
                            e -> log.error("Cannot get gateway renamed devices attribute", e));
                }
            }
        }
    }

    @Override
    public void delete(Device device) {
        List<EntityRelation> relationToGatewayList = relationService.findByFromAndType(TenantId.SYS_TENANT_ID, device.getId(), DataConstants.LAST_CONNECTED_GATEWAY, RelationTypeGroup.COMMON);
        if (!relationToGatewayList.isEmpty()) {
            EntityRelation relationToGateway = relationToGatewayList.get(0);
            final String[] deletedDeviceName = {device.getName()};
            ListenableFuture<List<AttributeKvEntry>> renamedGatewayDevicesFuture = attributesService.find(device.getTenantId(), relationToGateway.getTo(), DataConstants.SHARED_SCOPE, Collections.singletonList("renamedGatewayDevices"));
            DonAsynchron.withCallback(renamedGatewayDevicesFuture, renamedGatewayDevicesList -> {
                if (!renamedGatewayDevicesList.isEmpty()) {
                    ObjectNode renamedGatewayDevicesNode = (ObjectNode) JacksonUtil.toJsonNode(renamedGatewayDevicesList.get(0).getValueAsString());
                    if (renamedGatewayDevicesNode != null) {
                        AtomicBoolean renamedListChanged = new AtomicBoolean(false);
                        if (renamedGatewayDevicesNode.findValue(deletedDeviceName[0]) != null) {
                            renamedGatewayDevicesNode.remove(deletedDeviceName[0]);
                            renamedListChanged.set(true);
                        }
                        Map<String, String> renamedGatewayDevicesMap = JacksonUtil.OBJECT_MAPPER.convertValue(renamedGatewayDevicesNode, new TypeReference<>() {
                        });
                        renamedGatewayDevicesMap.forEach((key, value) -> {
                            // If device was renamed earlier
                            if (deletedDeviceName[0].equals(value)) {
                                renamedGatewayDevicesNode.remove(key);
                                deletedDeviceName[0] = key;
                                renamedListChanged.set(true);
                            }
                        });
                        if (renamedListChanged.get()) {
                            KvEntry renamedGatewayDevicesKvEntry = new JsonDataEntry(RENAMED_GATEWAY_DEVICES, JacksonUtil.toString(renamedGatewayDevicesNode));
                            saveGatewayDevicesAttribute(device, relationToGateway, renamedGatewayDevicesKvEntry);
                        }
                    }
                }
            }, e -> log.error("Cannot get gateway renamed devices attribute", e));
            ListenableFuture<List<AttributeKvEntry>> deletedGatewayDevicesFuture = attributesService.find(device.getTenantId(), relationToGateway.getTo(), DataConstants.SHARED_SCOPE, Collections.singletonList("deletedGatewayDevices"));
            DonAsynchron.withCallback(deletedGatewayDevicesFuture, deletedGatewayDevicesList -> {
                ArrayNode deletedGatewayDevicesNode;
                if (!deletedGatewayDevicesList.isEmpty()) {
                    deletedGatewayDevicesNode = (ArrayNode) JacksonUtil.toJsonNode(deletedGatewayDevicesList.get(0).getValueAsString());
                } else {
                    deletedGatewayDevicesNode = JacksonUtil.OBJECT_MAPPER.createArrayNode();
                }
                deletedGatewayDevicesNode.add(deletedDeviceName[0]);
                KvEntry deletedGatewayDevicesKvEntry = new JsonDataEntry(DELETED_GATEWAY_DEVICES, JacksonUtil.toString(deletedGatewayDevicesNode));
                saveGatewayDevicesAttribute(device, relationToGateway, deletedGatewayDevicesKvEntry);
            }, e -> log.error("Cannot get gateway deleted devices attribute", e));
        }
    }

    @Override
    public void checkAndUpdateDeletedGatewayDevicesAttribute(Device device) {
        List<EntityRelation> relationToGatewayList = relationService.findByFromAndType(TenantId.SYS_TENANT_ID, device.getId(), DataConstants.LAST_CONNECTED_GATEWAY, RelationTypeGroup.COMMON);
        if (!relationToGatewayList.isEmpty()) {
            EntityRelation relationToGateway = relationToGatewayList.get(0);
            ListenableFuture<List<AttributeKvEntry>> deletedGatewayDevicesFuture = attributesService.find(device.getTenantId(), relationToGateway.getTo(), DataConstants.SHARED_SCOPE, Collections.singletonList("deletedGatewayDevices"));
            DonAsynchron.withCallback(deletedGatewayDevicesFuture, deletedGatewayDevicesList -> {
                ArrayNode deletedGatewayDevicesNode;
                if (!deletedGatewayDevicesList.isEmpty()) {
                    int deletedDeviceIndex = -1;
                    deletedGatewayDevicesNode = (ArrayNode) JacksonUtil.toJsonNode(deletedGatewayDevicesList.get(0).getValueAsString());
                    for (int i = 0; i < deletedGatewayDevicesNode.size(); i++) {
                        if (deletedGatewayDevicesNode.get(i).asText().equals(device.getName())) {
                            deletedDeviceIndex = i;
                        }
                    }
                    if (deletedDeviceIndex != -1) {
                        deletedGatewayDevicesNode.remove(deletedDeviceIndex);
                        KvEntry deletedGatewayDevicesKvEntry = new JsonDataEntry(DELETED_GATEWAY_DEVICES, JacksonUtil.toString(deletedGatewayDevicesNode));
                        saveGatewayDevicesAttribute(device, relationToGateway, deletedGatewayDevicesKvEntry);
                    }
                }
            }, e -> log.error("Cannot get gateway deleted devices attribute", e));
        }
    }

    private void saveGatewayDevicesAttribute(Device device, EntityRelation relationToGateway, KvEntry gatewayDevicesKvEntry) {
        AttributeKvEntry attrKvEntry = new BaseAttributeKvEntry(System.currentTimeMillis(), gatewayDevicesKvEntry);
        tsSubService.saveAndNotify(device.getTenantId(), relationToGateway.getTo(), DataConstants.SHARED_SCOPE, List.of(attrKvEntry), true, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void unused) {
                log.trace("Attribute saved for gateway with ID [{}] and data [{}]", relationToGateway.getTo(), gatewayDevicesKvEntry.getJsonValue());
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("Cannot save gateway device attribute", throwable);
            }
        });
    }

    private boolean isHandleDeviceRenamingEnabled(JsonNode additionalInfo) {
        if (additionalInfo.get(HANDLE_DEVICE_RENAMING_PARAMETER) != null) {
            return additionalInfo.get(HANDLE_DEVICE_RENAMING_PARAMETER).asBoolean();
        }
        return false;
    }
}