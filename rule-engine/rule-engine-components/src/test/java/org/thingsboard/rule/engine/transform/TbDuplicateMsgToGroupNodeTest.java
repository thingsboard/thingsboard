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
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.group.EntityGroupService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TbDuplicateMsgToGroupNodeTest {

    final DeviceId ORIGINATOR_ID = new DeviceId(UUID.fromString("b0b69592-ae0e-4496-a5c7-b4ef81a4461b"));
    final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("ffce9463-8b23-429b-9c0f-322ff12c2cc3"));

    final ListeningExecutor dbCallbackExecutor = DirectListeningExecutor.INSTANCE;

    TbDuplicateMsgToGroupNode node;
    TbDuplicateMsgToGroupNodeConfiguration config;

    @Mock
    TbContext ctxMock;
    @Mock
    TbPeContext peCtxMock;
    @Mock
    EntityGroupService entityGroupServiceMock;

    @BeforeEach
    void setUp() {
        node = new TbDuplicateMsgToGroupNode();

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
        assertThat(config.isEntityGroupIsMessageOriginator()).isEqualTo(true);
        assertThat(config.getEntityGroupId()).isEqualTo(null);
    }

    @Test
    void givenConfigWithUnspecifiedEntityGroupId_whenInit_thenThrowException() {
        // GIVEN-WHEN
        var configuration = new TbDuplicateMsgToGroupNodeConfiguration().defaultConfiguration();
        configuration.setEntityGroupIsMessageOriginator(false);

        assertThatThrownBy(() -> initWithConfig(configuration))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("EntityGroupId should be specified!");
    }

    @Test
    void givenDefaultConfig_whenOnMsg_thenDuplicateToGroupEntities() throws TbNodeException {
        // GIVEN
        init();

        var originator = new EntityGroupId(UUID.randomUUID());
        var msg = getTbMsg(originator);

        EntityId firstUserId = new UserId(UUID.randomUUID());
        EntityId secondUserId = new UserId(UUID.randomUUID());
        var groupUserIdsList = List.of(firstUserId, secondUserId);

        when(entityGroupServiceMock.findAllEntityIdsAsync(TENANT_ID, originator, new PageLink(Integer.MAX_VALUE))).thenReturn(immediateFuture(groupUserIdsList));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(entityGroupServiceMock).findAllEntityIdsAsync(TENANT_ID, originator, new PageLink(Integer.MAX_VALUE));
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

    @Test
    void givenDefaultConfig_whenOnMsg_thenMsgOriginatorIsNotAnEntityGroup() throws TbNodeException {
        // GIVEN
        init();

        var originator = new DeviceId(UUID.randomUUID());
        var msg = getTbMsg(originator);

        // WHEN
        assertThatThrownBy(() -> node.onMsg(ctxMock, msg))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Message originator is not an entity group!");

        // THEN
        verify(entityGroupServiceMock, never())
                .findAllEntityIdsAsync(any(TenantId.class), any(), eq(new PageLink(Integer.MAX_VALUE)));
        verify(ctxMock, never()).ack(any());
        verify(ctxMock, never()).tellFailure(any(), any(Throwable.class));
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).enqueueForTellNext(any(), eq(TbNodeConnectionType.SUCCESS), any(), any());
    }

    @Test
    void givenEntityGroupIdSpecifiedInConfig_whenOnMsg_thenGroupIsFoundWithOneEntity() throws TbNodeException {
        // GIVEN
        var entityGroupId = new EntityGroupId(UUID.randomUUID());

        var configuration = new TbDuplicateMsgToGroupNodeConfiguration().defaultConfiguration();
        configuration.setEntityGroupIsMessageOriginator(false);
        configuration.setEntityGroupId(entityGroupId);
        initWithConfig(configuration);

        var msg = getTbMsg();

        EntityId userId = new UserId(UUID.randomUUID());

        when(entityGroupServiceMock.findAllEntityIdsAsync(TENANT_ID, entityGroupId, new PageLink(Integer.MAX_VALUE))).thenReturn(immediateFuture(List.of(userId)));

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
        verify(entityGroupServiceMock).findAllEntityIdsAsync(TENANT_ID, entityGroupId, new PageLink(Integer.MAX_VALUE));
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

        var originator = new EntityGroupId(UUID.randomUUID());
        var msg = getTbMsg(originator);

        when(entityGroupServiceMock.findAllEntityIdsAsync(TENANT_ID, originator, new PageLink(Integer.MAX_VALUE))).thenReturn(immediateFuture(Collections.emptyList()));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(entityGroupServiceMock).findAllEntityIdsAsync(TENANT_ID, originator, new PageLink(Integer.MAX_VALUE));
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

    static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyUpgradeResultAndConfig() {
        return Stream.of(
                Arguments.of(0, "{\"entityGroupIsMessageOriginator\":true,\"entityGroupId\":null}",
                        false, "{\"entityGroupIsMessageOriginator\":true,\"entityGroupId\":null}"),
                Arguments.of(0, "{\"entityGroupIsMessageOriginator\":true,\"entityGroupId\":null,\"groupOwnerId\":null}",
                        true, "{\"entityGroupIsMessageOriginator\":true,\"entityGroupId\":null}"),
                Arguments.of(0, "{\"entityGroupIsMessageOriginator\":false," +
                                "\"entityGroupId\":{\"entityType\":\"ENTITY_GROUP\",\"id\":\"5817b4c0-2628-11ee-9561-472ccbbe8f70\"}," +
                                "\"groupOwnerId\":{\"entityType\":\"CUSTOMER\",\"id\":\"58139610-2628-11ee-9561-472ccbbe8f70\"}}",
                        true, "{\"entityGroupIsMessageOriginator\":false," +
                                "\"entityGroupId\":{\"entityType\":\"ENTITY_GROUP\",\"id\":\"5817b4c0-2628-11ee-9561-472ccbbe8f70\"}}")
        );
    }

    @ParameterizedTest
    @MethodSource
    void givenFromVersionAndConfig_whenUpgrade_thenVerifyUpgradeResultAndConfig(int givenVersion, String givenConfigStr,
                                                                                boolean hasChanges, String expectedConfigStr) throws Exception {
        // GIVEN
        JsonNode givenConfig = JacksonUtil.toJsonNode(givenConfigStr);
        JsonNode expectedConfig = JacksonUtil.toJsonNode(expectedConfigStr);

        // WHEN
        var upgradeResult = node.upgrade(givenVersion, givenConfig);

        // THEN
        assertThat(upgradeResult.getFirst()).isEqualTo(hasChanges);
        ObjectNode upgradedConfig = (ObjectNode) upgradeResult.getSecond();
        assertThat(upgradedConfig).isEqualTo(expectedConfig);
    }


    void init() throws TbNodeException {
        initWithConfig(new TbDuplicateMsgToGroupNodeConfiguration().defaultConfiguration());
    }

    void initWithConfig(TbDuplicateMsgToGroupNodeConfiguration configuration) throws TbNodeException {
        config = configuration;
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);
    }

    TbMsg getTbMsg() {
        return getTbMsg(ORIGINATOR_ID);
    }

    static TbMsg getTbMsg(EntityId originator) {
        return TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(originator)
                .metaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
    }

}
