/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.queue.edqs;

import org.thingsboard.server.gen.transport.TransportProtos.FromEdqsMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToEdqsMsg;
import org.thingsboard.server.queue.TbQueueAdmin;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueResponseTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

public interface EdqsQueueFactory {

    TbQueueConsumer<TbProtoQueueMsg<ToEdqsMsg>> createEdqsEventsConsumer();

    TbQueueConsumer<TbProtoQueueMsg<ToEdqsMsg>> createEdqsEventsToBackupConsumer();

    TbQueueConsumer<TbProtoQueueMsg<ToEdqsMsg>> createEdqsStateConsumer();

    TbQueueProducer<TbProtoQueueMsg<ToEdqsMsg>> createEdqsStateProducer();

    TbQueueResponseTemplate<TbProtoQueueMsg<ToEdqsMsg>, TbProtoQueueMsg<FromEdqsMsg>> createEdqsResponseTemplate();

    TbQueueAdmin getEdqsQueueAdmin();

}
