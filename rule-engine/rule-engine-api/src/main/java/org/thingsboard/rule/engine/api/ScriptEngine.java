// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.Set;

public interface ScriptEngine {

    ListenableFuture<List<TbMsg>> executeUpdateAsync(TbMsg msg);

    ListenableFuture<TbMsg> executeGenerateAsync(TbMsg prevMsg);

    ListenableFuture<Boolean> executeFilterAsync(TbMsg msg);

    ListenableFuture<Set<String>> executeSwitchAsync(TbMsg msg);

    ListenableFuture<JsonNode> executeJsonAsync(TbMsg msg);

    ListenableFuture<String> executeToStringAsync(TbMsg msg);

    void destroy();

}
