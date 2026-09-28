// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.rule.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ScriptEngine {

    ListenableFuture<List<TbMsg>> executeUpdateAsync(TbMsg msg);

    ListenableFuture<TbMsg> executeGenerateAsync(TbMsg prevMsg);

    ListenableFuture<Boolean> executeAttributesFilterAsync(Map<String, KvEntry> attributes);

    ListenableFuture<Boolean> executeFilterAsync(TbMsg msg);

    ListenableFuture<Set<String>> executeSwitchAsync(TbMsg msg);

    ListenableFuture<JsonNode> executeJsonAsync(TbMsg msg);

    ListenableFuture<String> executeToStringAsync(TbMsg msg);

    void destroy();

}
