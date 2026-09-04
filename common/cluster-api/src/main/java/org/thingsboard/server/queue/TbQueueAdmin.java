// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue;

public interface TbQueueAdmin {

    default void createTopicIfNotExists(String topic) {
        createTopicIfNotExists(topic, null, false);
    }

    void createTopicIfNotExists(String topic, String properties, boolean force);

    void destroy();

    void deleteTopic(String topic);

}
