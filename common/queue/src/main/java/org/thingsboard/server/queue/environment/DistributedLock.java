// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.environment;

public interface DistributedLock {

    void lock();

    void unlock();

}
