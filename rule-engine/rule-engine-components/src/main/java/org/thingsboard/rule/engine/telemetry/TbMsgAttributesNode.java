/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.CLIENT_SCOPE;
import static org.thingsboard.server.common.data.DataConstants.NOTIFY_DEVICE_METADATA_KEY;
import static org.thingsboard.server.common.data.DataConstants.SCOPE;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_ATTRIBUTES_REQUEST;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "save attributes",
        configClazz = TbMsgAttributesNodeConfiguration.class,
        nodeDescription = "Saves attributes data",
        nodeDetails = "Saves entity attributes based on configurable scope parameter. Expects messages with 'POST_ATTRIBUTES_REQUEST' message type. " +
                      "If upsert(update/insert) operation is completed successfully rule node will send the incoming message via <b>Success</b> chain, otherwise, <b>Failure</b> chain is used. " +
                      "Additionally if checkbox <b>Send attributes updated notification</b> is set to true, rule node will put the \"Attributes Updated\" " +
                      "event for <b>SHARED_SCOPE</b> and <b>SERVER_SCOPE</b> attributes updates to the corresponding rule engine queue.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeAttributesConfig",
        icon = "file_upload"
)
public class TbMsgAttributesNode implements TbNode {

    private TbMsgAttributesNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgAttributesNodeConfiguration.class);
        if (config.getNotifyDevice() == null) {
            config.setNotifyDevice(true);
        }
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
        String scope = getScope(msg.getMetaData().getValue(SCOPE));
        boolean sendAttributesUpdateNotification = checkSendNotification(scope);
        ListenableFuture<List<AttributeKvEntry>> findFuture;
        if (config.isUpdateAttributesOnValueChange()) {
            List<String> keys = newAttributes.stream().map(KvEntry::getKey).collect(Collectors.toList());
            findFuture = ctx.getAttributesService().find(ctx.getTenantId(), msg.getOriginator(), scope, keys);
        } else {
            findFuture = Futures.immediateFuture(null);
        }
        Futures.addCallback(findFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(List<AttributeKvEntry> currentAttributes) {
                List<AttributeKvEntry> attributes = newAttributes;
                if (config.isUpdateAttributesOnValueChange()
                        && currentAttributes != null
                        && !currentAttributes.isEmpty()) {
                    Set<Pair<String, Object>> currentKeyValuePairs = currentAttributes.stream()
                            .map(item -> Pair.of(item.getKey(), item.getValue()))
                            .collect(Collectors.toSet());
                    attributes = attributes.stream()
                            .filter(item -> !currentKeyValuePairs.contains(Pair.of(item.getKey(), item.getValue())))
                            .collect(Collectors.toList());
                }
                if (attributes.isEmpty()) {
                    ctx.tellSuccess(msg);
                } else {
                    ctx.getTelemetryService().saveAndNotify(
                            ctx.getTenantId(),
                            msg.getOriginator(),
                            scope,
                            attributes,
                            checkNotifyDevice(msg.getMetaData().getValue(NOTIFY_DEVICE_METADATA_KEY)),
                            sendAttributesUpdateNotification ?
                                    new AttributesUpdateNodeCallback(ctx, msg, scope, attributes) :
                                    new TelemetryNodeCallback(ctx, msg)
                    );
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                ctx.tellFailure(msg, throwable);
            }
        }, ctx.getDbCallbackExecutor());

    }

    private boolean checkSendNotification(String scope) {
        return config.isSendAttributesUpdatedNotification() && !CLIENT_SCOPE.equals(scope);
    }

    private boolean checkNotifyDevice(String notifyDeviceMdValue) {
        return config.getNotifyDevice() || StringUtils.isEmpty(notifyDeviceMdValue) || Boolean.parseBoolean(notifyDeviceMdValue);
    }

    private String getScope(String mdScopeValue) {
        if (StringUtils.isNotEmpty(mdScopeValue)) {
            return mdScopeValue;
        }
        return config.getScope();
    }

}
