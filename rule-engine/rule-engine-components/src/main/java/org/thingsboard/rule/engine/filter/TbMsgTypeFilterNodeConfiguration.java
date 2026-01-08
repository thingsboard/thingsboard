/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.rule.engine.filter;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

import java.util.Arrays;
import java.util.List;

import static org.thingsboard.server.common.data.msg.TbMsgType.POST_ATTRIBUTES_REQUEST;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_TELEMETRY_REQUEST;
import static org.thingsboard.server.common.data.msg.TbMsgType.TO_SERVER_RPC_REQUEST;

/**
 * Created by ashvayka on 19.01.18.
 */
@Data
public class TbMsgTypeFilterNodeConfiguration implements NodeConfiguration<TbMsgTypeFilterNodeConfiguration> {

    private List<String> messageTypes;

    @Override
    public TbMsgTypeFilterNodeConfiguration defaultConfiguration() {
        var configuration = new TbMsgTypeFilterNodeConfiguration();
        configuration.setMessageTypes(Arrays.asList(
                POST_ATTRIBUTES_REQUEST.name(),
                POST_TELEMETRY_REQUEST.name(),
                TO_SERVER_RPC_REQUEST.name()));
        return configuration;
    }
}
