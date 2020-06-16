/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.service.script;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.queue.kafka.TbKafkaEncoder;

import java.nio.charset.StandardCharsets;

/**
 * Created by ashvayka on 25.09.18.
 */
public class RemoteJsRequestEncoder implements TbKafkaEncoder<TbProtoQueueMsg<JsInvokeProtos.RemoteJsRequest>> {
    @Override
    public byte[] encode(TbProtoQueueMsg<JsInvokeProtos.RemoteJsRequest> value) {
        try {
            return JsonFormat.printer().print(value.getValue()).getBytes(StandardCharsets.UTF_8);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
