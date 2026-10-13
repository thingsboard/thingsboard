// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.transform;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.thingsboard.common.util.DirectListeningExecutor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.TbPeContext;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.group.EntityGroupService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TbDuplicateMsgToGroupByNameNodeTest {

    final DeviceId ORIGINATOR_ID = new DeviceId(UUID.fromString("6c59c85e-8351-435a-98ec-721c627b0de8"));
    final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("8f88de53-4e79-4bb1-bc70-946d9a869458"));

    final ListeningExecutor dbCallbackExecutor = DirectListeningExecutor.INSTANCE;

    TbDuplicateMsgToGroupByNameNode node;
    TbDuplicateMsgToGroupByNameNodeConfiguration config;

    @Mock
    TbContext ctxMock;
    @Mock
    TbPeContext peCtxMock;
    @Mock
    EntityGroupService entityGroupServiceMock;

    @BeforeEach
    void setUp() {
        node = new TbDuplicateMsgToGroupByNameNode();

        lenient().when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        lenient().when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        lenient().when(ctxMock.getPeContext()).thenReturn(peCtxMock);
        lenient().when(peCtxMock.getEntityGroupService()).thenReturn(entityGroupServiceMock);
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenDefaultConfig_whenInit_thenOK() throws TbNodeException {
        // GIVEN-WHEN
        init();

        // THEN
        assertThat(config.getGroupType()).isEqualTo(EntityType.USER);
        assertThat(config.getGroupName()).isEqualTo(EntityGroup.GROUP_ALL_NAME);
        assertThat(config.isSearchEntityGroupForTenantOnly()).isEqualTo(false);
    }

    @Test
    void givenConfigWithUnsupportedGroupType_whenInit_thenThrowException() throws TbNodeException {
        var configuration = new TbDuplicateMsgToGroupByNameNodeConfiguration().defaultConfiguration();
        for (var groupType : EntityType.values()) {
            if (groupType.isGroupEntityType()) {
                configuration.setGroupType(groupType);
                configuration.setGroupName(groupType.getNormalName());
                initWithConfig(configuration);
                assertThat(config.getGroupType()).isEqualTo(groupType);
                assertThat(config.getGroupName()).isEqualTo(groupType.getNormalName());
                assertThat(config.isSearchEntityGroupForTenantOnly()).isEqualTo(false);
            } else {
                configuration.setGroupType(groupType);
                configuration.setGroupName(groupType.getNormalName());
                assertThatThrownBy(() -> initWithConfig(configuration))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("Entity Type :" + config.getGroupType() + " is not a group entity. " +
                                "Only " + EntityType.GROUP_ENTITY_TYPES + " types are allowed!");
            }
        }
    }

    @Test
    void givenConfigWithEmptyName_whenInit_thenThrowException() {
        // GIVEN-WHEN
        var configuration = new TbDuplicateMsgToGroupByNameNodeConfiguration().defaultConfiguration();
        configuration.setGroupName("");
        assertThatThrownBy(() -> initWithConfig(configuration))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Group name should be specified!");
    }

    @Test
    void givenDefaultConfig_whenOnMsg_thenGroupIsNotFound() throws TbNodeException {
        // GIVEN
        init();

        var msg = getTbMsg();
        var ownerId = new CustomerId(UUID.randomUUID());

        when(peCtxMock.getOwner(TENANT_ID, ORIGINATOR_ID)).thenReturn(ownerId);
        when(peCtxMock.getOwner(TENANT_ID, ownerId)).thenReturn(TENANT_ID);
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(
                eq(TENANT_ID), any(), eq(config.getGroupType()), eq(config.getGroupName())))
                .thenReturn(Optional.empty());

        // WHEN
        assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Can't find group with type: %s name: %s!", config.getGroupType(), config.getGroupName());

        // THEN
        verify(peCtxMock, times(2)).getOwner(any(), any());
        verify(entityGroupServiceMock, times(2))
                .findEntityGroupByTypeAndName(any(TenantId.class), any(EntityId.class), any(EntityType.class), anyString());
        verify(ctxMock, never()).ack(any());
        verify(ctxMock, never()).tellFailure(any(), any(Throwable.class));
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).enqueueForTellNext(any(), eq(TbNodeConnectionType.SUCCESS), any(), any());
    }

    @Test
    void givenDefaultConfig_whenOnMsg_thenGroupIsFoundOnTenantLevelWithOneEntity() throws TbNodeException {
        // GIVEN
        init();

        var msg = getTbMsg();

        var ownerId = new CustomerId(UUID.randomUUID());

        var userGroupId = new EntityGroupId(UUID.randomUUID());

        var userGroup = new EntityGroup();
        userGroup.setId(userGroupId);
        userGroup.setName(config.getGroupName());
        userGroup.setType(config.getGroupType());
        userGroup.setOwnerId(TENANT_ID);
        userGroup.setTenantId(TENANT_ID);

        when(peCtxMock.getOwner(eq(TENANT_ID), eq(ORIGINATOR_ID))).thenReturn(ownerId);
        when(peCtxMock.getOwner(eq(TENANT_ID), eq(ownerId))).thenReturn(TENANT_ID);

        when(entityGroupServiceMock.findEntityGroupByTypeAndName(
                eq(TENANT_ID), eq(ownerId), eq(config.getGroupType()), eq(config.getGroupName())))
                .thenReturn(Optional.empty());
        when(entityGroupServiceMock.findEntityGroupByTypeAndName(
                eq(TENANT_ID), eq(TENANT_ID), eq(config.getGroupType()), eq(config.getGroupName())))
                .thenReturn(Optional.of(userGroup));

        EntityId userId = new UserId(UUID.randomUUID());

        when(entityGroupServiceMock.findAllEntityIdsAsync(
                eq(TENANT_ID), eq(userGroupId), eq(new PageLink(Integer.MAX_VALUE))))
                .thenReturn(immediateFuture(List.of(userId)));

        doAnswer((Answer<TbMsg>) invocationOnMock -> {
            TbMsg tbMsg = (TbMsg) (invocationOnMock.getArguments())[0];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[1];
            return tbMsg.transform()
                    .originator(originator)
                    .build();
        }).when(ctxMock).transformMsgOriginator(
                eq(msg),
                eq(userId));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(peCtxMock).getOwner(eq(TENANT_ID), eq(ORIGINATOR_ID));
        verify(peCtxMock).getOwner(eq(TENANT_ID), eq(ownerId));
        verify(entityGroupServiceMock)
                .findEntityGroupByTypeAndName(eq(TENANT_ID), eq(ownerId), eq(config.getGroupType()), eq(config.getGroupName()));
        verify(entityGroupServiceMock)
                .findEntityGroupByTypeAndName(eq(TENANT_ID), eq(TENANT_ID), eq(config.getGroupType()), eq(config.getGroupName()));
        verify(ctxMock, never()).newMsg(anyString(),
                anyString(),
                any(EntityId.class),
                any(CustomerId.class),
                any(TbMsgMetaData.class),
                anyString());
        verify(ctxMock, never()).tellFailure(any(), any(Throwable.class));
        verify(ctxMock, never()).enqueueForTellNext(any(), eq(TbNodeConnectionType.SUCCESS), any(), any());
        verify(ctxMock, never()).ack(any());

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctxMock).tellSuccess(newMsgCaptor.capture());
        var actualMsg = newMsgCaptor.getValue();

        assertThat(actualMsg).isNotNull();
        assertThat(actualMsg).isNotSameAs(msg);
        assertThat(actualMsg.getType()).isSameAs(msg.getType());
        assertThat(actualMsg.getData()).isSameAs(msg.getData());
        assertThat(actualMsg.getMetaData()).isEqualTo(msg.getMetaData());
        assertThat(actualMsg.getOriginator()).isSameAs(userId);
    }

    @Test
    void givenDefaultConfig_whenOnMsg_thenGroupIsFoundWithNoEntitiesInside() throws TbNodeException {
        // GIVEN
        init();

        var msg = getTbMsg();

        var ownerId = new CustomerId(UUID.randomUUID());

        var userGroupId = new EntityGroupId(UUID.randomUUID());

        var userGroup = new EntityGroup();
        userGroup.setId(userGroupId);
        userGroup.setName(config.getGroupName());
        userGroup.setType(config.getGroupType());
        userGroup.setOwnerId(ownerId);
        userGroup.setTenantId(TENANT_ID);

        when(peCtxMock.getOwner(TENANT_ID, ORIGINATOR_ID)).thenReturn(ownerId);

        when(entityGroupServiceMock.findEntityGroupByTypeAndName(
                eq(TENANT_ID), eq(ownerId), eq(config.getGroupType()), eq(config.getGroupName())))
                .thenReturn(Optional.of(userGroup));
        when(entityGroupServiceMock.findAllEntityIdsAsync(
                eq(TENANT_ID), eq(userGroupId), eq(new PageLink(Integer.MAX_VALUE))))
                .thenReturn(immediateFuture(Collections.emptyList()));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(peCtxMock).getOwner(eq(TENANT_ID), eq(ORIGINATOR_ID));
        verify(entityGroupServiceMock)
                .findEntityGroupByTypeAndName(eq(TENANT_ID), eq(ownerId), eq(config.getGroupType()), eq(config.getGroupName()));
        verify(ctxMock, never()).newMsg(anyString(),
                anyString(),
                any(EntityId.class),
                any(CustomerId.class),
                any(TbMsgMetaData.class),
                anyString());
        verify(ctxMock, never()).transformMsgOriginator(any(TbMsg.class), any(EntityId.class));
        verify(ctxMock, never()).enqueueForTellNext(any(), eq(TbNodeConnectionType.SUCCESS), any(), any());
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).ack(msg);

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctxMock).tellFailure(eq(msg), throwableCaptor.capture());

        String expectedExceptionMessage = "Message or messages list are empty!";

        Throwable actualThrowable = throwableCaptor.getValue();
        assertInstanceOf(RuntimeException.class, actualThrowable);
        assertThat(actualThrowable.getMessage()).isEqualTo(expectedExceptionMessage);
    }

    @Test
    void givenSearchOnlyOnTenantLevel_whenOnMsg_thenDuplicateToGroupEntities() throws TbNodeException {
        // GIVEN
        var configuration = new TbDuplicateMsgToGroupByNameNodeConfiguration().defaultConfiguration();
        configuration.setConsiderMessageOriginatorAsAGroupOwner(false);
        configuration.setSearchEntityGroupForTenantOnly(true);
        initWithConfig(configuration);

        var msg = getTbMsg();

        var userGroupId = new EntityGroupId(UUID.randomUUID());

        var userGroup = new EntityGroup();
        userGroup.setId(userGroupId);
        userGroup.setName(config.getGroupName());
        userGroup.setType(config.getGroupType());
        userGroup.setOwnerId(TENANT_ID);
        userGroup.setTenantId(TENANT_ID);

        EntityId firstUserId = new UserId(UUID.randomUUID());
        EntityId secondUserId = new UserId(UUID.randomUUID());
        var groupUserIdsList = List.of(firstUserId, secondUserId);

        when(entityGroupServiceMock.findEntityGroupByTypeAndName(TENANT_ID, TENANT_ID, config.getGroupType(), config.getGroupName())).thenReturn(Optional.of(userGroup));
        when(entityGroupServiceMock.findAllEntityIdsAsync(TENANT_ID, userGroupId, new PageLink(Integer.MAX_VALUE))).thenReturn(immediateFuture(groupUserIdsList));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(peCtxMock, never()).getOwner(any(), any());
        verify(entityGroupServiceMock).findEntityGroupByTypeAndName(TENANT_ID, TENANT_ID, config.getGroupType(), config.getGroupName());
        verify(ctxMock, never()).transformMsgOriginator(any(TbMsg.class), any(EntityId.class));
        verify(ctxMock, never()).tellFailure(any(), any(Throwable.class));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> onSuccessCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> onFailureCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(ctxMock, times(groupUserIdsList.size())).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS), onSuccessCaptor.capture(), onFailureCaptor.capture());
        for (Runnable successCaptor : onSuccessCaptor.getAllValues()) {
            successCaptor.run();
        }
        verify(ctxMock).ack(msg);

        List<TbMsg> allValues = newMsgCaptor.getAllValues();
        IntStream.range(0, allValues.size()).forEach(i -> {
            TbMsg newMsg = allValues.get(i);

            assertThat(newMsg).usingRecursiveComparison().ignoringFields("id", "originator").isEqualTo(msg);
            assertThat(newMsg.getOriginator()).isEqualTo(groupUserIdsList.get(i));
            assertThat(newMsg.getId()).isNotNull().isNotEqualTo(msg.getId());
        });
    }

    static Stream<Arguments> givenConsiderMessageOriginatorAsAGroupOwner_whenOnMsg_thenDuplicateToOriginatorEntities() {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        return Stream.of(
                // default config with originator is Customer
                Arguments.of(tenantId, new CustomerId(UUID.randomUUID())),
                // default config with originator is Tenant
                Arguments.of(tenantId, tenantId)
        );
    }

    @ParameterizedTest
    @MethodSource
    void givenConsiderMessageOriginatorAsAGroupOwner_whenOnMsg_thenDuplicateToOriginatorEntities(TenantId tenantId, EntityId originatorId) throws TbNodeException {
        // GIVEN
        var configuration = new TbDuplicateMsgToGroupByNameNodeConfiguration().defaultConfiguration();
        initWithConfig(configuration);

        var msg = getTbMsgByOriginator(originatorId);

        var userGroupId = new EntityGroupId(UUID.randomUUID());

        var userGroup = new EntityGroup();
        userGroup.setId(userGroupId);
        userGroup.setName(config.getGroupName());
        userGroup.setType(config.getGroupType());
        userGroup.setOwnerId(originatorId);
        userGroup.setTenantId(tenantId);

        EntityId firstUserId = new UserId(UUID.randomUUID());
        EntityId secondUserId = new UserId(UUID.randomUUID());
        var groupUserIdsList = List.of(firstUserId, secondUserId);

        when(ctxMock.getTenantId()).thenReturn(tenantId);

        when(entityGroupServiceMock.findEntityGroupByTypeAndName(tenantId, originatorId, config.getGroupType(), config.getGroupName())).thenReturn(Optional.of(userGroup));
        when(entityGroupServiceMock.findAllEntityIdsAsync(tenantId, userGroupId, new PageLink(Integer.MAX_VALUE))).thenReturn(immediateFuture(groupUserIdsList));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(peCtxMock, never()).getOwner(any(), any());
        verify(entityGroupServiceMock).findEntityGroupByTypeAndName(tenantId, originatorId, config.getGroupType(), config.getGroupName());
        verify(ctxMock, never()).transformMsgOriginator(any(TbMsg.class), any(EntityId.class));
        verify(ctxMock, never()).tellFailure(any(), any(Throwable.class));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> onSuccessCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> onFailureCaptor = ArgumentCaptor.forClass(Consumer.class);
        verify(ctxMock, times(groupUserIdsList.size())).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS), onSuccessCaptor.capture(), onFailureCaptor.capture());
        for (Runnable successCaptor : onSuccessCaptor.getAllValues()) {
            successCaptor.run();
        }
        verify(ctxMock).ack(msg);
        List<TbMsg> allValues = newMsgCaptor.getAllValues();
        IntStream.range(0, allValues.size()).forEach(i -> {
            TbMsg newMsg = allValues.get(i);

            assertThat(newMsg).usingRecursiveComparison().ignoringFields("id", "originator").isEqualTo(msg);
            assertThat(newMsg.getOriginator()).isEqualTo(groupUserIdsList.get(i));
            assertThat(newMsg.getId()).isNotNull().isNotEqualTo(msg.getId());
        });
    }

    static Stream<Arguments> givenGroupName_whenOnMsg_thenDuplicateToGroupEntities() {
        return Stream.of(
                // config with entity group name as string
                Arguments.of("Entity Group Name"),
                // config with entity group name as metadata key from metadata
                Arguments.of("${groupNameMetaData}"),
                // config with entity group name as message key from message body
                Arguments.of("$[groupName]")
        );
    }

    @ParameterizedTest
    @MethodSource
    void givenGroupName_whenOnMsg_thenDuplicateToGroupEntities(String groupName) throws TbNodeException {
        // GIVEN
        var configuration = new TbDuplicateMsgToGroupByNameNodeConfiguration().defaultConfiguration();
        configuration.setConsiderMessageOriginatorAsAGroupOwner(true);
        configuration.setGroupType(EntityType.DEVICE);
        configuration.setGroupName(groupName);
        initWithConfig(configuration);
        String entityGroupName = "Entity Group Name";

        var msg = getTbMsgWithBody(TENANT_ID);

        var deviceGroupId = new EntityGroupId(UUID.randomUUID());

        var deviceGroup = new EntityGroup();
        deviceGroup.setId(deviceGroupId);
        deviceGroup.setName(config.getGroupName());
        deviceGroup.setType(config.getGroupType());
        deviceGroup.setTenantId(TENANT_ID);

        EntityId firstDeviceId = new DeviceId(UUID.randomUUID());
        EntityId secondDeviceId = new DeviceId(UUID.randomUUID());
        var groupDeviceIdsList = List.of(firstDeviceId, secondDeviceId);

        when(entityGroupServiceMock.findEntityGroupByTypeAndName(
                any(), any(), any(), any()))
                .thenReturn(Optional.of(deviceGroup));
        when(entityGroupServiceMock.findAllEntityIdsAsync(
                any(), any(), any()))
                .thenReturn(immediateFuture(groupDeviceIdsList));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(entityGroupServiceMock)
                .findEntityGroupByTypeAndName(eq(TENANT_ID), eq(TENANT_ID), eq(config.getGroupType()), eq(entityGroupName));
    }

    void init() throws TbNodeException {
        initWithConfig(new TbDuplicateMsgToGroupByNameNodeConfiguration().defaultConfiguration());
    }

    void initWithConfig(TbDuplicateMsgToGroupByNameNodeConfiguration configuration) throws TbNodeException {
        config = configuration;
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);
    }

    TbMsg getTbMsgWithBody(EntityId originatorId) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("groupNameMetaData", "Entity Group Name");
        return TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(originatorId)
                .copyMetaData(metaData)
                .data("{ \"temp\": 44, \"humidity\": 86, \"groupName\": \"Entity Group Name\" }")
                .build();
    }

    TbMsg getTbMsg() {
        return getTbMsgByOriginator(ORIGINATOR_ID);
    }

    TbMsg getTbMsgByOriginator(EntityId originatorId) {
        return TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(originatorId)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
    }

    // Rule nodes upgrade
    static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"searchEntityGroupForTenantOnly\":false,\"groupType\":\"USER\",\"groupName\":\"All\"}",
                        true,
                        "{\"searchEntityGroupForTenantOnly\":false,\"considerMessageOriginatorAsAGroupOwner\":false,\"groupType\":\"USER\",\"groupName\":\"All\"}"),
                // default config for version 1 with upgrade from version 0
                Arguments.of(0,
                        "{\"searchEntityGroupForTenantOnly\":false,\"considerMessageOriginatorAsAGroupOwner\":false,\"groupType\":\"USER\",\"groupName\":\"All\"}",
                        false,
                        "{\"searchEntityGroupForTenantOnly\":false,\"considerMessageOriginatorAsAGroupOwner\":false,\"groupType\":\"USER\",\"groupName\":\"All\"}")
        );
    }

    @ParameterizedTest
    @MethodSource
    void givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig(int givenVersion, String givenConfigStr, boolean hasChanges, String expectedConfigStr) throws TbNodeException {
        // GIVEN
        JsonNode givenConfig = JacksonUtil.toJsonNode(givenConfigStr);
        JsonNode expectedConfig = JacksonUtil.toJsonNode(expectedConfigStr);

        // WHEN
        TbPair<Boolean, JsonNode> upgradeResult = node.upgrade(givenVersion, givenConfig);

        // THEN
        assertThat(upgradeResult.getFirst()).isEqualTo(hasChanges);
        ObjectNode upgradedConfig = (ObjectNode) upgradeResult.getSecond();
        assertThat(upgradedConfig).isEqualTo(expectedConfig);
    }

}
