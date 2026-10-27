// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.stats.MessagesStats;

public interface TbQueueRequestTemplate<Request extends TbQueueMsg, Response extends TbQueueMsg> {

    void init();

    ListenableFuture<Response> send(Request request);

    ListenableFuture<Response> send(Request request, long timeoutNs);

    ListenableFuture<Response> send(Request request, Integer partition);

    void stop();

    void setMessagesStats(MessagesStats messagesStats);
}
