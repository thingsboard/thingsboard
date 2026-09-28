// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.transform;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.IntStream;

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
class TbDuplicateMsgToRelatedNodeTest {

    final DeviceId ORIGINATOR_ID = new DeviceId(UUID.fromString("61e68586-466f-41e3-abca-9457b80da8d6"));
    final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("9cc6c3c8-b90f-4d00-bfb2-fd38ccf79f63"));

    final ListeningExecutor dbCallbackExecutor = DirectListeningExecutor.INSTANCE;

    TbDuplicateMsgToRelatedNode node;
    TbDuplicateMsgToRelatedNodeConfiguration config;

    @Mock
    TbContext ctxMock;
    @Mock
    RelationService relationServiceMock;

    @BeforeEach
    void setUp() {
        node = new TbDuplicateMsgToRelatedNode();

        lenient().when(ctxMock.getDbCallbackExecutor()).thenReturn(dbCallbackExecutor);
        lenient().when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        lenient().when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
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
        assertThat(config.getRelationsQuery()).isEqualTo(getDefaultRelationQuery());
    }

    @Test
    void givenConfigWithUnspecifiedRelationQuery_whenInit_thenThrowException() {
        // GIVEN-WHEN
        var configuration = new TbDuplicateMsgToRelatedNodeConfiguration().defaultConfiguration();
        configuration.setRelationsQuery(null);
        assertThatThrownBy(() -> initWithConfig(configuration))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Relation query should be specified!");
    }

    @Test
    void givenDefaultConfig_whenOnMsg_thenDuplicateToRelatedEntities() throws TbNodeException {
        // GIVEN
        init();

        var msg = getTbMsg();

        EntityId firstRelatedEntityId = new AssetId(UUID.randomUUID());
        var firstRelation = new EntityRelation();
        firstRelation.setFrom(ORIGINATOR_ID);
        firstRelation.setTo(firstRelatedEntityId);
        firstRelation.setTypeGroup(RelationTypeGroup.COMMON);
        firstRelation.setType(EntityRelation.CONTAINS_TYPE);

        EntityId secondRelatedEntityId = new AssetId(UUID.randomUUID());
        var secondRelation = new EntityRelation();
        firstRelation.setFrom(ORIGINATOR_ID);
        firstRelation.setTo(secondRelatedEntityId);
        firstRelation.setTypeGroup(RelationTypeGroup.COMMON);
        firstRelation.setType(EntityRelation.CONTAINS_TYPE);

        var relationList = List.of(firstRelation, secondRelation);

        var entityRelationsQuery = buildQuery(config.getRelationsQuery());
        when(relationServiceMock.findByQuery(TENANT_ID, entityRelationsQuery)).thenReturn(immediateFuture(relationList));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(relationServiceMock).findByQuery(TENANT_ID, entityRelationsQuery);
        verify(ctxMock, never()).transformMsgOriginator(any(TbMsg.class), any(EntityId.class));
        verify(ctxMock, never()).tellFailure(any(), any(Throwable.class));

        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<Runnable> onSuccessCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Consumer<Throwable>> onFailureCaptor = ArgumentCaptor.forClass(Consumer.class);

        verify(ctxMock, times(relationList.size())).enqueueForTellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.SUCCESS), onSuccessCaptor.capture(), onFailureCaptor.capture());

        for (Runnable successCaptor : onSuccessCaptor.getAllValues()) {
            successCaptor.run();
        }

        verify(ctxMock).ack(msg);

        List<TbMsg> allValues = newMsgCaptor.getAllValues();
        IntStream.range(0, allValues.size()).forEach(i -> {
            TbMsg newMsg = allValues.get(i);

            assertThat(newMsg).usingRecursiveComparison().ignoringFields("id", "originator").isEqualTo(msg);
            assertThat(newMsg.getOriginator()).isEqualTo(relationList.get(i).getTo());
            assertThat(newMsg.getId()).isNotNull().isNotEqualTo(msg.getId());
        });
    }

    @Test
    void givenDefaultConfig_whenOnMsg_thenOneRelatedEntityFound() throws TbNodeException {
        init();

        var msg = getTbMsg();

        EntityId relatedEntityId = new AssetId(UUID.randomUUID());
        var relation = new EntityRelation();
        relation.setFrom(ORIGINATOR_ID);
        relation.setTo(relatedEntityId);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType(EntityRelation.CONTAINS_TYPE);

        var entityRelationsQuery = buildQuery(config.getRelationsQuery());
        when(relationServiceMock.findByQuery(TENANT_ID, entityRelationsQuery)).thenReturn(immediateFuture(List.of(relation)));

        doAnswer((Answer<TbMsg>) invocationOnMock -> {
            TbMsg tbMsg = (TbMsg) (invocationOnMock.getArguments())[0];
            EntityId originator = (EntityId) (invocationOnMock.getArguments())[1];
            return tbMsg.transform()
                    .originator(originator)
                    .build();
        }).when(ctxMock).transformMsgOriginator(
                eq(msg),
                eq(relatedEntityId));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(relationServiceMock).findByQuery(TENANT_ID, entityRelationsQuery);
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
        assertThat(actualMsg.getOriginator()).isSameAs(relatedEntityId);
    }

    @Test
    void givenDefaultConfig_whenOnMsg_thenNoRelatedEntitiesFound() throws TbNodeException {
        init();

        var msg = getTbMsg();

        var entityRelationsQuery = buildQuery(config.getRelationsQuery());
        when(relationServiceMock.findByQuery(TENANT_ID, entityRelationsQuery)).thenReturn(immediateFuture(Collections.emptyList()));

        // WHEN
        node.onMsg(ctxMock, msg);

        // THEN
        verify(relationServiceMock).findByQuery(TENANT_ID, entityRelationsQuery);
        verify(ctxMock, never()).newMsg(anyString(),
                anyString(),
                any(EntityId.class),
                any(CustomerId.class),
                any(TbMsgMetaData.class),
                anyString());
        verify(ctxMock, never()).transformMsgOriginator(any(TbMsg.class), any(EntityId.class));
        verify(ctxMock, never()).enqueueForTellNext(any(), eq(TbNodeConnectionType.SUCCESS), any(), any());
        verify(ctxMock, never()).tellSuccess(any());
        verify(ctxMock, never()).ack(any());

        ArgumentCaptor<Throwable> throwableCaptor = ArgumentCaptor.forClass(Throwable.class);

        verify(ctxMock).tellFailure(eq(msg), throwableCaptor.capture());

        String expectedExceptionMessage = "No related entities were found!";

        Throwable actualThrowable = throwableCaptor.getValue();
        assertInstanceOf(RuntimeException.class, actualThrowable);
        assertThat(actualThrowable.getMessage()).isEqualTo(expectedExceptionMessage);
    }

    void init() throws TbNodeException {
        initWithConfig(new TbDuplicateMsgToRelatedNodeConfiguration().defaultConfiguration());
    }

    void initWithConfig(TbDuplicateMsgToRelatedNodeConfiguration configuration) throws TbNodeException {
        config = configuration;
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        node.init(ctxMock, nodeConfiguration);
    }

    TbMsg getTbMsg() {
        return TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(ORIGINATOR_ID)
                .copyMetaData(TbMsgMetaData.EMPTY)
                .data(TbMsg.EMPTY_JSON_OBJECT)
                .build();
    }

    RelationsQuery getDefaultRelationQuery() {
        var relationsQuery = new RelationsQuery();
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        var entityTypeFilter = new RelationEntityTypeFilter(EntityRelation.CONTAINS_TYPE, Collections.emptyList());
        relationsQuery.setFilters(Collections.singletonList(entityTypeFilter));
        return relationsQuery;
    }

    EntityRelationsQuery buildQuery(RelationsQuery relationsQuery) {
        var query = new EntityRelationsQuery();
        var parameters = new RelationsSearchParameters(
                ORIGINATOR_ID,
                relationsQuery.getDirection(),
                relationsQuery.getMaxLevel(),
                relationsQuery.isFetchLastLevelOnly()
        );
        query.setParameters(parameters);
        query.setFilters(relationsQuery.getFilters());
        return query;
    }

}
