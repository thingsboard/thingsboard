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
package org.thingsboard.rule.engine.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.rule.engine.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;

import static org.thingsboard.rule.engine.DonAsynchron.withCallback;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "send email",
        configClazz = TbSendEmailNodeConfiguration.class,
        nodeDescription = "Log incoming messages using JS script for transformation Message into String",
        nodeDetails = "Transform incoming Message with configured JS condition to String and log final value. " +
                "Message payload can be accessed via <code>msg</code> property. For example <code>'temperature = ' + msg.temperature ;</code>" +
                "Message metadata can be accessed via <code>metadata</code> property. For example <code>'name = ' + metadata.customerName;</code>")
public class TbSendEmailNode implements TbNode {

    static final String SEND_EMAIL_TYPE = "SEND_EMAIL";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TbSendEmailNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbSendEmailNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            validateType(msg.getType());
            EmailPojo email = getEmail(msg);
            withCallback(ctx.getMailExecutor().executeAsync(() -> {
                        ctx.getMailService().send(email.getFrom(), email.getTo(), email.getCc(),
                                email.getBcc(), email.getSubject(), email.getBody());
                        return null;
                    }),
                    ok -> ctx.tellNext(msg),
                    fail -> ctx.tellError(msg, fail));
        } catch (Exception ex) {
            ctx.tellError(msg, ex);
        }
    }

    private EmailPojo getEmail(TbMsg msg) throws IOException {
        EmailPojo email = MAPPER.readValue(msg.getData(), EmailPojo.class);
        if (StringUtils.isBlank(email.getTo())) {
            throw new IllegalStateException("Email destination can not be blank [" + email.getTo() + "]");
        }
        return email;
    }

    private void validateType(String type) {
        if (!SEND_EMAIL_TYPE.equals(type)) {
            log.warn("Not expected msg type [{}] for SendEmail Node", type);
            throw new IllegalStateException("Not expected msg type " + type + " for SendEmail Node");
        }
    }

    @Override
    public void destroy() {
    }
}
