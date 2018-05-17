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
package org.thingsboard.server.service.cluster.discovery;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

/**
 * @author Andrew Shvayka
 */
@Service
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
@DependsOn("environmentLogService")
public class DummyDiscoveryService implements DiscoveryService {

    @Autowired
    private ServerInstanceService serverInstance;

    @PostConstruct
    public void init() {
        log.info("Initializing...");
    }

    @Override
    public void publishCurrentServer() {
        //Do nothing
    }

    @Override
    public void unpublishCurrentServer() {
        //Do nothing
    }

    @Override
    public ServerInstance getCurrentServer() {
        return serverInstance.getSelf();
    }

    @Override
    public List<ServerInstance> getOtherServers() {
        return Collections.emptyList();
    }

    @Override
    public boolean addListener(DiscoveryServiceListener listener) {
        return false;
    }

    @Override
    public boolean removeListener(DiscoveryServiceListener listener) {
        return false;
    }
}
