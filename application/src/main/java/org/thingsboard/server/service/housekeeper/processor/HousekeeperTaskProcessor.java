// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.housekeeper.processor;

import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.msg.housekeeper.HousekeeperClient;

import java.util.concurrent.Future;

public abstract class HousekeeperTaskProcessor<T extends HousekeeperTask> {

    @Autowired
    protected HousekeeperClient housekeeperClient;

    public abstract void process(T task) throws Exception;

    public abstract HousekeeperTaskType getTaskType();

    public <V> V wait(Future<V> future) throws Exception {
        try {
            return future.get(); // will be interrupted after taskProcessingTimeout
        } catch (InterruptedException e) {
            future.cancel(true); // interrupting the underlying task
            throw e;
        }
    }

}
