// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.DirectListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.ota.DeviceGroupOtaPackage;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;
import org.thingsboard.server.dao.ota.OtaPackageStateService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TbAddToGroupNodeTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("b744dd76-79ea-442c-a7b8-c06c7f568488"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("48641148-1f52-4d9e-ad74-238515365df5"));
    private final EntityGroupId ENTITY_GROUP_ID = new EntityGroupId(UUID.fromString("dc91a98a-055c-4493-aae9-2de393f4f5ab"));
    private final ListeningExecutor dbCallbackExecutor = DirectListeningExecutor.INSTANCE;

    private TbAddToGroupNode node;
    private TbAddToGroupConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private TbPeContext peContextMock;
    @Mock
    private EntityGroupService entityGroupServiceMock;
    @Mock
    private DeviceGroupOtaPackageService deviceGroupOtaPackageService;
    @Mock
    private OtaPackageStateService otaPackageStateService;
    @Mock
    private DeviceService deviceServiceMock;

    @BeforeEach
    public void setUp() {
        node = new TbAddToGroupNode();
        config = new TbAddToGroupConfiguration().defaultConfiguration();
    }

    @Test
    public void givenEntityWithoutEntityGroup_whenOnMsg_thenAddToExistingGroup() throws TbNodeException {
        config.setGroupNamePattern("${groupName}");
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        initMocks();
        when(entityGroupServiceMock.findEntityGroupByTypeAndNameAsync(any(), any(), any(), any()))
                .thenReturn(Futures.immediateFuture(Optional.of(new EntityGroup(ENTITY_GROUP_ID))));

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of("groupName", "Device Group")))
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(entityGroupServiceMock).findEntityGroupByTypeAndNameAsync(TENANT_ID, TENANT_ID, EntityType.DEVICE, "Device Group");
        verify(entityGroupServiceMock).addEntityToEntityGroup(TENANT_ID, ENTITY_GROUP_ID, DEVICE_ID);
        verify(ctxMock).tellNext(msg, TbNodeConnectionType.SUCCESS);
        verifyNoMoreInteractions(ctxMock, peContextMock, entityGroupServiceMock);
    }

    @Test
    public void givenEntityAndEntityGroupNotExistAndCreateGroupIfNotExistsIsTrue_whenOnMsg_thenCreateAndAddEntityToNewGroup() throws TbNodeException {
        config.setGroupNamePattern("${groupName}");
        config.setCreateGroupIfNotExists(true);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        initMocks();
        when(entityGroupServiceMock.findEntityGroupByTypeAndNameAsync(any(), any(), any(), any()))
                .thenReturn(Futures.immediateFuture(Optional.empty()));
        when(entityGroupServiceMock.saveEntityGroup(any(), any(), any())).thenReturn(new EntityGroup(ENTITY_GROUP_ID));

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of("groupName", "Another Device Group")))
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(entityGroupServiceMock).findEntityGroupByTypeAndNameAsync(TENANT_ID, TENANT_ID, EntityType.DEVICE, "Another Device Group");
        EntityGroup newEntityGroup = new EntityGroup();
        newEntityGroup.setName("Another Device Group");
        newEntityGroup.setType(EntityType.DEVICE);
        verify(entityGroupServiceMock).saveEntityGroup(TENANT_ID, TENANT_ID, newEntityGroup);
        verify(entityGroupServiceMock).addEntityToEntityGroup(TENANT_ID, ENTITY_GROUP_ID, DEVICE_ID);
        verify(ctxMock).tellNext(msg, TbNodeConnectionType.SUCCESS);
        verifyNoMoreInteractions(ctxMock, peContextMock, entityGroupServiceMock);
    }

    @Test
    void givenEntityWithEntityGroupAndRemoveFromCurrentGroups_whenOnMsg_thenAddToExistingGroupAndRemoveFromCurrentGroups() throws TbNodeException {
        EntityGroupId currentEntityGroupId = ENTITY_GROUP_ID;
        EntityGroupId newEntityGroupId = new EntityGroupId(UUID.fromString("0aba647a-c081-42da-83eb-2fe9dc920bcb"));
        Device device = new Device(DEVICE_ID);
        device.setTenantId(TENANT_ID);
        config.setGroupNamePattern("${groupName}");
        config.setRemoveFromCurrentGroups(true);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        initMocks();
        when(entityGroupServiceMock.findEntityGroupByTypeAndNameAsync(any(), any(), any(), any()))
                .thenReturn(Futures.immediateFuture(Optional.of(new EntityGroup(newEntityGroupId))));
        when(entityGroupServiceMock.findEntityGroupsForEntityAsync(any(), any()))
                .thenReturn(Futures.immediateFuture(List.of(currentEntityGroupId)));
        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        when(deviceServiceMock.findDeviceById(any(), any())).thenReturn(device);
        when(entityGroupServiceMock.findEntityGroupByTypeAndNameAsync(any(), any(), any(), eq(EntityGroup.GROUP_ALL_NAME)))
                .thenReturn(Futures.immediateFuture(Optional.of(new EntityGroup(currentEntityGroupId))));

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of("groupName", "Device Group")))
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(entityGroupServiceMock).findEntityGroupByTypeAndNameAsync(TENANT_ID, TENANT_ID, EntityType.DEVICE, "Device Group");
        verify(entityGroupServiceMock).findEntityGroupsForEntityAsync(TENANT_ID, DEVICE_ID);
        verify(deviceServiceMock).findDeviceById(TENANT_ID, DEVICE_ID);
        verify(entityGroupServiceMock).findEntityGroupByTypeAndNameAsync(TENANT_ID, TENANT_ID, EntityType.DEVICE, EntityGroup.GROUP_ALL_NAME);
        verify(entityGroupServiceMock).addEntityToEntityGroup(TENANT_ID, newEntityGroupId, DEVICE_ID);
        verify(ctxMock).tellNext(msg, TbNodeConnectionType.SUCCESS);
        verifyNoMoreInteractions(ctxMock, peContextMock, entityGroupServiceMock);
    }

    @Test
    void givenDeviceInOtaGroupAndRemoveFromCurrentGroups_whenNewGroupHasNoOta_thenOtaStateIsUpdatedAfterRemoval() throws TbNodeException {
        EntityGroupId oldGroupId = new EntityGroupId(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
        EntityGroupId groupAllId = new EntityGroupId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
        EntityGroupId newGroupId = ENTITY_GROUP_ID;

        Device device = new Device(DEVICE_ID);
        device.setTenantId(TENANT_ID);

        config.setGroupNamePattern("${groupName}");
        config.setRemoveFromCurrentGroups(true);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        initMocks();
        // Target new group found by name
        when(entityGroupServiceMock.findEntityGroupByTypeAndNameAsync(any(), any(), any(), any()))
                .thenReturn(Futures.immediateFuture(Optional.of(new EntityGroup(newGroupId))));
        // "All" group is separate
        when(entityGroupServiceMock.findEntityGroupByTypeAndNameAsync(any(), any(), any(), eq(EntityGroup.GROUP_ALL_NAME)))
                .thenReturn(Futures.immediateFuture(Optional.of(new EntityGroup(groupAllId))));
        when(entityGroupServiceMock.findEntityGroupsForEntityAsync(any(), any()))
                .thenReturn(Futures.immediateFuture(List.of(oldGroupId)));
        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        when(deviceServiceMock.findDeviceById(any(), any())).thenReturn(device);
        // Old group has firmware OTA, but no software OTA
        when(deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(oldGroupId, OtaPackageType.FIRMWARE))
                .thenReturn(new DeviceGroupOtaPackage());
        // New group has no OTA packages (default mock returns null)
        when(ctxMock.getOtaPackageStateService()).thenReturn(otaPackageStateService);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of("groupName", "New Device Group")))
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        verify(entityGroupServiceMock).removeEntityFromEntityGroup(TENANT_ID, oldGroupId, DEVICE_ID);
        verify(entityGroupServiceMock).addEntityToEntityGroup(TENANT_ID, newGroupId, DEVICE_ID);
        // OTA state must be recalculated after removal from the old group that had firmware OTA,
        // even though the new group has no OTA packages
        verify(otaPackageStateService).update(TENANT_ID, List.of(DEVICE_ID), true, false);
        verify(ctxMock).tellNext(msg, TbNodeConnectionType.SUCCESS);
    }

    @Test
    void givenDeviceMovingBetweenOtaGroups_whenOnMsg_thenTwoOtaUpdatesFireInOrder() throws TbNodeException {
        EntityGroupId oldGroupId = new EntityGroupId(UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"));
        EntityGroupId groupAllId = new EntityGroupId(UUID.fromString("11111111-2222-3333-4444-555555555555"));
        EntityGroupId newGroupId = ENTITY_GROUP_ID;

        Device device = new Device(DEVICE_ID);
        device.setTenantId(TENANT_ID);

        config.setGroupNamePattern("${groupName}");
        config.setRemoveFromCurrentGroups(true);
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        initMocks();
        when(entityGroupServiceMock.findEntityGroupByTypeAndNameAsync(any(), any(), any(), any()))
                .thenReturn(Futures.immediateFuture(Optional.of(new EntityGroup(newGroupId))));
        when(entityGroupServiceMock.findEntityGroupByTypeAndNameAsync(any(), any(), any(), eq(EntityGroup.GROUP_ALL_NAME)))
                .thenReturn(Futures.immediateFuture(Optional.of(new EntityGroup(groupAllId))));
        when(entityGroupServiceMock.findEntityGroupsForEntityAsync(any(), any()))
                .thenReturn(Futures.immediateFuture(List.of(oldGroupId)));
        when(ctxMock.getDeviceService()).thenReturn(deviceServiceMock);
        when(deviceServiceMock.findDeviceById(any(), any())).thenReturn(device);
        // Old group has firmware OTA only
        when(deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(oldGroupId, OtaPackageType.FIRMWARE))
                .thenReturn(new DeviceGroupOtaPackage());
        when(deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(oldGroupId, OtaPackageType.SOFTWARE))
                .thenReturn(null);
        // New group has software OTA only
        when(deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(newGroupId, OtaPackageType.FIRMWARE))
                .thenReturn(null);
        when(deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(newGroupId, OtaPackageType.SOFTWARE))
                .thenReturn(new DeviceGroupOtaPackage());
        when(ctxMock.getOtaPackageStateService()).thenReturn(otaPackageStateService);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of("groupName", "New Device Group")))
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        // Removal from old group (firmware only) must trigger update before add to new group (software only)
        var inOrderVerifier = inOrder(otaPackageStateService);
        inOrderVerifier.verify(otaPackageStateService).update(TENANT_ID, List.of(DEVICE_ID), true, false);
        inOrderVerifier.verify(otaPackageStateService).update(TENANT_ID, List.of(DEVICE_ID), false, true);
        verify(ctxMock).tellNext(msg, TbNodeConnectionType.SUCCESS);
    }

    @Test
    public void givenDefaultConfig_whenInit_thenOk() {
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        assertThatNoException().isThrownBy(() -> node.init(ctxMock, configuration));
    }

    @Test
    public void givenEntityGroupWithFirmware_whenOnMsg_thenUpdateFirmwareOnDevice() throws TbNodeException {
        config.setGroupNamePattern("${groupName}");
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        initMocks();
        when(entityGroupServiceMock.findEntityGroupByTypeAndNameAsync(any(), any(), any(), any()))
                .thenReturn(Futures.immediateFuture(Optional.of(new EntityGroup(ENTITY_GROUP_ID))));
        when(ctxMock.getOtaPackageStateService()).thenReturn(otaPackageStateService);
        when(deviceGroupOtaPackageService.findDeviceGroupOtaPackageByGroupIdAndType(ENTITY_GROUP_ID, OtaPackageType.FIRMWARE))
                .thenReturn(new DeviceGroupOtaPackage());

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of("groupName", "Device Group")))
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(entityGroupServiceMock).findEntityGroupByTypeAndNameAsync(TENANT_ID, TENANT_ID, EntityType.DEVICE, "Device Group");
        verify(entityGroupServiceMock).addEntityToEntityGroup(TENANT_ID, ENTITY_GROUP_ID, DEVICE_ID);
        verify(deviceGroupOtaPackageService).findDeviceGroupOtaPackageByGroupIdAndType(ENTITY_GROUP_ID, OtaPackageType.FIRMWARE);
        verify(deviceGroupOtaPackageService).findDeviceGroupOtaPackageByGroupIdAndType(ENTITY_GROUP_ID, OtaPackageType.SOFTWARE);
        verify(otaPackageStateService).update(TENANT_ID, List.of(DEVICE_ID), true, false);
        verify(ctxMock).tellNext(msg, TbNodeConnectionType.SUCCESS);
        verifyNoMoreInteractions(ctxMock, peContextMock, entityGroupServiceMock, deviceGroupOtaPackageService, otaPackageStateService);
    }

    private void initMocks() {
        when(ctxMock.getPeContext()).thenReturn(peContextMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(peContextMock.getOwner(any(), any())).thenReturn(TENANT_ID);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(peContextMock.getEntityGroupService()).thenReturn(entityGroupServiceMock);
        when(peContextMock.getDeviceGroupOtaPackageService()).thenReturn(deviceGroupOtaPackageService);
    }
}
