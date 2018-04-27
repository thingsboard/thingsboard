/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.extensions.kinesis.plugin;

import com.amazonaws.handlers.AsyncHandler;
import com.amazonaws.services.kinesis.AmazonKinesisAsync;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.core.BasicStatusCodeResponse;
import org.thingsboard.server.extensions.api.plugins.PluginContext;
import org.thingsboard.server.extensions.api.plugins.handlers.RuleMsgHandler;
import org.thingsboard.server.extensions.api.plugins.msg.ResponsePluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleException;
import org.thingsboard.server.extensions.kinesis.action.KinesisActionMsg;
import org.thingsboard.server.extensions.kinesis.action.KinesisActionPayload;

@RequiredArgsConstructor
@Slf4j
public class KinesisMsgHandler implements RuleMsgHandler {

    private final AmazonKinesisAsync kinesis;

    @Override
    public void process(PluginContext ctx, TenantId tenantId, RuleId ruleId, RuleToPluginMsg<?> msg) throws RuleException {
        if (!(msg instanceof KinesisActionMsg)) {
            throw new RuleException("Unsupported message type " + msg.getClass().getName() + "!");
        }

        KinesisActionPayload payload = ((KinesisActionMsg) msg).getPayload();
        log.debug("Processing Kinesis payload: {}", payload);

        try {
            PutRecordRequest putRecordRequest = KinesisPutRecordRequestFactory.INSTANCE.create((KinesisActionMsg) msg);
            kinesis.putRecordAsync(putRecordRequest, new AsyncHandler<PutRecordRequest, PutRecordResult>() {

                @Override
                public void onError(Exception e) {
                    if (payload.isSync()) {
                        ctx.reply(new ResponsePluginToRuleMsg(msg.getUid(), tenantId, ruleId,
                                BasicStatusCodeResponse.onError(payload.getMsgType(), payload.getRequestId(), e)));
                    }
                }

                @Override
                public void onSuccess(PutRecordRequest request, PutRecordResult putRecordResult) {
                    if (payload.isSync()) {
                        ctx.reply(new ResponsePluginToRuleMsg(msg.getUid(), tenantId, ruleId,
                                BasicStatusCodeResponse.onSuccess(payload.getMsgType(), payload.getRequestId())));
                    }
                }
            });
        } catch (Exception e) {
            throw new RuleException(e.getMessage(), e);
        }
    }
}
