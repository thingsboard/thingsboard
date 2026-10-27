// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue;

import com.google.common.util.concurrent.ListenableFuture;

public interface TbQueueHandler<Request extends TbQueueMsg, Response extends TbQueueMsg> {

    ListenableFuture<Response> handle(Request request);

    default Response constructErrorResponseMsg(Request request, Throwable cause) {
        return null;
    }

}
