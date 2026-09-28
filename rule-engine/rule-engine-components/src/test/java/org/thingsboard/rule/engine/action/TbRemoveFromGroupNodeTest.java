// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.DirectListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbPeContext;
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
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.ota.DeviceGroupOtaPackageService;
import org.thingsboard.server.dao.ota.OtaPackageStateService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TbRemoveFromGroupNodeTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("cb27b618-e85b-4a65-b270-edc4b59fc01f"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("961167a0-c6a7-44a9-ac2b-f1f40102ba97"));
    private final ListeningExecutor dbCallbackExecutor = DirectListeningExecutor.INSTANCE;
    private final EntityGroupId ENTITY_GROUP_ID = new EntityGroupId(UUID.fromString("bf14ba3f-cf37-4b26-b292-79aba01effd7"));

    private TbRemoveFromGroupNode node;
    private TbRemoveFromGroupConfiguration config;

    @Mock
    private TbContext ctxMock;
    @Mock
    private TbPeContext peContextMock;
    @Mock
    private EntityGroupService entityGroupServiceMock;
    @Mock
    private DeviceGroupOtaPackageService deviceGroupOtaPackageServiceMock;
    @Mock
    private OtaPackageStateService otaPackageStateServiceMock;

    @BeforeEach
    public void setUp() {
        node = new TbRemoveFromGroupNode();
        config = new TbRemoveFromGroupConfiguration().defaultConfiguration();
    }

    @Test
    public void givenDeviceWithDeviceGroup_whenOnMsg_thenRemoveFromGroup() throws TbNodeException {
        EntityGroupId entityGroupId = new EntityGroupId(UUID.fromString("bf14ba3f-cf37-4b26-b292-79aba01effd7"));
        config.setGroupNamePattern("${groupName}");
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        initMocks();
        when(entityGroupServiceMock.findEntityGroupByTypeAndNameAsync(any(), any(), any(), any()))
                .thenReturn(Futures.immediateFuture(Optional.of(new EntityGroup(entityGroupId))));
        when(peContextMock.getDeviceGroupOtaPackageService()).thenReturn(deviceGroupOtaPackageServiceMock);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of("groupName", "Device Group")))
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(entityGroupServiceMock).findEntityGroupByTypeAndNameAsync(TENANT_ID, TENANT_ID, EntityType.DEVICE, "Device Group");
        verify(entityGroupServiceMock).removeEntityFromEntityGroup(TENANT_ID, entityGroupId, DEVICE_ID);
        verify(deviceGroupOtaPackageServiceMock).findDeviceGroupOtaPackageByGroupIdAndType(entityGroupId, OtaPackageType.FIRMWARE);
        verify(deviceGroupOtaPackageServiceMock).findDeviceGroupOtaPackageByGroupIdAndType(entityGroupId, OtaPackageType.SOFTWARE);
        verify(ctxMock).tellNext(msg, TbNodeConnectionType.SUCCESS);
        verifyNoMoreInteractions(ctxMock, peContextMock, entityGroupServiceMock, deviceGroupOtaPackageServiceMock);
    }

    @Test
    public void givenDeviceWithoutDeviceGroup_whenOnMsg_thenThrowsException() throws TbNodeException {
        // GIVEN
        config.setGroupNamePattern("${groupName}");
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        initMocks();
        when(entityGroupServiceMock.findEntityGroupByTypeAndNameAsync(any(), any(), any(), any()))
                .thenReturn(Futures.immediateFuture(Optional.empty()));

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of("groupName", "Device Group")))
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        var actualExceptionCaptor = ArgumentCaptor.forClass(Throwable.class);

        verify(ctxMock).tellFailure(eq(msg), actualExceptionCaptor.capture());

        assertThat(actualExceptionCaptor.getValue())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No entity group found with type 'DEVICE' and name 'Device Group'.");

        verifyNoMoreInteractions(ctxMock, peContextMock, entityGroupServiceMock);
    }

    @Test
    public void givenEntityGroupWithFirmwareAndSoftware_whenOnMsg_thenUpdateFirmwareAndSoftwareOnDevice() throws TbNodeException {
        config.setGroupNamePattern("${groupName}");
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, configuration);

        initMocks();
        when(entityGroupServiceMock.findEntityGroupByTypeAndNameAsync(any(), any(), any(), any()))
                .thenReturn(Futures.immediateFuture(Optional.of(new EntityGroup(ENTITY_GROUP_ID))));
        when(peContextMock.getDeviceGroupOtaPackageService()).thenReturn(deviceGroupOtaPackageServiceMock);
        EntityGroup entityGroup = new EntityGroup(ENTITY_GROUP_ID);
        entityGroup.setType(EntityType.DEVICE);
        when(deviceGroupOtaPackageServiceMock.findDeviceGroupOtaPackageByGroupIdAndType(ENTITY_GROUP_ID, OtaPackageType.FIRMWARE))
                .thenReturn(new DeviceGroupOtaPackage());
        when(deviceGroupOtaPackageServiceMock.findDeviceGroupOtaPackageByGroupIdAndType(ENTITY_GROUP_ID, OtaPackageType.SOFTWARE))
                .thenReturn(new DeviceGroupOtaPackage());
        when(ctxMock.getOtaPackageStateService()).thenReturn(otaPackageStateServiceMock);

        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(DEVICE_ID)
                .copyMetaData(new TbMsgMetaData(Map.of("groupName", "Device Group")))
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
        node.onMsg(ctxMock, msg);

        verify(peContextMock).getOwner(TENANT_ID, DEVICE_ID);
        verify(entityGroupServiceMock).findEntityGroupByTypeAndNameAsync(TENANT_ID, TENANT_ID, EntityType.DEVICE, "Device Group");
        verify(entityGroupServiceMock).removeEntityFromEntityGroup(TENANT_ID, ENTITY_GROUP_ID, DEVICE_ID);
        verify(deviceGroupOtaPackageServiceMock).findDeviceGroupOtaPackageByGroupIdAndType(ENTITY_GROUP_ID, OtaPackageType.FIRMWARE);
        verify(deviceGroupOtaPackageServiceMock).findDeviceGroupOtaPackageByGroupIdAndType(ENTITY_GROUP_ID, OtaPackageType.SOFTWARE);
        verify(otaPackageStateServiceMock).update(TENANT_ID, List.of(DEVICE_ID), true, true);
        verify(ctxMock).tellNext(msg, TbNodeConnectionType.SUCCESS);
        verifyNoMoreInteractions(ctxMock, peContextMock, entityGroupServiceMock, deviceGroupOtaPackageServiceMock, otaPackageStateServiceMock);
    }

    @Test
    public void givenDefaultConfig_whenInit_thenOk() {
        var configuration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        assertThatNoException().isThrownBy(() -> node.init(ctxMock, configuration));
    }

    private void initMocks() {
        when(ctxMock.getPeContext()).thenReturn(peContextMock);
        when(peContextMock.getOwner(any(), any())).thenReturn(TENANT_ID);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        when(peContextMock.getEntityGroupService()).thenReturn(entityGroupServiceMock);
        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
    }

}
