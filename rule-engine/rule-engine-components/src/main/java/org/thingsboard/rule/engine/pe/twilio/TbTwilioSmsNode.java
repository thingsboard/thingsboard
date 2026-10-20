// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.pe.twilio;

import com.twilio.exception.ApiException;
import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "twilio sms",
        configClazz = TbTwilioSmsNodeConfiguration.class,
        nodeDescription = "Sends SMS message via Twilio.",
        nodeDetails = "Will send message payload as SMS message via Twilio.",
        uiResources = {"static/rulenode/twilio-config.js"},
        configDirective = "tbActionNodeTwilioSmsConfig",
        icon = "sms",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/external/twilio-sms/",
        hasSecrets = true
)
public class TbTwilioSmsNode implements TbNode {

    private boolean forceAck;
    private TbTwilioSmsNodeConfiguration config;
    private TwilioRestClient twilioRestClient;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.forceAck = ctx.isExternalNodeForceAck();
        this.config = TbNodeUtils.convert(configuration, TbTwilioSmsNodeConfiguration.class);
        this.twilioRestClient = new TwilioRestClient.Builder(this.config.getAccountSid(), this.config.getAccountToken()).build();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        log.trace("[{}][{}] Msg received: {}", ctx.getTenantId().getId(), ctx.getSelfId().getId(), msg);
        var tbMsg = ackIfNeeded(ctx, msg);
        try {
            withCallback(ctx.getExternalCallExecutor().executeAsync(() -> {
                        sendSms(ctx, tbMsg);
                        return null;
                    }),
                    ok -> {
                        log.trace("[{}][{}] Successfully processed msg: {}", ctx.getTenantId().getId(), ctx.getSelfId().getId(), tbMsg);
                        if (forceAck) {
                            ctx.enqueueForTellNext(tbMsg.copyWithNewCtx().build(), TbNodeConnectionType.SUCCESS);
                        } else {
                            ctx.tellNext(tbMsg, TbNodeConnectionType.SUCCESS);
                        }
                    },
                    fail -> {
                        logFailure(ctx, tbMsg, fail);
                        if (forceAck) {
                            ctx.enqueueForTellFailure(tbMsg.copyWithNewCtx().build(), fail);
                        } else {
                            ctx.tellFailure(tbMsg, fail);
                        }
                    });
        } catch (Exception ex) {
            logFailure(ctx, tbMsg, ex);
            ctx.tellFailure(tbMsg, ex);
        }
    }

    private TbMsg ackIfNeeded(TbContext ctx, TbMsg msg) {
        if (forceAck) {
            ctx.ack(msg);
            return msg.copyWithNewCtx().build();
        } else {
            return msg;
        }
    }

    private void logFailure(TbContext ctx, TbMsg msg, Throwable fail) {
        String errorMsg = String.format("[%s][%s] Failed to process msg: %s", ctx.getTenantId().getId(), ctx.getSelfId().getId(), msg);
        log.error(errorMsg, fail);
    }

    private void sendSms(TbContext ctx, TbMsg msg) {
        String numberFrom = TbNodeUtils.processPattern(this.config.getNumberFrom(), msg);
        String numbersTo = TbNodeUtils.processPattern(this.config.getNumbersTo(), msg);
        String[] numbersToList = numbersTo.split(",");
        if (numbersToList.length == 0) {
            throw new IllegalArgumentException("To numbers list is empty!");
        }
        for (String numberTo : numbersToList) {
            log.trace("[{}][{}][{}] Sending sms for number: {} ...", ctx.getTenantId().getId(), ctx.getSelfId().getId(), msg.getId(), numbersTo);
            try {
                Message.creator(
                        new PhoneNumber(numberTo.trim()),
                        new PhoneNumber(numberFrom.trim()),
                        msg.getData().replaceAll("^\"|\"$", "").replaceAll("\\\\n", "\n")
                ).create(this.twilioRestClient);
                log.trace("[{}][{}][{}] Sms for number: {} sent successfully!", ctx.getTenantId().getId(), ctx.getSelfId().getId(), msg.getId(), numbersTo);
            } catch (ApiException e) {
                String apiMsg = String.format("[%s][%s] Failed to send sms from number %s to number %s",
                        ctx.getTenantId().getId(), ctx.getSelfId().getId(), numberFrom, numberTo);
                log.debug(apiMsg, e);
                ctx.tellFailure(msg, new RuntimeException(apiMsg, e));
            }
        }
    }

}
