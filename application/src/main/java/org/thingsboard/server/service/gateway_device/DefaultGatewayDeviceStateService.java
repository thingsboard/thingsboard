package org.thingsboard.server.service.gateway_device;

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
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultGatewayDeviceStateService implements GatewayDeviceStateService {

    private static final String RENAMED_GATEWAY_DEVICES = "renamedGatewayDevices";

    @Lazy
    @Autowired
    private TelemetrySubscriptionService tsSubService;
    private final AttributesService attributesService;
    private final RelationService relationService;

    @Override
    public void update(Device device, Device oldDevice) {
        List<EntityRelation> relationToGatewayList = relationService.findByFromAndType(TenantId.SYS_TENANT_ID, device.getId(), DataConstants.LAST_CONNECTED_GATEWAY, RelationTypeGroup.COMMON);
        if (!relationToGatewayList.isEmpty()) {
            EntityRelation relationToGateway = relationToGatewayList.get(0);
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
                            if (renamedGatewayDevicesNode.findValue(newDeviceName) != null) {
                                // If new device name is the same like the first name
                                renamedGatewayDevicesNode.remove(newDeviceName);
                            } else if (renamedGatewayDevicesNode.findValue(oldDeviceName) == null) {

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

    @Override
    public void delete(Device device) {
        List<EntityRelation> relationToGatewayList = relationService.findByFromAndType(TenantId.SYS_TENANT_ID, device.getId(), DataConstants.LAST_CONNECTED_GATEWAY, RelationTypeGroup.COMMON);
        if (!relationToGatewayList.isEmpty()) {
            EntityRelation relationToGateway = relationToGatewayList.get(0);
            String deletedDeviceName = device.getName();
            ListenableFuture<List<AttributeKvEntry>> renamedGatewayDevicesFuture = attributesService.find(device.getTenantId(), relationToGateway.getTo(), DataConstants.SHARED_SCOPE, Collections.singletonList("renamedGatewayDevices"));
            DonAsynchron.withCallback(renamedGatewayDevicesFuture, renamedGatewayDevicesList -> {
                if (!renamedGatewayDevicesList.isEmpty()) {
                    ObjectNode renamedGatewayDevicesNode = JacksonUtil.fromString(renamedGatewayDevicesList.get(0).getValueAsString(), ObjectNode.class);
                    if (renamedGatewayDevicesNode != null && renamedGatewayDevicesNode.findValue(deletedDeviceName) != null) {
                        renamedGatewayDevicesNode.remove(deletedDeviceName);
                        KvEntry renamedGatewayDevicesKvEntry = new JsonDataEntry(RENAMED_GATEWAY_DEVICES, JacksonUtil.toString(renamedGatewayDevicesNode));
                        saveGatewayDevicesAttribute(device, relationToGateway, renamedGatewayDevicesKvEntry);
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
                deletedGatewayDevicesNode.add(deletedDeviceName);
                KvEntry renamedGatewayDevicesKvEntry = new JsonDataEntry(RENAMED_GATEWAY_DEVICES, JacksonUtil.toString(deletedGatewayDevicesNode));
                saveGatewayDevicesAttribute(device, relationToGateway, renamedGatewayDevicesKvEntry);
            }, e -> log.error("Cannot get gateway deleted devices attribute", e));
        }
    }

    private void saveGatewayDevicesAttribute(Device device, EntityRelation relationToGateway, KvEntry renamedGatewayDevicesKvEntry) {
        AttributeKvEntry attrKvEntry = new BaseAttributeKvEntry(System.currentTimeMillis(), renamedGatewayDevicesKvEntry);
        tsSubService.saveAndNotify(device.getTenantId(), relationToGateway.getTo(), DataConstants.SHARED_SCOPE, List.of(attrKvEntry), true, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void unused) {
                log.trace("Attribute saved for gateway with ID [{}] and data [{}]", relationToGateway.getTo(), renamedGatewayDevicesKvEntry.getJsonValue());
            }

            @Override
            public void onFailure(Throwable throwable) {
                log.error("Cannot save gateway device attribute", throwable);
            }
        });
    }
}