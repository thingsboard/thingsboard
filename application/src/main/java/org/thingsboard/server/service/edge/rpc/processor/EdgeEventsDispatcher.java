// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.processor;

import com.google.common.util.concurrent.ListenableFuture;

public interface EdgeEventsDispatcher {

    ListenableFuture<Boolean> processNewEvents() throws Exception;
}
