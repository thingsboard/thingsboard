// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.environment;

public interface DistributedLockService {

    DistributedLock getLock(String key);

}
