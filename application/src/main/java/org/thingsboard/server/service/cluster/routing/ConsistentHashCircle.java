/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.service.cluster.routing;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.service.cluster.discovery.ServerInstance;

import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Created by ashvayka on 23.09.18.
 */
@Slf4j
public class ConsistentHashCircle {
    private final ConcurrentNavigableMap<Long, ServerInstance> circle =
            new ConcurrentSkipListMap<>();

    public void put(long hash, ServerInstance instance) {
        circle.put(hash, instance);
    }

    public void remove(long hash) {
        circle.remove(hash);
    }

    public boolean isEmpty() {
        return circle.isEmpty();
    }

    public boolean containsKey(Long hash) {
        return circle.containsKey(hash);
    }

    public ConcurrentNavigableMap<Long, ServerInstance> tailMap(Long hash) {
        return circle.tailMap(hash);
    }

    public Long firstKey() {
        return circle.firstKey();
    }

    public ServerInstance get(Long hash) {
        return circle.get(hash);
    }

    public void log() {
        circle.entrySet().forEach((e) -> log.debug("{} -> {}", e.getKey(), e.getValue().getServerAddress()));
    }
}
