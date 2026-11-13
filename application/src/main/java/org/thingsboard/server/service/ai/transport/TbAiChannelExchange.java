// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import org.thingsboard.ai.common.channel.ChannelFrame;

/**
 * One chat turn or operation on a pooled channel. Callbacks run on the channel's I/O thread and must not block.
 */
interface TbAiChannelExchange {

    void onFrame(TbAiPooledConnection connection, ChannelFrame frame);

    void onClose(int code, String reason);

}
