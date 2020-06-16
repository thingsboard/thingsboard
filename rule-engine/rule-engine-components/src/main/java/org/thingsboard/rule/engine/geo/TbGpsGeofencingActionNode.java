/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by ashvayka on 19.01.18.
 */
@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "gps geofencing events",
        configClazz = TbGpsGeofencingActionNodeConfiguration.class,
        relationTypes = {"Success", "Entered", "Left", "Inside", "Outside"},
        nodeDescription = "Produces incoming messages using GPS based geofencing",
        nodeDetails = "Extracts latitude and longitude parameters from incoming message and returns different events based on configuration parameters",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeGpsGeofencingConfig"
)
public class TbGpsGeofencingActionNode extends AbstractGeofencingNode<TbGpsGeofencingActionNodeConfiguration> {

    private final Map<EntityId, EntityGeofencingState> entityStates = new HashMap<>();
    private final Gson gson = new Gson();
    private final JsonParser parser = new JsonParser();

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        boolean matches = checkMatches(msg);
        long ts = System.currentTimeMillis();

        EntityGeofencingState entityState = entityStates.computeIfAbsent(msg.getOriginator(), key -> {
            try {
                Optional<AttributeKvEntry> entry = ctx.getAttributesService()
                        .find(ctx.getTenantId(), msg.getOriginator(), DataConstants.SERVER_SCOPE, ctx.getServiceId())
                        .get(1, TimeUnit.MINUTES);
                if (entry.isPresent()) {
                    JsonObject element = parser.parse(entry.get().getValueAsString()).getAsJsonObject();
                    return new EntityGeofencingState(element.get("inside").getAsBoolean(), element.get("stateSwitchTime").getAsLong(), element.get("stayed").getAsBoolean());
                } else {
                    return new EntityGeofencingState(false, 0L, false);
                }
            } catch (InterruptedException | TimeoutException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        boolean told = false;
        if (entityState.getStateSwitchTime() == 0L || entityState.isInside() != matches) {
            switchState(ctx, msg.getOriginator(), entityState, matches, ts);
            ctx.tellNext(msg, matches ? "Entered" : "Left");
            told = true;
        } else {
            if (!entityState.isStayed()) {
                long stayTime = ts - entityState.getStateSwitchTime();
                if (stayTime > (entityState.isInside() ?
                        TimeUnit.valueOf(config.getMinInsideDurationTimeUnit()).toMillis(config.getMinInsideDuration()) : TimeUnit.valueOf(config.getMinOutsideDurationTimeUnit()).toMillis(config.getMinOutsideDuration()))) {
                    setStaid(ctx, msg.getOriginator(), entityState);
                    ctx.tellNext(msg, entityState.isInside() ? "Inside" : "Outside");
                    told = true;
                }
            }
        }
        if (!told) {
            ctx.tellSuccess(msg);
        }
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
        ctx.getAttributesService().save(ctx.getTenantId(), entityId, DataConstants.SERVER_SCOPE, attributeKvEntryList);
    }

    @Override
    protected Class<TbGpsGeofencingActionNodeConfiguration> getConfigClazz() {
        return TbGpsGeofencingActionNodeConfiguration.class;
    }
}
