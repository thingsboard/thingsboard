// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import org.springframework.context.ApplicationListener;
import org.thingsboard.server.queue.discovery.event.PartitionChangeEvent;

public interface ClusterIntegrationService extends ApplicationListener<PartitionChangeEvent> {

}
