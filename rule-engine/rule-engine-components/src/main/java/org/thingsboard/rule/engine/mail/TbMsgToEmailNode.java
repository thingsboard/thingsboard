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
package org.thingsboard.rule.engine.mail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.io.IOException;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;
import static org.thingsboard.rule.engine.mail.TbSendEmailNode.SEND_EMAIL_TYPE;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "转换为邮件",
        configClazz = TbMsgToEmailNodeConfiguration.class,
        nodeDescription = "将消息转换为电子邮件消息",
        nodeDetails = "通过使用从消息元数据派生的值填充电子邮件字段，将消息转换为电子邮件消息。" +
                      "设置'SEND_EMAIL'为输出消息类型。",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeToEmailConfig",
        icon = "email"
)
public class TbMsgToEmailNode implements TbNode {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TbMsgToEmailNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgToEmailNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            EmailPojo email = convert(msg);
            TbMsg emailMsg = buildEmailMsg(ctx, msg, email);
            ctx.tellNext(emailMsg, SUCCESS);
        } catch (Exception ex) {
            log.warn("Can not convert message to email " + ex.getMessage());
            ctx.tellFailure(msg, ex);
        }
    }

    private TbMsg buildEmailMsg(TbContext ctx, TbMsg msg, EmailPojo email) throws JsonProcessingException {
        String emailJson = MAPPER.writeValueAsString(email);
        return ctx.transformMsg(msg, SEND_EMAIL_TYPE, msg.getOriginator(), msg.getMetaData().copy(), emailJson);
    }

    private EmailPojo convert(TbMsg msg) throws IOException {
        EmailPojo.EmailPojoBuilder builder = EmailPojo.builder();
        builder.from(fromTemplate(this.config.getFromTemplate(), msg.getMetaData()));
        builder.to(fromTemplate(this.config.getToTemplate(), msg.getMetaData()));
        builder.cc(fromTemplate(this.config.getCcTemplate(), msg.getMetaData()));
        builder.bcc(fromTemplate(this.config.getBccTemplate(), msg.getMetaData()));
        builder.subject(fromTemplate(this.config.getSubjectTemplate(), msg.getMetaData()));
        builder.body(fromTemplate(this.config.getBodyTemplate(), msg.getMetaData()));
        return builder.build();
    }

    private String fromTemplate(String template, TbMsgMetaData metaData) {
        if (!StringUtils.isEmpty(template)) {
            return TbNodeUtils.processPattern(template, metaData);
        } else {
            return null;
        }
    }

    @Override
    public void destroy() {

    }
}
