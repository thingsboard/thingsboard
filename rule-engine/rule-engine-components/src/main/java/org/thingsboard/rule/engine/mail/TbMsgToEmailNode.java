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

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.parser.ParseException;
import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.TbNodeUtils;
import org.thingsboard.rule.engine.api.*;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.io.IOException;
import java.util.Optional;

import static org.thingsboard.rule.engine.mail.TbSendEmailNode.SEND_EMAIL_TYPE;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "to email",
        configClazz = TbMsgToEmailNodeConfiguration.class,
        nodeDescription = "Change Message Originator To Tenant/Customer/Related Entity",
        nodeDetails = "Related Entity found using configured relation direction and Relation Type. " +
                "If multiple Related Entities are found, only first Entity is used as new Originator, other entities are discarded. ",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeToEmailConfig")
public class TbMsgToEmailNode implements TbNode {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private TbMsgToEmailNodeConfiguration config;

    private Optional<Template> fromTemplate;
    private Optional<Template> toTemplate;
    private Optional<Template> ccTemplate;
    private Optional<Template> bccTemplate;
    private Optional<Template> subjectTemplate;
    private Optional<Template> bodyTemplate;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbMsgToEmailNodeConfiguration.class);
        try {
            fromTemplate = toTemplate(config.getFromTemplate(), "From Template");
            toTemplate = toTemplate(config.getToTemplate(), "To Template");
            ccTemplate = toTemplate(config.getCcTemplate(), "Cc Template");
            bccTemplate = toTemplate(config.getBccTemplate(), "Bcc Template");
            subjectTemplate = toTemplate(config.getSubjectTemplate(), "Subject Template");
            bodyTemplate = toTemplate(config.getBodyTemplate(), "Body Template");
        } catch (ParseException e) {
            log.error("Failed to create templates based on provided configuration!", e);
            throw new TbNodeException(e);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            EmailPojo email = convert(msg);
            TbMsg emailMsg = buildEmailMsg(ctx, msg, email);
            ctx.tellNext(emailMsg);
        } catch (Exception ex) {
            log.warn("Can not convert message to email " + ex.getMessage());
            ctx.tellError(msg, ex);
        }
    }

    private TbMsg buildEmailMsg(TbContext ctx, TbMsg msg, EmailPojo email) throws JsonProcessingException {
        String emailJson = MAPPER.writeValueAsString(email);
        return ctx.newMsg(SEND_EMAIL_TYPE, msg.getOriginator(), msg.getMetaData().copy(), emailJson);
    }

    private EmailPojo convert(TbMsg msg) throws IOException {
        EmailPojo.EmailPojoBuilder builder = EmailPojo.builder();
        VelocityContext context = RuleVelocityUtils.createContext(msg);
        fromTemplate.ifPresent(t -> builder.from(RuleVelocityUtils.merge(t, context)));
        toTemplate.ifPresent(t -> builder.to(RuleVelocityUtils.merge(t, context)));
        ccTemplate.ifPresent(t -> builder.cc(RuleVelocityUtils.merge(t, context)));
        bccTemplate.ifPresent(t -> builder.bcc(RuleVelocityUtils.merge(t, context)));
        subjectTemplate.ifPresent(t -> builder.subject(RuleVelocityUtils.merge(t, context)));
        bodyTemplate.ifPresent(t -> builder.body(RuleVelocityUtils.merge(t, context)));
        return builder.build();
    }

    private Optional<Template> toTemplate(String source, String name) throws ParseException {
        if (!StringUtils.isEmpty(source)) {
            return Optional.of(RuleVelocityUtils.create(source, name));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void destroy() {

    }
}
