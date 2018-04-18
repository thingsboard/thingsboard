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
package org.thingsboard.server.extensions.core.filter;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.device.DeviceToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.extensions.api.component.Filter;
import org.thingsboard.server.extensions.api.rules.RuleContext;
import org.thingsboard.server.extensions.api.rules.RuleFilter;
import org.thingsboard.server.extensions.api.rules.SimpleRuleLifecycleComponent;

import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Andrew Shvayka
 */
@Filter(name = "Message Type Filter", descriptor = "MsgTypeFilterDescriptor.json", configuration = MsgTypeFilterConfiguration.class)
@Slf4j
public class MsgTypeFilter extends SimpleRuleLifecycleComponent implements RuleFilter<MsgTypeFilterConfiguration> {

    private List<SessionMsgType> sessionMsgTypes;

    @Override
    public void init(MsgTypeFilterConfiguration configuration) {
        sessionMsgTypes = Arrays.stream(configuration.getMessageTypes()).map(type -> {
            switch (type) {
                case "GET_ATTRIBUTES":
                    return SessionMsgType.GET_ATTRIBUTES_REQUEST;
                case "POST_ATTRIBUTES":
                    return SessionMsgType.POST_ATTRIBUTES_REQUEST;
                case "POST_TELEMETRY":
                    return SessionMsgType.POST_TELEMETRY_REQUEST;
                case "RPC_REQUEST":
                    return SessionMsgType.TO_SERVER_RPC_REQUEST;
                default:
                    throw new InvalidParameterException("Can't map " + type + " to " + SessionMsgType.class.getName() + "!");
            }
        }).collect(Collectors.toList());
    }

    @Override
    public boolean filter(RuleContext ctx, DeviceToDeviceActorMsg msg) {
        for (SessionMsgType sessionMsgType : sessionMsgTypes) {
            if (sessionMsgType == msg.getPayload().getMsgType()) {
                return true;
            }
        }
        return false;
    }
}
