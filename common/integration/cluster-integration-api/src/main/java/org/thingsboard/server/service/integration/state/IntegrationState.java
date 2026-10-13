// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.state;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.integration.api.ThingsboardPlatformIntegration;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Data
@RequiredArgsConstructor
public class IntegrationState {

    private final Lock updateLock = new ReentrantLock();
    private final Queue<ComponentLifecycleEvent> updateQueue = new ConcurrentLinkedQueue<>();
    private final TenantId tenantId;
    private final IntegrationId id;

    private volatile ComponentLifecycleEvent currentState;
    private volatile ThingsboardPlatformIntegration<?> integration;
    private Integration configuration;

}
