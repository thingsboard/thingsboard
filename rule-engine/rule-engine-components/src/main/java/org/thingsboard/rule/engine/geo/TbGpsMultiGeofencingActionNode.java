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
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonObject;
import com.nimbusds.oauth2.sdk.util.CollectionUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

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
        ListenableFuture<List<EntityId>> matchedZonesFuture = findMatchedZones(ctx, msg);

        ListenableFuture<GeofenceResponse> geofenceResponseFuture = Futures.transformAsync(matchedZonesFuture, matchedZones -> geofencingProcessor.process(msg.getMetaDataTs(), ctx.getSelfId(), msg.getOriginator(), matchedZones), ctx.getDbCallbackExecutor());

        withCallback(geofenceResponseFuture, geofenceResponse -> {
            processGeofenceResponse(ctx, msg, msg.getOriginator(), geofenceResponse);
            ctx.tellSuccess(msg);
        }, t -> {
            log.error("Failed to process geofencing", t);
            ctx.tellFailure(msg, t);
        }, MoreExecutors.directExecutor());
    }

    private void processGeofenceResponse(TbContext ctx, TbMsg originalMsg, EntityId originatorId, GeofenceResponse geofenceResponse) {
        TbMsgMetaData metaData = originalMsg.getMetaData().copy();
        metaData.putValue("originatorId", originatorId.toString());

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
            ctx.enqueueForTellNext(tbMsg, relationType);
        }
    }

    private ListenableFuture<List<EntityId>> findMatchedZones(TbContext tbContext, TbMsg tbMsg) throws TbNodeException {
        JsonObject msgDataObj = getJsonObject(tbMsg);
        double latitude = getValueFromMessageByName(tbMsg, msgDataObj, config.getLatitudeKeyName());
        double longitude = getValueFromMessageByName(tbMsg, msgDataObj, config.getLongitudeKeyName());

        if (config.getRelationsQuery() == null) {
            return Futures.immediateFuture(Collections.emptyList());
        }

        ListenableFuture<List<EntityId>> entityIdsFuture = EntitiesRelatedEntityIdAsyncLoader.findEntitiesAsync(tbContext, tbMsg.getOriginator(), config.getRelationsQuery());

        ListenableFuture<List<Pair<EntityId, Optional<AttributeKvEntry>>>> entityIdToAttributesFuture = Futures.transformAsync(entityIdsFuture, relatedZones -> {
            List<ListenableFuture<Pair<EntityId, Optional<AttributeKvEntry>>>> attributeFutures = relatedZones.stream()
                    .map(relatedGeofenceId -> Futures.transform(
                            tbContext.getAttributesService()
                                    .find(tbContext.getTenantId(), relatedGeofenceId, AttributeScope.SERVER_SCOPE, config.getPerimeterAttributeKey()),
                            attribute -> Pair.of(relatedGeofenceId, attribute),
                            MoreExecutors.directExecutor()
                    ))
                    .collect(Collectors.toList());

            return Futures.allAsList(attributeFutures);
        }, MoreExecutors.directExecutor());

        return Futures.transformAsync(entityIdToAttributesFuture, entityIdAttributePairs -> {
            List<EntityId> matchedGeofenceIds = new ArrayList<>();

            entityIdAttributePairs.forEach(pair -> {
                EntityId entityId = pair.getLeft();
                Optional<AttributeKvEntry> attributeKvEntry = pair.getRight();

                attributeKvEntry.map(this::mapToPerimeter).ifPresent(perimeter -> {
                    try {
                        if (checkMatches(perimeter, latitude, longitude)) {
                            matchedGeofenceIds.add(entityId);
                        }
                    } catch (TbNodeException e) {
                        throw new RuntimeException(e);
                    }
                });
            });

            return Futures.immediateFuture(matchedGeofenceIds);
        }, MoreExecutors.directExecutor());
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
