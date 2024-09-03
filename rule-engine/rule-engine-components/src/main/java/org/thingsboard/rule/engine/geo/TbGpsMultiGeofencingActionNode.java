/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.geo;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonObject;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.relation.*;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@RuleNode(type = ComponentType.ACTION,
        name = "gps multi geofencing events",
        configClazz = TbGpsMultiGeofencingActionNodeConfiguration.class,
        relationTypes = {"Success", "Entered", "Left", "Inside", "Outside"},
        nodeDescription = "Produces incoming messages using GPS based geofencing",
        nodeDetails = "Extracts latitude and longitude parameters from incoming message and returns different events " +
                "based on configuration parameters")
public class TbGpsMultiGeofencingActionNode extends AbstractGeofencingNode<TbGpsMultiGeofencingActionNodeConfiguration> {

    private GeofencingProcessor geofencingProcessor;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        super.init(ctx, configuration);
        geofencingProcessor = new GeofencingProcessor(ctx, this.config);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        List<EntityId> matchedGeofenceIds = findMatchedZones(ctx, msg);
        GeofenceResponse geofenceResponse = geofencingProcessor.process(msg.getMetaDataTs(), msg.getOriginator(), matchedGeofenceIds);
        processGeofenceResponse(ctx, msg, msg.getOriginator(), geofenceResponse);
        ctx.tellSuccess(msg);
    }

    private void processGeofenceResponse(TbContext ctx, TbMsg originalMsg, EntityId originatorId, GeofenceResponse geofenceResponse) {
        TbMsgMetaData metaData = originalMsg.getMetaData().copy();
        metaData.putValue("originatorId", originatorId.toString());
        metaData.putValue("originatorName", getOriginatorName(ctx, originatorId));

        processGeofenceEvents(ctx, originalMsg, metaData, geofenceResponse.getEnteredGeofences(), "Entered");
        processGeofenceEvents(ctx, originalMsg, metaData, geofenceResponse.getLeftGeofences(), "Left");
        processGeofenceEvents(ctx, originalMsg, metaData, geofenceResponse.getInsideGeofences(), "Inside");
        processGeofenceEvents(ctx, originalMsg, metaData, geofenceResponse.getOutsideGeofences(), "Outside");
    }

    private void processGeofenceEvents(TbContext ctx, TbMsg originalMsg, TbMsgMetaData metaData, List<EntityId> geofences, String relationType) {
        if (CollectionUtils.isEmpty(geofences)) {
            return;
        }
        for (EntityId geofence : geofences) {
            TbMsg tbMsg = TbMsg.newMsg(TbMsgType.valueOf(originalMsg.getType()), geofence, metaData, originalMsg.getData());
            ctx.tellNext(tbMsg, relationType);
        }
    }

    private String getOriginatorName(TbContext tbContext, EntityId originatorId) {
        if (EntityType.ASSET.equals(originatorId.getEntityType())) {
            return tbContext.getAssetService().findAssetById(tbContext.getTenantId(), (AssetId) originatorId).getName();
        } else if (EntityType.DEVICE.equals(originatorId.getEntityType())) {
            return tbContext.getDeviceService().findDeviceById(tbContext.getTenantId(), (DeviceId) originatorId).getName();
        }
        return null;
    }

    protected List<EntityId> findMatchedZones(TbContext tbContext, TbMsg tbMsg) throws TbNodeException, ExecutionException, InterruptedException {
        JsonObject msgDataObj = getJsonObject(tbMsg);
        double latitude = getValueFromMessageByName(tbMsg, msgDataObj, config.getLatitudeKeyName());
        double longitude = getValueFromMessageByName(tbMsg, msgDataObj, config.getLongitudeKeyName());

        if (config.getEntityRelationsQuery() == null) {
            return Collections.emptyList();
        }

        EntityRelationsQuery entityRelationsQuery = config.getEntityRelationsQuery();

        if (config.getEntityRelationsQuery().getParameters().getRootId() == null || config.getEntityRelationsQuery().getParameters().getRootType() == null) {
            entityRelationsQuery = copyEntityRelationsQuery(tbMsg.getOriginator(), config.getEntityRelationsQuery());
        }

        List<EntityId> relatedZones = getInFuture(tbContext.getRelationService().findByQuery(tbContext.getTenantId(), entityRelationsQuery)).stream().map(this::getEntityIdFromEntityRelation).toList();

        Map<EntityId, Perimeter> entityIdPerimeterMap = new HashMap<>();

        List<EntityId> matchedGeofenceIds = new ArrayList<>();

        for (EntityId relatedGeofenceId : relatedZones) {
            Optional<AttributeKvEntry> attributeKvEntry = tbContext.getAttributesService().find(tbContext.getTenantId(), relatedGeofenceId, AttributeScope.SERVER_SCOPE, config.getPerimeterAttributeKey()).get();
            attributeKvEntry.map(this::mapToPerimeter).ifPresent(p -> entityIdPerimeterMap.put(relatedGeofenceId, p));
        }

        for (EntityId entityId : entityIdPerimeterMap.keySet()) {
            Perimeter perimeter = entityIdPerimeterMap.get(entityId);
            boolean matches = checkMatches(perimeter, latitude, longitude);
            if (matches) {
                matchedGeofenceIds.add(entityId);
            }
        }

        return matchedGeofenceIds;
    }

    private EntityRelationsQuery copyEntityRelationsQuery(EntityId originatorId, EntityRelationsQuery entityRelationsQuery) {
        EntityRelationsQuery copy = new EntityRelationsQuery();
        copy.setParameters(entityRelationsQuery.getParameters());
        copy.getParameters().setRootId(originatorId.getId());
        copy.getParameters().setRootType(originatorId.getEntityType());
        copy.setFilters(entityRelationsQuery.getFilters());
        return copy;
    }

    private EntityId getEntityIdFromEntityRelation(EntityRelation entityRelation) {
        return EntitySearchDirection.TO.equals(config.getRelationField()) ? entityRelation.getTo() : entityRelation.getFrom();
    }

    private <T> T getInFuture(ListenableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private Perimeter mapToPerimeter(AttributeKvEntry attributeKvEntry) {
        Optional<String> jsonValue = attributeKvEntry.getJsonValue();
        if (jsonValue.isEmpty()) {
            return null;
        }
        JsonNode jsonNode = JacksonUtil.toJsonNode(jsonValue.get());
        return JacksonUtil.treeToValue(jsonNode, Perimeter.class);
    }

    @Override
    protected Class<TbGpsMultiGeofencingActionNodeConfiguration> getConfigClazz() {
        return TbGpsMultiGeofencingActionNodeConfiguration.class;
    }
}
