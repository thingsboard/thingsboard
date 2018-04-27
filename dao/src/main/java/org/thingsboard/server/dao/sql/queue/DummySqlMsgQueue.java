/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.queue;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.queue.MsgQueue;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Collections;
import java.util.UUID;

/**
 * Created by ashvayka on 27.04.18.
 */
@Component
@Slf4j
@SqlDao
public class DummySqlMsgQueue implements MsgQueue {
    @Override
    public ListenableFuture<Void> put(TbMsg msg, UUID nodeId, long clusterPartition) {
        return Futures.immediateFuture(null);
    }

    @Override
    public ListenableFuture<Void> ack(TbMsg msg, UUID nodeId, long clusterPartition) {
        return Futures.immediateFuture(null);
    }

    @Override
    public Iterable<TbMsg> findUnprocessed(UUID nodeId, long clusterPartition) {
        return Collections.emptyList();
    }
}
