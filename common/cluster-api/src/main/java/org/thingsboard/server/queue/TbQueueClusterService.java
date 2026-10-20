// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue;

import org.thingsboard.server.common.data.queue.Queue;

import java.util.List;

public interface TbQueueClusterService {

    void onQueuesUpdate(List<Queue> queues);

    void onQueuesDelete(List<Queue> queues);

}
