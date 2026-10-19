// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.environment;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.concurrent.locks.ReentrantLock;

@Service
@ConditionalOnProperty(prefix = "zk", value = "enabled", havingValue = "false", matchIfMissing = true)
public class DummyDistributedLockService implements DistributedLockService {

    @Override
    public DistributedLock getLock(String key) {
        return new DummyDistributedLock();
    }

    @RequiredArgsConstructor
    private static class DummyDistributedLock implements DistributedLock {

        private final ReentrantLock lock = new ReentrantLock();

        @SneakyThrows
        @Override
        public void lock() {
            lock.lock();
        }

        @SneakyThrows
        @Override
        public void unlock() {
            lock.unlock();
        }

    }

}
