// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.controller.plugin;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TbWebSocketTextMsg implements TbWebSocketMsg<String> {

    private final String value;

    @Override
    public TbWebSocketMsgType getType() {
        return TbWebSocketMsgType.TEXT;
    }

    @Override
    public String getMsg() {
        return value;
    }
}
