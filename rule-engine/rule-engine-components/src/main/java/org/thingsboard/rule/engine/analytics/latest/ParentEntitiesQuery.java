// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.analytics.latest;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = ParentEntitiesRelationsQuery.class, name = "relationsQuery"),
        @JsonSubTypes.Type(value = ParentEntitiesGroup.class, name = "group"),
        @JsonSubTypes.Type(value = ParentEntitiesSingleEntity.class, name = "single")})
public interface ParentEntitiesQuery {

    ListenableFuture<List<EntityId>> getParentEntitiesAsync(TbContext ctx);

    ListenableFuture<List<EntityId>> getLocalParentEntitiesAsync(TbContext ctx);

    ListenableFuture<List<EntityId>> getChildEntitiesAsync(TbContext ctx, EntityId parentEntityId);

    boolean useParentEntitiesOnlyForSimpleAggregation();

}
