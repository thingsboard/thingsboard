// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.msg.inbound;

import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.AgentId;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Per-agent locks guarding the inbound compose pipeline. Locks are striped over a fixed array rather than kept in a
 * per-agent map: a map keyed by agent id either grows without bound or needs a lifecycle hook to purge entries, and a
 * weak/soft map is unusable here because nothing strongly reaches the entry while a lock is held, so an entry can be
 * collected mid-critical-section and the next caller would receive a brand-new lock. Two agents sharing a stripe only
 * serialize against each other, which is harmless.
 */
@Component
@TbCoreComponent
public class AgentAppAutoInstallLockRegistry {

    private static final int STRIPES = 1024;

    private final ReadWriteLock[] autoInstallLocks = new ReadWriteLock[STRIPES];
    private final Lock[] appCreationLocks = new Lock[STRIPES];

    public AgentAppAutoInstallLockRegistry() {
        for (int i = 0; i < STRIPES; i++) {
            autoInstallLocks[i] = new ReentrantReadWriteLock();
            appCreationLocks[i] = new ReentrantLock();
        }
    }

    public ReadWriteLock forAgent(AgentId agentId) {
        return autoInstallLocks[stripe(agentId)];
    }

    public Lock forAppCreation(AgentId agentId) {
        return appCreationLocks[stripe(agentId)];
    }

    private static int stripe(AgentId agentId) {
        return Math.floorMod(agentId.getId().hashCode(), STRIPES);
    }
}
