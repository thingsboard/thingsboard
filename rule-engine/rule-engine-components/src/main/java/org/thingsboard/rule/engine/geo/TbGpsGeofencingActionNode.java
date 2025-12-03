/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.thingsboard.rule.engine.util.GpsGeofencingEvents.ENTERED;
import static org.thingsboard.rule.engine.util.GpsGeofencingEvents.INSIDE;
import static org.thingsboard.rule.engine.util.GpsGeofencingEvents.LEFT;
import static org.thingsboard.rule.engine.util.GpsGeofencingEvents.OUTSIDE;

@RuleNode(
        type = ComponentType.ACTION,
        name = "gps geofencing events",
        version = 1,
        configClazz = TbGpsGeofencingActionNodeConfiguration.class,
        relationTypes = {"Success", "Entered", "Left", "Inside", "Outside"},
        nodeDescription = "Produces incoming messages using GPS based geofencing",
        nodeDetails = "Extracts latitude and longitude parameters from incoming message and returns different events based on configuration parameters. " +
                "<br><br>" +
                "If an object with coordinates extracted from incoming message enters the geofence, sends a message with the type <code>Entered</code>. " +
                "If an object leaves the geofence, sends a message with the type <code>Left</code>. " +
                "If the presence monitoring strategy <b>\"On first message\"</b> is selected, sends messages via rule node connection type <code>Inside</code> or <code>Outside</code> only the first time the geofencing and duration conditions are satisfied; otherwise sends messages via rule node connection type <code>Success</code>. " +
                "If the presence monitoring strategy <b>\"On each message\"</b> is selected, sends messages via rule node connection type <code>Inside</code> or <code>Outside</code> every time the geofencing condition is satisfied. " +
                "<br><br>" +
                "Output connections: <code>Entered</code>, <code>Left</code>, <code>Inside</code>, <code>Outside</code>, <code>Success</code>",
        configDirective = "tbActionNodeGpsGeofencingConfig",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/gps-geofencing-events/"
)
public class TbGpsGeofencingActionNode extends AbstractGeofencingNode<TbGpsGeofencingActionNodeConfiguration> {

    private static final String REPORT_PRESENCE_STATUS_ON_EACH_MESSAGE = "reportPresenceStatusOnEachMessage";
    private final ConcurrentMap<EntityId, EntityGeofencingState> entityStates = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        boolean matches = checkMatches(msg);
        long ts = System.currentTimeMillis();

        EntityGeofencingState entityState = entityStates.computeIfAbsent(msg.getOriginator(), key -> {
            try {
                Optional<AttributeKvEntry> entry = ctx.getAttributesService()
                        .find(ctx.getTenantId(), msg.getOriginator(), AttributeScope.SERVER_SCOPE, ctx.getServiceId())
                        .get(1, TimeUnit.MINUTES);
                if (entry.isPresent()) {
                    JsonObject element = JsonParser.parseString(entry.get().getValueAsString()).getAsJsonObject();
                    return new EntityGeofencingState(element.get("inside").getAsBoolean(), element.get("stateSwitchTime").getAsLong(), element.get("stayed").getAsBoolean());
                } else {
                    return new EntityGeofencingState(false, 0L, false);
                }
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        if (entityState.getStateSwitchTime() == 0L || entityState.isInside() != matches) {
            switchState(ctx, msg.getOriginator(), entityState, matches, ts);
            ctx.tellNext(msg, matches ? ENTERED : LEFT);
            return;
        }

        if (config.isReportPresenceStatusOnEachMessage()) {
            ctx.tellNext(msg, entityState.isInside() ? INSIDE : OUTSIDE);
            return;
        }

        if (entityState.isStayed()) {
            ctx.tellSuccess(msg);
            return;
        }

        long stayTime = ts - entityState.getStateSwitchTime();
        if (stayTime > (entityState.isInside() ?
                TimeUnit.valueOf(config.getMinInsideDurationTimeUnit()).toMillis(config.getMinInsideDuration()) :
                TimeUnit.valueOf(config.getMinOutsideDurationTimeUnit()).toMillis(config.getMinOutsideDuration()))) {
            setStaid(ctx, msg.getOriginator(), entityState);
            ctx.tellNext(msg, entityState.isInside() ? INSIDE : OUTSIDE);
            return;
        }

        ctx.tellSuccess(msg);
    }

    private void switchState(TbContext ctx, EntityId entityId, EntityGeofencingState entityState, boolean matches, long ts) {
        entityState.setInside(matches);
        entityState.setStateSwitchTime(ts);
        entityState.setStayed(false);
        persist(ctx, entityId, entityState);
    }

    private void setStaid(TbContext ctx, EntityId entityId, EntityGeofencingState entityState) {
        entityState.setStayed(true);
        persist(ctx, entityId, entityState);
    }

    private void persist(TbContext ctx, EntityId entityId, EntityGeofencingState entityState) {
        JsonObject object = new JsonObject();
        object.addProperty("inside", entityState.isInside());
        object.addProperty("stateSwitchTime", entityState.getStateSwitchTime());
        object.addProperty("stayed", entityState.isStayed());
        AttributeKvEntry entry = new BaseAttributeKvEntry(new StringDataEntry(ctx.getServiceId(), gson.toJson(object)), System.currentTimeMillis());
        List<AttributeKvEntry> attributeKvEntryList = Collections.singletonList(entry);
        ctx.getAttributesService().save(ctx.getTenantId(), entityId, AttributeScope.SERVER_SCOPE, attributeKvEntryList);
    }

    @Override
    protected Class<TbGpsGeofencingActionNodeConfiguration> getConfigClazz() {
        return TbGpsGeofencingActionNodeConfiguration.class;
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        if (fromVersion == 0) {
            if (!oldConfiguration.has(REPORT_PRESENCE_STATUS_ON_EACH_MESSAGE)) {
                hasChanges = true;
                ((ObjectNode) oldConfiguration).put(REPORT_PRESENCE_STATUS_ON_EACH_MESSAGE, false);
            }
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
