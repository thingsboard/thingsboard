/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.rule.engine.geo;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * Created by ashvayka on 19.01.18.
 */
@Slf4j
@RuleNode(
        type = ComponentType.FILTER,
        name = "gps geofencing filter",
        configClazz = TbGpsGeofencingFilterNodeConfiguration.class,
        relationTypes = {"True", "False"},
        nodeDescription = "Filter incoming messages by GPS based geofencing",
        nodeDetails = "Extracts latitude and longitude parameters from incoming message and returns 'True' if they are inside configured perimeters, 'False' otherwise.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbFilterNodeGpsGeofencingConfig")
public class TbGpsGeofencingFilterNode extends AbstractGeofencingNode<TbGpsGeofencingFilterNodeConfiguration> {

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws TbNodeException {
        ctx.tellNext(msg, checkMatches(msg) ? "True" : "False");
    }

    @Override
    protected Class<TbGpsGeofencingFilterNodeConfiguration> getConfigClazz() {
        return TbGpsGeofencingFilterNodeConfiguration.class;
    }
}
