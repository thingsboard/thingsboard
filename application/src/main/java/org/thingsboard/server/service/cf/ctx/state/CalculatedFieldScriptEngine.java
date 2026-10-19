// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.ListenableFuture;

public interface CalculatedFieldScriptEngine {

    ListenableFuture<Object> executeScriptAsync(Object[] args);

    ListenableFuture<JsonNode> executeJsonAsync(Object[] args);

    void destroy();

}
