// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.pe.twilio.voice;

import com.twilio.http.TwilioRestClient;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.twiml.VoiceResponse;
import com.twilio.twiml.voice.Pause;
import com.twilio.twiml.voice.Say;
import com.twilio.twiml.voice.SsmlProsody;
import com.twilio.type.PhoneNumber;
import com.twilio.type.Twiml;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@RuleNode(
        type = ComponentType.EXTERNAL,
        name = "twilio voice",
        configClazz = TbTwilioVoiceNodeConfiguration.class,
        nodeDescription = "Sends voice message via Twilio.",
        nodeDetails = "Will send message payload as voice message via Twilio, using Twilio text to speech service.",
        uiResources = {"static/rulenode/twilio-config.js"},
        configDirective = "tbActionNodeTwilioVoiceConfig",
        icon = "phone_in_talk",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/external/twilio-voice/",
        hasSecrets = true
)
public class TbTwilioVoiceNode implements TbNode {

    private boolean forceAck;
    private TbTwilioVoiceNodeConfiguration config;
    private TwilioRestClient twilioRestClient;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.forceAck = ctx.isExternalNodeForceAck();
        this.config = TbNodeUtils.convert(configuration, TbTwilioVoiceNodeConfiguration.class);
        this.twilioRestClient = new TwilioRestClient.Builder(this.config.getAccountSid(), this.config.getAccountToken()).build();
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var tbMsg = ackIfNeeded(ctx, msg);
        withCallback(ctx.getExternalCallExecutor().executeAsync(() -> {
                    sendVoiceMessage(tbMsg);
                    return null;
                }),
                ok -> {
                    if (forceAck) {
                        ctx.enqueueForTellNext(tbMsg.copyWithNewCtx().build(), TbNodeConnectionType.SUCCESS);
                    } else {
                        ctx.tellSuccess(tbMsg);
                    }
                },
                fail -> {
                    if (forceAck) {
                        ctx.enqueueForTellFailure(tbMsg.copyWithNewCtx().build(), fail);
                    } else {
                        ctx.tellFailure(tbMsg, fail);
                    }
                });
    }

    private TbMsg ackIfNeeded(TbContext ctx, TbMsg msg) {
        if (forceAck) {
            ctx.ack(msg);
            return msg.copyWithNewCtx().build();
        } else {
            return msg;
        }
    }

    private void sendVoiceMessage(TbMsg msg) throws Exception {
        String numberFrom = TbNodeUtils.processPattern(this.config.getNumberFrom(), msg);
        String numbersTo = TbNodeUtils.processPattern(this.config.getNumbersTo(), msg);

        String[] numbersToList = numbersTo.split(",");
        if (StringUtils.isBlank(numbersToList[0])) {
            throw new IllegalArgumentException("To numbers list is empty!");
        }

        String payload = msg.getData();
        payload = payload.substring(1, payload.length() - 1);
        Say.Language language = Say.Language.EN_US;
        for (Say.Language lang : Say.Language.values()) {
            if (lang.toString().equals(config.getLanguage())) {
                language = lang;
                break;
            }
        }

        Say.Voice voice = Say.Voice.MAN;
        for (Say.Voice voiceIter : Say.Voice.values()) {
            if (voiceIter.toString().equals(config.getVoice())) {
                voice = voiceIter;
                break;
            }
        }

        SsmlProsody prosody = new SsmlProsody.Builder(payload)
                .pitch(config.getPitch().toString() + "%")
                .rate(config.getRate().toString() + "%")
                .volume(config.getVolume().toString() + "dB")
                .build();

        Pause startPause = new Pause.Builder().length(config.getStartPause()).build();
        Say say = new Say.Builder().language(language).voice(voice).prosody(prosody).build();
        VoiceResponse response = new VoiceResponse.Builder().pause(startPause).say(say).build();

        for (String numberTo : numbersToList) {
            Call.creator(
                    new PhoneNumber(numberTo.trim()),
                    new PhoneNumber(numberFrom.trim()),
                    new Twiml(response.toXml())
            ).create(this.twilioRestClient);
        }
    }

}
