package org.thingsboard.server.extensions.core.action.rpc;

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
 * Created by ashvayka on 14.09.17.
 */
@Action(name = "Server Side RPC Call Action", descriptor = "ServerSideRpcCallActionDescriptor.json", configuration = ServerSideRpcCallActionConfiguration.class)
@Slf4j
public class ServerSideRpcCallAction extends SimpleRuleLifecycleComponent implements PluginAction<ServerSideRpcCallActionConfiguration> {

    private ServerSideRpcCallActionConfiguration configuration;
    private Optional<Template> deviceIdTemplate;
    private Optional<Template> deviceRelationTemplate;
    private Optional<Template> rpcCallMethodTemplate;
    private Optional<Template> rpcCallBodyTemplate;

    @Override
    public void init(ServerSideRpcCallActionConfiguration configuration) {
        this.configuration = configuration;
        try {
            deviceIdTemplate = toTemplate(configuration.getDeviceIdTemplate(), "Device Id Template");
            deviceRelationTemplate = toTemplate(configuration.getDeviceRelationTemplate(), "Device Relation Template");
            rpcCallMethodTemplate = toTemplate(configuration.getRpcCallMethodTemplate(), "RPC Call Method Template");
            rpcCallBodyTemplate = toTemplate(configuration.getRpcCallBodyTemplate(), "RPC Call Body Template");
        } catch (ParseException e) {
            log.error("Failed to create templates based on provided configuration!", e);
            throw new RuntimeException("Failed to create templates based on provided configuration!", e);
        }
    }

    @Override
    public Optional<RuleToPluginMsg<?>> convert(RuleContext ctx, ToDeviceActorMsg toDeviceActorMsg, RuleProcessingMetaData metadata) {
        String sendFlag = configuration.getSendFlag();
        if (StringUtils.isEmpty(sendFlag) || (Boolean) metadata.get(sendFlag).orElse(Boolean.FALSE)) {
            VelocityContext context = VelocityUtils.createContext(metadata);

            ServerSideRpcCallActionMsg.ServerSideRpcCallActionMsgBuilder builder = ServerSideRpcCallActionMsg.builder();

            deviceIdTemplate.ifPresent(t -> builder.deviceId(VelocityUtils.merge(t, context)));
            deviceRelationTemplate.ifPresent(t -> builder.deviceRelation(VelocityUtils.merge(t, context)));
            rpcCallMethodTemplate.ifPresent(t -> builder.rpcCallMethod(VelocityUtils.merge(t, context)));
            rpcCallBodyTemplate.ifPresent(t -> builder.rpcCallBody(VelocityUtils.merge(t, context)));
            return Optional.of(new ServerSideRpcCallRuleToPluginActionMsg(toDeviceActorMsg.getTenantId(), toDeviceActorMsg.getCustomerId(), toDeviceActorMsg.getDeviceId(),
                    builder.build()));
        } else {
            return Optional.empty();
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
