// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.discovery;

import java.util.List;

public interface QueueRoutingInfoService {

    List<QueueRoutingInfo> getAllQueuesRoutingInfo();

}
