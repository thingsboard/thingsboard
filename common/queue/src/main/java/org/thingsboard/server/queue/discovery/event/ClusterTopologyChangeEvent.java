// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.discovery.event;

import lombok.Getter;
import org.thingsboard.server.queue.discovery.QueueKey;

import java.util.Set;

public class ClusterTopologyChangeEvent extends TbApplicationEvent {

    private static final long serialVersionUID = -2441739930040282254L;

    @Getter
    private final Set<QueueKey> queueKeys;

    public ClusterTopologyChangeEvent(Object source, Set<QueueKey> queueKeys) {
        super(source);
        this.queueKeys = queueKeys;
    }
}
