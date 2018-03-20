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
package org.thingsboard.server.actors.shared.plugin;

import akka.japi.Creator;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.plugin.PluginActor;
import org.thingsboard.server.actors.shared.EntityActorsManager;
import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.plugin.PluginMetaData;
import org.thingsboard.server.dao.plugin.PluginService;

@Slf4j
public abstract class PluginManager extends EntityActorsManager<PluginId, PluginActor, PluginMetaData> {

    protected final PluginService pluginService;

    public PluginManager(ActorSystemContext systemContext) {
        super(systemContext);
        this.pluginService = systemContext.getPluginService();
    }

    @Override
    public Creator<PluginActor> creator(PluginId entityId){
        return new PluginActor.ActorCreator(systemContext, getTenantId(), entityId);
    }

}
