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
package org.thingsboard.server.queue.environment;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.thingsboard.server.queue.discovery.ZkDiscoveryService;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "true")
@Slf4j
public class ZkDistributedLockService implements DistributedLockService {

    private final ZkDiscoveryService zkDiscoveryService;

    @Override
    public DistributedLock getLock(String key) {
        return new ZkDistributedLock(key);
    }

    @RequiredArgsConstructor
    private class ZkDistributedLock implements DistributedLock {

        private final InterProcessLock interProcessLock;

        public ZkDistributedLock(String key) {
            this.interProcessLock = new InterProcessMutex(zkDiscoveryService.getClient(), zkDiscoveryService.getZkDir() + "/locks/" + key);
        }

        @SneakyThrows
        @Override
        public void lock() {
            interProcessLock.acquire();
        }

        @SneakyThrows
        @Override
        public void unlock() {
            interProcessLock.release();
        }
    }

}
