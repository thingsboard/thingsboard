/**
 * Copyright © 2016-2019 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.service.queue;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class TbMsgQueueState {
    private final TbMsg msg;
    private final TenantId tenantId;
    private AtomicInteger retryAttempt;
    private AtomicBoolean ack;

    public TbMsgQueueState(TbMsg msg, TenantId tenantId, AtomicInteger retryAttempt, AtomicBoolean ack) {
        this.msg = msg;
        this.tenantId = tenantId;
        this.retryAttempt = retryAttempt;
        this.ack = ack;
    }

    public void ack() {
        ack.set(true);
    }

    public void incrementRetryAttempt() {
        retryAttempt.incrementAndGet();
    }
}
