/**
 * Copyright © 2016-2024 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.geo;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.transform.MultipleTbMsgsCallbackWrapper;
import org.thingsboard.rule.engine.transform.TbMsgCallbackWrapper;
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.springframework.util.CollectionUtils.isEmpty;
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
        if (config.getRelationsQuery() == null) {
            throw new TbNodeException("Relations query should be specified", true);
        }
        geofencingProcessor = new GeofencingProcessor(ctx, this.config);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        ListenableFuture<List<EntityId>> matchedZonesFuture = findMatchedZones(ctx, msg);

        ListenableFuture<Set<GeofenceState>> changedGeofenceStatesFuture = Futures.transformAsync(matchedZonesFuture, matchedZones -> geofencingProcessor.process(ctx.getSelfId(), msg, matchedZones),
                MoreExecutors.directExecutor());

        withCallback(changedGeofenceStatesFuture, geofenceResponse -> processGeofenceResponse(ctx, msg, geofenceResponse), t -> ctx.tellFailure(msg, t), MoreExecutors.directExecutor());
    }

    private void processGeofenceResponse(TbContext ctx, TbMsg originalMsg, Set<GeofenceState> changedGeofenceState) {
        if (isEmpty(changedGeofenceState)) {
            ctx.tellSuccess(originalMsg);
            return;
        }
        TbMsgMetaData metaData = originalMsg.getMetaData().copy();
        metaData.putValue("originatorId", originalMsg.getOriginator().toString());
        TbMsgCallbackWrapper wrapper = createCallbackWrapper(ctx, originalMsg, changedGeofenceState.size());
        changedGeofenceState.forEach(geofenceState -> {
            TbMsg tbMsg = TbMsg.newMsg(originalMsg.getInternalType(), geofenceState.getGeofenceId(), metaData, originalMsg.getData());
            ctx.enqueueForTellNext(tbMsg, geofenceState.getGeofenceStateStatus().getRuleNodeConnection(), wrapper::onSuccess, wrapper::onFailure);
        });
    }

    private TbMsgCallbackWrapper createCallbackWrapper(TbContext ctx, TbMsg originalMsg, int messageCount) {
        return new MultipleTbMsgsCallbackWrapper(messageCount, new TbMsgCallback() {
            @Override
            public void onSuccess() {
                ctx.tellSuccess(originalMsg);
            }

            @Override
            public void onFailure(RuleEngineException e) {
                ctx.tellFailure(originalMsg, e);
            }
        });
    }

    private ListenableFuture<List<EntityId>> findMatchedZones(TbContext tbContext, TbMsg tbMsg) throws TbNodeException {
        JsonObject msgDataObj = getMsgDataAsJsonObject(tbMsg);
        double latitude = getValueFromMessageByName(tbMsg, msgDataObj, config.getLatitudeKeyName());
        double longitude = getValueFromMessageByName(tbMsg, msgDataObj, config.getLongitudeKeyName());

        ListenableFuture<List<EntityId>> entityIdsFuture = EntitiesRelatedEntityIdAsyncLoader.findEntitiesAsync(tbContext, tbMsg.getOriginator(), config.getRelationsQuery());

        ListenableFuture<List<TbPair<EntityId, Optional<AttributeKvEntry>>>> entityIdToAttributesFuture = Futures.transformAsync(entityIdsFuture, relatedZones -> {
            List<ListenableFuture<TbPair<EntityId, Optional<AttributeKvEntry>>>> attributeFutures = relatedZones.stream()
                    .map(relatedGeofenceId -> Futures.transform(
                            tbContext.getAttributesService()
                                    .find(tbContext.getTenantId(), relatedGeofenceId, AttributeScope.SERVER_SCOPE, config.getPerimeterKeyName()),
                            attribute -> TbPair.of(relatedGeofenceId, attribute),
                            MoreExecutors.directExecutor()
                    ))
                    .collect(Collectors.toList());

            return Futures.allAsList(attributeFutures);
        }, MoreExecutors.directExecutor());

        return Futures.transformAsync(entityIdToAttributesFuture, entityIdAttributePairs -> {
            List<EntityId> matchedGeofenceIds = new ArrayList<>();

            entityIdAttributePairs.forEach(pair -> {
                EntityId entityId = pair.getFirst();
                Optional<AttributeKvEntry> attributeKvEntry = pair.getSecond();

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
        return jsonValue.map(s -> JacksonUtil.fromString(s, Perimeter.class)).orElse(null);
    }

    @Override
    protected Class<TbGpsMultiGeofencingActionNodeConfiguration> getConfigClazz() {
        return TbGpsMultiGeofencingActionNodeConfiguration.class;
    }
}
