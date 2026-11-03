// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue;

import java.util.UUID;

public interface TbQueueMsg {

    UUID getKey();

    TbQueueMsgHeaders getHeaders();

    byte[] getData();
}
