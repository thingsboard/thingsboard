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
package org.thingsboard.server.service.cf;

import com.google.common.util.concurrent.FutureCallback;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.AttributesDeleteRequest;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.TimeseriesDeleteRequest;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.aggregation.RelatedEntitiesAggregationCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.AttributesSaveResult;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationPathQuery;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationPathLevel;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeScopeProto;
import org.thingsboard.server.gen.transport.TransportProtos.AttributeValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.CalculatedFieldTelemetryMsgProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCalculatedFieldMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TsKvProto;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.thingsboard.server.common.util.ProtoUtils.toTsKvProto;
import static org.thingsboard.server.utils.CalculatedFieldUtils.toProto;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultCalculatedFieldQueueService implements CalculatedFieldQueueService {

    public static final TbQueueCallback DUMMY_TB_QUEUE_CALLBACK = new TbQueueCallback() {
        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
        }

        @Override
        public void onFailure(Throwable t) {
        }
    };

    private final CalculatedFieldCache calculatedFieldCache;
    private final TbClusterService clusterService;
    private final RelationService relationService;

    @Override
    public void pushRequestToQueue(TimeseriesSaveRequest request, TimeseriesSaveResult result, FutureCallback<Void> callback) {
        var tenantId = request.getTenantId();
        var entityId = request.getEntityId();
        var entries = request.getEntries();
        checkEntityAndPushToQueue(tenantId, entityId,
                cf -> cf.matches(entries),
                cf -> cf.linkMatches(entityId, entries),
                cf -> cf.dynamicSourceMatches(request.getEntries()),
                cf -> cf.relatedEntityMatches(entries),
                () -> toCalculatedFieldTelemetryMsgProto(request, result), callback);
    }

    @Override
    public void pushRequestToQueue(TimeseriesSaveRequest request, FutureCallback<Void> callback) {
        pushRequestToQueue(request, null, callback);
    }

    @Override
    public void pushRequestToQueue(AttributesSaveRequest request, AttributesSaveResult result, FutureCallback<Void> callback) {
        var tenantId = request.getTenantId();
        var entityId = request.getEntityId();
        var entries = request.getEntries();
        var scope = request.getScope();
        checkEntityAndPushToQueue(tenantId, entityId,
                cf -> cf.matches(entries, scope),
                cf -> cf.linkMatches(entityId, entries, scope),
                cf -> cf.dynamicSourceMatches(request.getEntries(), request.getScope()),
                cf -> cf.relatedEntityMatches(entries, scope),
                () -> toCalculatedFieldTelemetryMsgProto(request, result), callback);
    }

    @Override
    public void pushRequestToQueue(AttributesSaveRequest request, FutureCallback<Void> callback) {
        pushRequestToQueue(request, null, callback);
    }

    @Override
    public void pushRequestToQueue(AttributesDeleteRequest request, List<String> result, FutureCallback<Void> callback) {
        var tenantId = request.getTenantId();
        var entityId = request.getEntityId();
        var scope = request.getScope();
        checkEntityAndPushToQueue(tenantId, entityId,
                cf -> cf.matchesKeys(result, scope),
                cf -> cf.linkMatchesAttrKeys(entityId, result, scope),
                cf -> cf.matchesDynamicSourceKeys(result, request.getScope()),
                cf -> cf.matchesRelatedEntityKeys(result, scope),
                () -> toCalculatedFieldTelemetryMsgProto(request, result), callback);
    }

    @Override
    public void pushRequestToQueue(TimeseriesDeleteRequest request, List<String> result, FutureCallback<Void> callback) {
        var tenantId = request.getTenantId();
        var entityId = request.getEntityId();
        checkEntityAndPushToQueue(tenantId, entityId,
                cf -> cf.matchesKeys(result),
                cf -> cf.linkMatchesTsKeys(entityId, result),
                cf -> cf.matchesDynamicSourceKeys(result),
                cf -> cf.matchesRelatedEntityKeys(result),
                () -> toCalculatedFieldTelemetryMsgProto(request, result), callback);
    }

    private void checkEntityAndPushToQueue(TenantId tenantId, EntityId entityId,
                                           Predicate<CalculatedFieldCtx> mainEntityFilter,
                                           Predicate<CalculatedFieldCtx> linkedEntityFilter,
                                           Predicate<CalculatedFieldCtx> dynamicSourceFilter,
                                           Predicate<CalculatedFieldCtx> relatedEntityFilter,
                                           Supplier<ToCalculatedFieldMsg> msg, FutureCallback<Void> callback) {
        if (EntityType.TENANT.equals(entityId.getEntityType())) {
            tenantId = (TenantId) entityId;
        }
        boolean send = checkEntityForCalculatedFields(tenantId, entityId, mainEntityFilter, linkedEntityFilter, dynamicSourceFilter, relatedEntityFilter);
        if (send) {
            ToCalculatedFieldMsg calculatedFieldMsg = msg.get();
            clusterService.pushMsgToCalculatedFields(tenantId, entityId, calculatedFieldMsg, wrap(callback));
        } else {
            if (callback != null) {
                callback.onSuccess(null);
            }
        }
    }

    private boolean checkEntityForCalculatedFields(TenantId tenantId, EntityId entityId, Predicate<CalculatedFieldCtx> filter, Predicate<CalculatedFieldCtx> linkedEntityFilter, Predicate<CalculatedFieldCtx> dynamicSourceFilter, Predicate<CalculatedFieldCtx> relatedEntityFilter) {
        if (!CalculatedField.isSupportedRefEntity(entityId)) {
            return false;
        }

        if (calculatedFieldCache.hasCalculatedFields(tenantId, entityId, filter)) {
            return true;
        }

        List<CalculatedFieldLink> links = calculatedFieldCache.getCalculatedFieldLinksByEntityId(entityId);
        for (CalculatedFieldLink link : links) {
            CalculatedFieldCtx ctx = calculatedFieldCache.getCalculatedFieldCtx(link.calculatedFieldId());
            if (ctx != null && linkedEntityFilter.test(ctx)) {
                return true;
            }
        }

        for (EntityId dynamicEntity : calculatedFieldCache.getDynamicEntities(tenantId, entityId)) {
            if (calculatedFieldCache.getCalculatedFieldCtxsByEntityId(dynamicEntity).stream().anyMatch(dynamicSourceFilter)) {
                return true;
            }
            EntityId dynamicEntityProfileId = calculatedFieldCache.getProfileId(tenantId, dynamicEntity);
            if (calculatedFieldCache.getCalculatedFieldCtxsByEntityId(dynamicEntityProfileId).stream().anyMatch(dynamicSourceFilter)) {
                return true;
            }
        }

        boolean hasMatchesEntityAggCfs = calculatedFieldCache.getCalculatedFieldCtxsByType(CalculatedFieldType.ENTITY_AGGREGATION).anyMatch(filter);
        if (hasMatchesEntityAggCfs) {
            return true;
        }

        List<CalculatedFieldCtx> relatedEntitiesAggregationCfs = calculatedFieldCache.getCalculatedFieldCtxsByType(CalculatedFieldType.RELATED_ENTITIES_AGGREGATION)
                .filter(relatedEntityFilter)
                .toList();
        for (CalculatedFieldCtx cfCtx : relatedEntitiesAggregationCfs) {
            if (cfCtx.getCalculatedField().getConfiguration() instanceof RelatedEntitiesAggregationCalculatedFieldConfiguration aggConfig) {
                RelationPathLevel relation = aggConfig.getRelation();
                EntitySearchDirection inverseDirection = switch (relation.direction()) {
                    case FROM -> EntitySearchDirection.TO;
                    case TO -> EntitySearchDirection.FROM;
                };
                RelationPathLevel inverseRelation = new RelationPathLevel(inverseDirection, relation.relationType());
                List<EntityRelation> byRelationPathQuery = relationService.findByRelationPathQuery(tenantId, new EntityRelationPathQuery(entityId, List.of(inverseRelation)));
                if (!byRelationPathQuery.isEmpty()) {
                    EntityId cfEntityId = cfCtx.getEntityId();
                    for (EntityRelation entityRelation : byRelationPathQuery) {
                        EntityId relatedId = (inverseDirection == EntitySearchDirection.FROM) ? entityRelation.getTo() : entityRelation.getFrom();
                        if (cfEntityId.equals(relatedId) || cfEntityId.equals(calculatedFieldCache.getProfileId(tenantId, relatedId))) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private ToCalculatedFieldMsg toCalculatedFieldTelemetryMsgProto(TimeseriesSaveRequest request, TimeseriesSaveResult result) {
        CalculatedFieldTelemetryMsgProto.Builder telemetryMsg = buildTelemetryMsgProto(request.getTenantId(), request.getEntityId(), request.getPreviousCalculatedFieldIds(), request.getTbMsgId(), request.getTbMsgType());

        List<TsKvEntry> entries = request.getEntries();
        List<Long> versions = result != null ? result.getVersions() : Collections.emptyList();

        for (int i = 0; i < entries.size(); i++) {
            TsKvProto.Builder tsProtoBuilder = toTsKvProto(entries.get(i)).toBuilder();
            if (versions != null && !versions.isEmpty() && versions.get(i) != null) {
                tsProtoBuilder.setVersion(versions.get(i));
            }
            telemetryMsg.addTsData(tsProtoBuilder.build());
        }

        return ToCalculatedFieldMsg.newBuilder().setTelemetryMsg(telemetryMsg).build();
    }

    private ToCalculatedFieldMsg toCalculatedFieldTelemetryMsgProto(AttributesSaveRequest request, AttributesSaveResult result) {
        ToCalculatedFieldMsg.Builder msg = ToCalculatedFieldMsg.newBuilder();

        CalculatedFieldTelemetryMsgProto.Builder telemetryMsg = buildTelemetryMsgProto(request.getTenantId(), request.getEntityId(), request.getPreviousCalculatedFieldIds(), request.getTbMsgId(), request.getTbMsgType());
        telemetryMsg.setScope(AttributeScopeProto.valueOf(request.getScope().name()));

        List<AttributeKvEntry> entries = request.getEntries();
        List<Long> versions = result.versions();

        for (int i = 0; i < entries.size(); i++) {
            AttributeValueProto.Builder attrProtoBuilder = ProtoUtils.toProto(entries.get(i)).toBuilder();
            if (versions != null && !versions.isEmpty() && versions.get(i) != null) {
                attrProtoBuilder.setVersion(versions.get(i));
            }
            telemetryMsg.addAttrData(attrProtoBuilder.build());
        }
        msg.setTelemetryMsg(telemetryMsg.build());

        return msg.build();
    }

    private ToCalculatedFieldMsg toCalculatedFieldTelemetryMsgProto(AttributesDeleteRequest request, List<String> removedKeys) {
        CalculatedFieldTelemetryMsgProto telemetryMsg = buildTelemetryMsgProto(request.getTenantId(), request.getEntityId(), request.getPreviousCalculatedFieldIds(), request.getTbMsgId(), request.getTbMsgType())
                .setScope(AttributeScopeProto.valueOf(request.getScope().name()))
                .addAllRemovedAttrKeys(removedKeys).build();
        return ToCalculatedFieldMsg.newBuilder()
                .setTelemetryMsg(telemetryMsg)
                .build();
    }

    private ToCalculatedFieldMsg toCalculatedFieldTelemetryMsgProto(TimeseriesDeleteRequest request, List<String> removedKeys) {
        CalculatedFieldTelemetryMsgProto telemetryMsg = buildTelemetryMsgProto(request.getTenantId(), request.getEntityId(), request.getPreviousCalculatedFieldIds(), request.getTbMsgId(), request.getTbMsgType())
                .addAllRemovedTsKeys(removedKeys).build();
        return ToCalculatedFieldMsg.newBuilder()
                .setTelemetryMsg(telemetryMsg)
                .build();
    }

    private CalculatedFieldTelemetryMsgProto.Builder buildTelemetryMsgProto(TenantId tenantId, EntityId entityId, List<CalculatedFieldId> calculatedFieldIds, UUID tbMsgId, TbMsgType tbMsgType) {
        CalculatedFieldTelemetryMsgProto.Builder telemetryMsg = CalculatedFieldTelemetryMsgProto.newBuilder();

        if (EntityType.TENANT.equals(entityId.getEntityType())) {
            tenantId = (TenantId) entityId;
        }

        telemetryMsg.setTenantIdMSB(tenantId.getId().getMostSignificantBits());
        telemetryMsg.setTenantIdLSB(tenantId.getId().getLeastSignificantBits());

        telemetryMsg.setEntityType(entityId.getEntityType().name());
        telemetryMsg.setEntityIdMSB(entityId.getId().getMostSignificantBits());
        telemetryMsg.setEntityIdLSB(entityId.getId().getLeastSignificantBits());

        if (calculatedFieldIds != null) {
            for (CalculatedFieldId cfId : calculatedFieldIds) {
                telemetryMsg.addPreviousCalculatedFields(toProto(cfId));
            }
        }

        if (tbMsgId != null) {
            telemetryMsg.setTbMsgIdMSB(tbMsgId.getMostSignificantBits());
            telemetryMsg.setTbMsgIdLSB(tbMsgId.getLeastSignificantBits());
        }

        if (tbMsgType != null) {
            telemetryMsg.setTbMsgType(tbMsgType.name());
        }

        return telemetryMsg;
    }

    private static TbQueueCallback wrap(FutureCallback<Void> callback) {
        if (callback != null) {
            return new FutureCallbackWrapper(callback);
        } else {
            return DUMMY_TB_QUEUE_CALLBACK;
        }
    }

    private static class FutureCallbackWrapper implements TbQueueCallback {
        private final FutureCallback<Void> callback;

        public FutureCallbackWrapper(FutureCallback<Void> callback) {
            this.callback = callback;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            callback.onSuccess(null);
        }

        @Override
        public void onFailure(Throwable t) {
            callback.onFailure(t);
        }

    }

}
