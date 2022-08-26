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
package org.thingsboard.rule.engine.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.concurrent.ExecutionException;

@Slf4j
@RuleNode(
        type = ComponentType.ENRICHMENT,
        name = "fetch device credentials",
        configClazz = TbFetchDeviceCredentialsNodeConfiguration.class,
        nodeDescription = "Fetch device credentials for message originator",
        nodeDetails = "Adds <b>credentialsType</b> and <b>credentials</b> properties to the message metadata if the " +
                "configuration parameter <b>fetchToMetadata</b> is set to <code>true</code>, otherwise, adds properties " +
                "to the message data. If originator type is not <b>DEVICE</b> or rule node failed to get device credentials " +
                "- send Message via <code>Failure</code> chain, otherwise <code>Success</code> chain is used.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbEnrichmentNodeFetchDeviceCredentialsConfig",
        icon = "functions"
)
public class TbFetchDeviceCredentialsNode implements TbNode {

    private static final String CREDENTIALS = "credentials";
    private static final String CREDENTIALS_TYPE = "credentialsType";

    TbFetchDeviceCredentialsNodeConfiguration config;
    boolean fetchToMetadata;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbFetchDeviceCredentialsNodeConfiguration.class);
        this.fetchToMetadata = config.isFetchToMetadata();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        EntityId originator = msg.getOriginator();
        if (!EntityType.DEVICE.equals(originator.getEntityType())) {
            ctx.tellFailure(msg, new RuntimeException("Unsupported originator type: " + originator.getEntityType() + "!"));
            return;
        }
        DeviceId deviceId = new DeviceId(msg.getOriginator().getId());
        DeviceCredentials deviceCredentials = ctx.getDeviceCredentialsService().findDeviceCredentialsByDeviceId(ctx.getTenantId(), deviceId);
        if (deviceCredentials == null) {
            ctx.tellFailure(msg, new RuntimeException("Failed to get Device Credentials for device: " + deviceId + "!"));
            return;
        }

        TbMsg transformedMsg;
        String credentialsType = deviceCredentials.getCredentialsType().name();
        JsonNode credentialsInfo = ctx.getDeviceCredentialsService().toCredentialsInfo(deviceCredentials);
        if (fetchToMetadata) {
            TbMsgMetaData metaData = msg.getMetaData();
            metaData.putValue(CREDENTIALS_TYPE, credentialsType);
            metaData.putValue(CREDENTIALS, JacksonUtil.toString(credentialsInfo));
            transformedMsg = TbMsg.transformMsg(msg, msg.getType(), originator, metaData, msg.getData());
        } else {
            ObjectNode data = (ObjectNode) JacksonUtil.toJsonNode(msg.getData());
            data.put(CREDENTIALS_TYPE, credentialsType);
            data.set(CREDENTIALS, credentialsInfo);
            transformedMsg = TbMsg.transformMsg(msg, msg.getType(), originator, msg.getMetaData(), JacksonUtil.toString(data));
        }
        ctx.tellSuccess(transformedMsg);
    }

    @Override
    public void destroy() {
    }

}

