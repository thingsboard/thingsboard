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
package org.thingsboard.rule.engine.util;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.data.RelationsQuery;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationsQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationEntityTypeFilter;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.relation.RelationsSearchParameters;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.common.util.DonAsynchron.withCallback;

public class EntitiesRelatedEntityIdAsyncLoaderTest {

    private static final EntityId ASSET_ORIGINATOR_ID = new AssetId(UUID.randomUUID());
    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final ListeningExecutor DB_EXECUTOR = new TestDbCallbackExecutor();

    private TbContext ctxMock;
    private RelationService relationServiceMock;

    private RelationsQuery relationsQuery;

    @BeforeEach
    void setUp() {
        ctxMock = mock(TbContext.class);
        relationServiceMock = mock(RelationService.class);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        relationsQuery = new RelationsQuery();
        relationsQuery.setDirection(EntitySearchDirection.FROM);
        relationsQuery.setMaxLevel(1);
        RelationEntityTypeFilter entityTypeFilter = new RelationEntityTypeFilter(
                EntityRelation.CONTAINS_TYPE, Collections.emptyList()
        );
        relationsQuery.setFilters(Collections.singletonList(entityTypeFilter));
    }

    @Test
    public void givenRelationsQuery_whenFindEntityAsync_ShouldBuildCorrectEntityRelationsQuery() {
        // GIVEN
        var expectedEntityRelationsQuery = new EntityRelationsQuery();
        var parameters = new RelationsSearchParameters(
                ASSET_ORIGINATOR_ID,
                relationsQuery.getDirection(),
                relationsQuery.getMaxLevel(),
                relationsQuery.isFetchLastLevelOnly()
        );
        expectedEntityRelationsQuery.setParameters(parameters);
        expectedEntityRelationsQuery.setFilters(relationsQuery.getFilters());

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        when(relationServiceMock.findByQuery(eq(TENANT_ID), eq(expectedEntityRelationsQuery)))
                .thenReturn(Futures.immediateFuture(null));
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctxMock, ASSET_ORIGINATOR_ID, relationsQuery);

        // THEN
        verify(relationServiceMock, times(1)).findByQuery(eq(TENANT_ID), eq(expectedEntityRelationsQuery));
    }


    @Test
    public void givenSeveralEntitiesFound_whenFindEntityAsync_ShouldKeepOneAndDiscardOthers() throws Exception {
        // GIVEN
        var expectedEntityRelationsQuery = new EntityRelationsQuery();
        var parameters = new RelationsSearchParameters(
                ASSET_ORIGINATOR_ID,
                relationsQuery.getDirection(),
                relationsQuery.getMaxLevel(),
                relationsQuery.isFetchLastLevelOnly()
        );
        expectedEntityRelationsQuery.setParameters(parameters);
        expectedEntityRelationsQuery.setFilters(relationsQuery.getFilters());

        var device1 = new Device(new DeviceId(UUID.randomUUID()));
        device1.setName("Device 1");
        var device2 = new Device(new DeviceId(UUID.randomUUID()));
        device1.setName("Device 2");
        var device3 = new Device(new DeviceId(UUID.randomUUID()));
        device3.setName("Device 3");

        var entityRelationDevice1 = new EntityRelation();
        entityRelationDevice1.setFrom(ASSET_ORIGINATOR_ID);
        entityRelationDevice1.setTo(device1.getId());
        entityRelationDevice1.setType(EntityRelation.CONTAINS_TYPE);

        var entityRelationDevice2 = new EntityRelation();
        entityRelationDevice2.setFrom(ASSET_ORIGINATOR_ID);
        entityRelationDevice2.setTo(device2.getId());
        entityRelationDevice2.setType(EntityRelation.CONTAINS_TYPE);

        var entityRelationDevice3 = new EntityRelation();
        entityRelationDevice3.setFrom(ASSET_ORIGINATOR_ID);
        entityRelationDevice3.setTo(device3.getId());
        entityRelationDevice3.setType(EntityRelation.CONTAINS_TYPE);

        var expectedEntityRelationsList = List.of(entityRelationDevice1, entityRelationDevice2, entityRelationDevice3);

        when(ctxMock.getTenantId()).thenReturn(TENANT_ID);
        when(ctxMock.getRelationService()).thenReturn(relationServiceMock);
        when(relationServiceMock.findByQuery(eq(TENANT_ID), eq(expectedEntityRelationsQuery)))
                .thenReturn(Futures.immediateFuture(expectedEntityRelationsList));
        when(ctxMock.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        // WHEN
        var deviceIdFuture = EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctxMock, ASSET_ORIGINATOR_ID, relationsQuery);

        // THEN
        assertNotNull(deviceIdFuture);

        var actualDeviceId = deviceIdFuture.get();
        assertNotNull(actualDeviceId);
        assertEquals(device1.getId(), actualDeviceId);
    }


    @Test
    public void givenRelationQuery_whenFindEntityAsync_thenOK() {
        // GIVEN
        List<EntityRelation> entityRelations = new ArrayList<>();
        entityRelations.add(createEntityRelation(TENANT_ID, ASSET_ORIGINATOR_ID));
        when(relationServiceMock.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        // WHEN
        ListenableFuture<EntityId> entityIdFuture = EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctxMock, TENANT_ID, relationsQuery);

        // THEN
        verifyEntityIdFuture(entityIdFuture, ASSET_ORIGINATOR_ID);
    }

    @Test
    public void givenRelationQuery_whenFindEntityAsync_thenReturnNull() {
        // GIVEN
        List<EntityRelation> entityRelations = new ArrayList<>();
        when(relationServiceMock.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        // WHEN
        ListenableFuture<EntityId> entityIdFuture = EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctxMock, TENANT_ID, relationsQuery);

        // THEN
        verifyEntityIdFuture(entityIdFuture, null);
    }

    @Test
    public void givenRelationQuery_whenFindEntityAsync_thenFailure() {
        // GIVEN
        relationsQuery.setDirection(null);
        List<EntityRelation> entityRelations = new ArrayList<>();
        entityRelations.add(createEntityRelation(TENANT_ID, ASSET_ORIGINATOR_ID));

        when(relationServiceMock.findByQuery(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Futures.immediateFuture(entityRelations));

        // WHEN
        ListenableFuture<EntityId> entityIdFuture = EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctxMock, TENANT_ID, relationsQuery);

        // THEN
        verifyEntityIdFuture(entityIdFuture, ASSET_ORIGINATOR_ID);
    }

    private void verifyEntityIdFuture(ListenableFuture<EntityId> entityIdFuture, EntityId assetId) {
        withCallback(entityIdFuture,
                entityId -> assertThat(entityId).isEqualTo(assetId),
                throwable -> assertThat(throwable).isInstanceOf(IllegalStateException.class), ctxMock.getDbCallbackExecutor());
    }

    private static EntityRelation createEntityRelation(EntityId from, EntityId to) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(from);
        relation.setTo(to);
        relation.setType(EntityRelation.CONTAINS_TYPE);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        return relation;
    }
}
