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
package org.thingsboard.rule.engine.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.adaptor.JsonConverter;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.NOTIFY_DEVICE_METADATA_KEY;
import static org.thingsboard.server.common.data.DataConstants.SCOPE;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_ATTRIBUTES_REQUEST;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "save attributes",
        configClazz = TbMsgAttributesNodeConfiguration.class,
        version = 2,
        nodeDescription = "Saves attributes data",
        nodeDetails = "Saves entity attributes based on configurable scope parameter. Expects messages with 'POST_ATTRIBUTES_REQUEST' message type. " +
                "If upsert(update/insert) operation is completed successfully rule node will send the incoming message via <b>Success</b> chain, otherwise, <b>Failure</b> chain is used. " +
                "Additionally if checkbox <b>Send attributes updated notification</b> is set to true, rule node will put the \"Attributes Updated\" " +
                "event for <b>SHARED_SCOPE</b> and <b>SERVER_SCOPE</b> attributes updates to the corresponding rule engine queue." +
                "Performance checkbox 'Save attributes only if the value changes' will skip attributes overwrites for values with no changes (avoid concurrent writes because this check is not transactional; will not update 'Last updated time' for skipped attributes).",
        configDirective = "tbActionNodeAttributesConfig",
        icon = "file_upload"
)
public class TbMsgAttributesNode implements TbNode {

    static final String NOTIFY_DEVICE_KEY = "notifyDevice";
    static final String SEND_ATTRIBUTES_UPDATED_NOTIFICATION_KEY = "sendAttributesUpdatedNotification";
    static final String UPDATE_ATTRIBUTES_ONLY_ON_VALUE_CHANGE_KEY = "updateAttributesOnlyOnValueChange";

    private TbMsgAttributesNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgAttributesNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (!msg.isTypeOf(POST_ATTRIBUTES_REQUEST)) {
            ctx.tellFailure(msg, new IllegalArgumentException("Unsupported msg type: " + msg.getType()));
            return;
        }
        String src = msg.getData();
        List<AttributeKvEntry> newAttributes = new ArrayList<>(JsonConverter.convertToAttributes(JsonParser.parseString(src)));
        if (newAttributes.isEmpty()) {
            ctx.tellSuccess(msg);
            return;
        }
        AttributeScope scope = getScope(msg.getMetaData().getValue(SCOPE));
        boolean sendAttributesUpdateNotification = checkSendNotification(scope);

        if (!config.isUpdateAttributesOnlyOnValueChange()) {
            saveAttr(newAttributes, ctx, msg, scope, sendAttributesUpdateNotification);
            return;
        }

        List<String> keys = newAttributes.stream().map(KvEntry::getKey).collect(Collectors.toList());
        ListenableFuture<List<AttributeKvEntry>> findFuture = ctx.getAttributesService().find(ctx.getTenantId(), msg.getOriginator(), scope, keys);

        DonAsynchron.withCallback(findFuture,
                currentAttributes -> {
                    List<AttributeKvEntry> attributesChanged = filterChangedAttr(currentAttributes, newAttributes);
                    saveAttr(attributesChanged, ctx, msg, scope, sendAttributesUpdateNotification);
                },
                throwable -> ctx.tellFailure(msg, throwable),
                MoreExecutors.directExecutor());
    }

    void saveAttr(List<AttributeKvEntry> attributes, TbContext ctx, TbMsg msg, AttributeScope scope, boolean sendAttributesUpdateNotification) {
        if (attributes.isEmpty()) {
            ctx.tellSuccess(msg);
            return;
        }
        FutureCallback<Void> callback = sendAttributesUpdateNotification ?
                new AttributesUpdateNodeCallback(ctx, msg, scope.name(), attributes) :
                new TelemetryNodeCallback(ctx, msg);
        ctx.getTelemetryService().saveAttributes(AttributesSaveRequest.builder()
                .tenantId(ctx.getTenantId())
                .entityId(msg.getOriginator())
                .scope(scope)
                .entries(attributes)
                .notifyDevice(config.isNotifyDevice() || checkNotifyDeviceMdValue(msg.getMetaData().getValue(NOTIFY_DEVICE_METADATA_KEY)))
                .callback(callback)
                .build());
    }

    List<AttributeKvEntry> filterChangedAttr(List<AttributeKvEntry> currentAttributes, List<AttributeKvEntry> newAttributes) {
        if (currentAttributes == null || currentAttributes.isEmpty()) {
            return newAttributes;
        }

        Map<String, AttributeKvEntry> currentAttrMap = currentAttributes.stream()
                .collect(Collectors.toMap(AttributeKvEntry::getKey, Function.identity(), (existing, replacement) -> existing));

        return newAttributes.stream()
                .filter(item -> {
                    AttributeKvEntry cacheAttr = currentAttrMap.get(item.getKey());
                    return cacheAttr == null
                            || !Objects.equals(item.getValue(), cacheAttr.getValue()) //JSON and String can be equals by value, but different by type
                            || !Objects.equals(item.getDataType(), cacheAttr.getDataType());
                })
                .collect(Collectors.toList());
    }

    private boolean checkSendNotification(AttributeScope scope) {
        return config.isSendAttributesUpdatedNotification() && AttributeScope.CLIENT_SCOPE != scope;
    }

    private boolean checkNotifyDeviceMdValue(String notifyDeviceMdValue) {
        // Check for empty string for backward-compatibility. A while ago node always notified devices.
        return StringUtils.isEmpty(notifyDeviceMdValue) || Boolean.parseBoolean(notifyDeviceMdValue);
    }

    private AttributeScope getScope(String mdScopeValue) {
        if (StringUtils.isNotEmpty(mdScopeValue)) {
            return AttributeScope.valueOf(mdScopeValue);
        }
        return AttributeScope.valueOf(config.getScope());
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0:
                if (!oldConfiguration.has(UPDATE_ATTRIBUTES_ONLY_ON_VALUE_CHANGE_KEY)) {
                    hasChanges = true;
                    ((ObjectNode) oldConfiguration).put(UPDATE_ATTRIBUTES_ONLY_ON_VALUE_CHANGE_KEY, false);
                }
            case 1:
                // update notifyDevice. set true if null or property doesn't exist for backward-compatibility.
                hasChanges = fixEscapedBooleanConfigParameter(oldConfiguration, NOTIFY_DEVICE_KEY, hasChanges, true);
                // update sendAttributesUpdatedNotification.
                hasChanges = fixEscapedBooleanConfigParameter(oldConfiguration, SEND_ATTRIBUTES_UPDATED_NOTIFICATION_KEY, hasChanges, false);
                // update updateAttributesOnlyOnValueChange.
                hasChanges = fixEscapedBooleanConfigParameter(oldConfiguration, UPDATE_ATTRIBUTES_ONLY_ON_VALUE_CHANGE_KEY, hasChanges, true);
                break;
            default:
                break;
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

    private boolean fixEscapedBooleanConfigParameter(JsonNode oldConfiguration, String boolKey, boolean hasChanges, boolean valueIfNull) {
        if (oldConfiguration.hasNonNull(boolKey)) {
            var value = oldConfiguration.get(boolKey);
            if (value.isTextual()) {
                hasChanges = true;
                ((ObjectNode) oldConfiguration)
                        .put(boolKey, value.asBoolean(valueIfNull));
            }
        } else {
            hasChanges = true;
            ((ObjectNode) oldConfiguration)
                    .put(boolKey, valueIfNull);
        }
        return hasChanges;
    }

}
