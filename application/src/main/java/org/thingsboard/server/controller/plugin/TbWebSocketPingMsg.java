// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.controller.plugin;

import lombok.RequiredArgsConstructor;

import java.nio.ByteBuffer;

@RequiredArgsConstructor
public class TbWebSocketPingMsg implements TbWebSocketMsg<ByteBuffer> {

    public static TbWebSocketPingMsg INSTANCE = new TbWebSocketPingMsg();

    private static final ByteBuffer PING_MSG = ByteBuffer.wrap(new byte[]{});

    @Override
    public TbWebSocketMsgType getType() {
        return TbWebSocketMsgType.PING;
    }

    @Override
    public ByteBuffer getMsg() {
        return PING_MSG;
    }
}
