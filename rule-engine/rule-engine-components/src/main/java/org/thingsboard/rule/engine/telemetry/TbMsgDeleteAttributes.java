/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "delete attributes",
        configClazz = TbMsgDeleteAttributesConfiguration.class,
        nodeDescription = "Delete attributes for Message Originator.",
        nodeDetails = "Allowed scope parameter values: <b>SERVER/CLIENT/SHARED</b>. If no attributes are selected - " +
                "message send via <b>Failure</b> chain. If selected attributes successfully deleted - message send via " +
                "<b>Success</b> chain, otherwise <b>Failure</b> chain will be used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeDeleteAttributesConfig",
        icon = "remove_circle"
)
public class TbMsgDeleteAttributes implements TbNode {

    TbMsgDeleteAttributesConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgDeleteAttributesConfiguration.class);
        if (CollectionUtils.isEmpty(config.getKeys())) {
            throw new IllegalArgumentException("Attribute keys list is empty!");
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        List<String> keysPatterns = config.getKeys();
        String scope = TbNodeUtils.processPattern(config.getScope(), msg);
        if (DataConstants.SERVER_SCOPE.equals(scope) ||
                DataConstants.CLIENT_SCOPE.equals(scope) ||
                DataConstants.SHARED_SCOPE.equals(scope)) {
            List<String> keysToDelete = keysPatterns.stream()
                    .map(keyPattern -> TbNodeUtils.processPattern(keyPattern, msg))
                    .distinct()
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (keysToDelete.isEmpty()) {
                throw new RuntimeException("Selected keys patterns have invalid values!");
            }
            ctx.getTelemetryService().deleteAndNotify(ctx.getTenantId(), msg.getOriginator(), scope, keysToDelete, new TelemetryNodeCallback(ctx, msg));
        } else {
            ctx.tellFailure(msg, new IllegalArgumentException("Unsupported attributes scope '" + scope + "'! Only 'SERVER_SCOPE', 'CLIENT_SCOPE' or 'SHARED_SCOPE' are allowed!"));
        }
    }

    @Override
    public void destroy() {

    }
}
