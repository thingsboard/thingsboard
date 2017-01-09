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
package org.thingsboard.server.extensions.core.filter;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.msg.device.ToDeviceActorMsg;
import org.thingsboard.server.common.msg.session.MsgType;
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

    private List<MsgType> msgTypes;

    @Override
    public void init(MsgTypeFilterConfiguration configuration) {
        msgTypes = Arrays.stream(configuration.getMessageTypes()).map(type -> {
            switch (type) {
                case "GET_ATTRIBUTES":
                    return MsgType.GET_ATTRIBUTES_REQUEST;
                case "POST_ATTRIBUTES":
                    return MsgType.POST_ATTRIBUTES_REQUEST;
                case "POST_TELEMETRY":
                    return MsgType.POST_TELEMETRY_REQUEST;
                case "RPC_REQUEST":
                    return MsgType.TO_SERVER_RPC_REQUEST;
                default:
                    throw new InvalidParameterException("Can't map " + type + " to " + MsgType.class.getName() + "!");
            }
        }).collect(Collectors.toList());
    }

    @Override
    public boolean filter(RuleContext ctx, ToDeviceActorMsg msg) {
        for (MsgType msgType : msgTypes) {
            if (msgType == msg.getPayload().getMsgType()) {
                return true;
            }
        }
        return false;
    }
}
