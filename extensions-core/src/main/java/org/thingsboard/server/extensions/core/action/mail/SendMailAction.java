/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.extensions.core.action.mail;

import lombok.extern.slf4j.Slf4j;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.parser.ParseException;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.extensions.api.component.Action;
import org.thingsboard.server.extensions.api.plugins.PluginAction;
import org.thingsboard.server.extensions.api.plugins.msg.PluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.ResponsePluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleContext;
import org.thingsboard.server.extensions.api.rules.RuleProcessingMetaData;
import org.thingsboard.server.extensions.api.rules.SimpleRuleLifecycleComponent;
import org.thingsboard.server.extensions.core.utils.VelocityUtils;

import java.util.Optional;

/**
 * @author Andrew Shvayka
 */
@Action(name = "Send Mail Action", descriptor = "SendMailActionDescriptor.json", configuration = SendMailActionConfiguration.class)
@Slf4j
public class SendMailAction extends SimpleRuleLifecycleComponent implements PluginAction<SendMailActionConfiguration> {

    private SendMailActionConfiguration configuration;
    private Optional<Template> fromTemplate;
    private Optional<Template> toTemplate;
    private Optional<Template> ccTemplate;
    private Optional<Template> bccTemplate;
    private Optional<Template> subjectTemplate;
    private Optional<Template> bodyTemplate;

    @Override
    public void init(SendMailActionConfiguration configuration) {
        this.configuration = configuration;
        try {
            fromTemplate = toTemplate(configuration.getFromTemplate(), "From Template");
            toTemplate = toTemplate(configuration.getToTemplate(), "To Template");
            ccTemplate = toTemplate(configuration.getCcTemplate(), "Cc Template");
            bccTemplate = toTemplate(configuration.getBccTemplate(), "Bcc Template");
            subjectTemplate = toTemplate(configuration.getSubjectTemplate(), "Subject Template");
            bodyTemplate = toTemplate(configuration.getBodyTemplate(), "Body Template");
        } catch (ParseException e) {
            log.error("Failed to create templates based on provided configuration!", e);
            throw new RuntimeException("Failed to create templates based on provided configuration!", e);
        }
    }

    private Optional<Template> toTemplate(String source, String name) throws ParseException {
        if (!StringUtils.isEmpty(source)) {
            return Optional.of(VelocityUtils.create(source, name));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<RuleToPluginMsg<?>> convert(RuleContext ctx, ToDeviceActorMsg toDeviceActorMsg, RuleProcessingMetaData metadata) {
        String sendFlag = configuration.getSendFlag();
        if (StringUtils.isEmpty(sendFlag) || (Boolean) metadata.get(sendFlag).orElse(Boolean.FALSE)) {
            VelocityContext context = VelocityUtils.createContext(metadata);

            SendMailActionMsg.SendMailActionMsgBuilder builder = SendMailActionMsg.builder();
            fromTemplate.ifPresent(t -> builder.from(VelocityUtils.merge(t, context)));
            toTemplate.ifPresent(t -> builder.to(VelocityUtils.merge(t, context)));
            ccTemplate.ifPresent(t -> builder.cc(VelocityUtils.merge(t, context)));
            bccTemplate.ifPresent(t -> builder.bcc(VelocityUtils.merge(t, context)));
            subjectTemplate.ifPresent(t -> builder.subject(VelocityUtils.merge(t, context)));
            bodyTemplate.ifPresent(t -> builder.body(VelocityUtils.merge(t, context)));
            return Optional.of(new SendMailRuleToPluginActionMsg(toDeviceActorMsg.getTenantId(), toDeviceActorMsg.getCustomerId(), toDeviceActorMsg.getDeviceId(),
                    builder.build()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<ToDeviceMsg> convert(PluginToRuleMsg<?> response) {
        if (response instanceof ResponsePluginToRuleMsg) {
            return Optional.of(((ResponsePluginToRuleMsg) response).getPayload());
        }
        return Optional.empty();
    }

    @Override
    public boolean isOneWayAction() {
        return true;
    }

}
