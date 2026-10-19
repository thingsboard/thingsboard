// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.telemetry;

import org.thingsboard.rule.engine.api.AttributesDeleteRequest;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.thingsboard.server.common.data.DataConstants.NOTIFY_DEVICE_METADATA_KEY;
import static org.thingsboard.server.common.data.DataConstants.SCOPE;

@RuleNode(
        type = ComponentType.ACTION,
        name = "delete attributes",
        configClazz = TbMsgDeleteAttributesNodeConfiguration.class,
        nodeDescription = "Delete attributes for Message Originator.",
        nodeDetails = "Attempt to remove attributes by selected keys. If msg originator doesn't have an attribute with " +
                " a key selected in the configuration, it will be ignored. If delete operation is completed successfully, " +
                " rule node will send the \"Attributes Deleted\" event to the root chain of the message originator and " +
                " send the incoming message via <b>Success</b> chain, otherwise, <b>Failure</b> chain is used.",
        configDirective = "tbActionNodeDeleteAttributesConfig",
        icon = "remove_circle",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/delete-attributes/"
)
public class TbMsgDeleteAttributesNode implements TbNode {

    private TbMsgDeleteAttributesNodeConfiguration config;
    private List<String> keys;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = TbNodeUtils.convert(configuration, TbMsgDeleteAttributesNodeConfiguration.class);
        keys = config.getKeys();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        List<String> keysToDelete = keys.stream()
                .map(keyPattern -> TbNodeUtils.processPattern(keyPattern, msg))
                .distinct()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        if (keysToDelete.isEmpty()) {
            ctx.tellSuccess(msg);
        } else {
            AttributeScope scope = getScope(msg.getMetaData().getValue(SCOPE));
            ctx.getTelemetryService().deleteAttributes(AttributesDeleteRequest.builder()
                    .tenantId(ctx.getTenantId())
                    .entityId(msg.getOriginator())
                    .scope(scope)
                    .keys(keysToDelete)
                    .notifyDevice(checkNotifyDevice(msg.getMetaData().getValue(NOTIFY_DEVICE_METADATA_KEY), scope))
                    .previousCalculatedFieldIds(msg.getPreviousCalculatedFieldIds())
                    .tbMsgId(msg.getId())
                    .tbMsgType(msg.getInternalType())
                    .callback(config.isSendAttributesDeletedNotification() ?
                            new AttributesDeleteNodeCallback(ctx, msg, scope.name(), keysToDelete) :
                            new TelemetryNodeCallback(ctx, msg))
                    .build());
        }
    }

    private AttributeScope getScope(String mdScopeValue) {
        if (StringUtils.isNotEmpty(mdScopeValue)) {
            return AttributeScope.valueOf(mdScopeValue);
        }
        return AttributeScope.valueOf(config.getScope());
    }

    private boolean checkNotifyDevice(String notifyDeviceMdValue, AttributeScope scope) {
        return (AttributeScope.SHARED_SCOPE == scope) && (config.isNotifyDevice() || Boolean.parseBoolean(notifyDeviceMdValue));
    }

}
