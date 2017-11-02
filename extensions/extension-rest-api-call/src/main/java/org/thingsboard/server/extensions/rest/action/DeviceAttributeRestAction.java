package org.thingsboard.server.extensions.rest.action;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.msg.core.BasicUpdateAttributesRequest;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.ToDeviceMsg;
import org.thingsboard.server.extensions.api.component.Action;
import org.thingsboard.server.extensions.api.plugins.PluginAction;
import org.thingsboard.server.extensions.api.plugins.msg.PluginToRuleMsg;
import org.thingsboard.server.extensions.api.plugins.msg.RuleToPluginMsg;
import org.thingsboard.server.extensions.api.rules.RuleContext;
import org.thingsboard.server.extensions.api.rules.RuleProcessingMetaData;

import java.util.*;
import java.util.logging.Logger;

@Action(name = "Device Attributes Rest Plugin Action",
        descriptor = "DeviceAttributeRestDescriptor.json", configuration = RestApiCallPluginActionConfiguration.class)
@Slf4j
public class DeviceAttributeRestAction implements PluginAction<RestApiCallPluginActionConfiguration>{
    private RestApiCallPluginActionConfiguration configuration;
    private final org.slf4j.Logger logger = LoggerFactory.getLogger(DeviceAttributeRestAction.class);
    @Override
    public void resume() {

    }

    @Override
    public void suspend() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void init(RestApiCallPluginActionConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Optional<RuleToPluginMsg<?>> convert(RuleContext ctx, ToDeviceActorMsg msg, RuleProcessingMetaData deviceMsgMd) {
        BasicUpdateAttributesRequest payload;

        String device = ctx.getDeviceMetaData().getDeviceName();
        if (msg.getPayload() instanceof BasicUpdateAttributesRequest) {
            payload = (BasicUpdateAttributesRequest) msg.getPayload();
        } else {
            throw new IllegalArgumentException("Action does not support messages of type: " + msg.getPayload().getMsgType());
        }

        HashMap<String, String> attributes = new HashMap<>(payload.getAttributes().size());
        attributes.put("deviceName",device);
        String tagsStr = "";
        //Comma separated tags.
        for(AttributeKvEntry attr: payload.getAttributes()){
            tagsStr = tagsStr + attr.getValueAsString() + ",";
            //attributes.put(attr.getKey(), attr.getValueAsString());
        }
        tagsStr = tagsStr.substring(0,tagsStr.length() - 1);
        attributes.put("tags",tagsStr);
        ObjectMapper mapper = new ObjectMapper();
        String msgBody;
        try {
            msgBody = mapper.writeValueAsString(attributes);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Action does not support messages of type: " + msg.getPayload().getMsgType());
        }
        RestApiCallActionPayload.RestApiCallActionPayloadBuilder builder = RestApiCallActionPayload.builder();
        builder.msgType(payload.getMsgType());
        builder.requestId(payload.getRequestId());
        builder.sync(configuration.isSync());
        builder.actionPath(configuration.getActionPath());
        builder.httpMethod(HttpMethod.valueOf(configuration.getRequestMethod()));
        builder.expectedResultCode(HttpStatus.valueOf(configuration.getExpectedResultCode()));
        builder.msgBody(msgBody);
        return Optional.of(new RestApiCallActionMsg(msg.getTenantId(),
                msg.getCustomerId(),
                msg.getDeviceId(),
                builder.build()));
    }

    @Override
    public Optional<ToDeviceMsg> convert(PluginToRuleMsg<?> response) {
        return null;
    }

    @Override
    public boolean isOneWayAction() {
        return false;
    }

}
