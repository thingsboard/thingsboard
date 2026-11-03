// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.memory;

import org.thingsboard.server.queue.TbQueueMsg;

import java.util.List;

public interface InMemoryStorage {

    void printStats();

    int getLagTotal();

    int getLag(String topic);

    boolean put(String topic, TbQueueMsg msg);

    <T extends TbQueueMsg> List<T> get(String topic) throws InterruptedException;

}
