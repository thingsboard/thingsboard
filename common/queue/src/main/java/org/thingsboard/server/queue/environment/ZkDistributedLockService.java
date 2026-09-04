// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
