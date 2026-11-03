// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.common.consumer;

import java.io.Serializable;

public enum QueueTaskType implements Serializable {

    UPDATE_PARTITIONS, UPDATE_CONFIG, DELETE,
    ADD_PARTITIONS, REMOVE_PARTITIONS, DELETE_PARTITIONS

}
