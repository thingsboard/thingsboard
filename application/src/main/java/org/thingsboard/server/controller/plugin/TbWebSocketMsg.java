// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.controller.plugin;

public interface TbWebSocketMsg<T> {

    TbWebSocketMsgType getType();

    T getMsg();

}
