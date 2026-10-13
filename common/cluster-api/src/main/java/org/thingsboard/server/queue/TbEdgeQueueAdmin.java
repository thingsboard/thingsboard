// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue;

public interface TbEdgeQueueAdmin extends TbQueueAdmin {

    void syncEdgeNotificationsOffsets(String fatGroupId, String newGroupId);

}
