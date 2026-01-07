/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.queue.common;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.util.JsonFormat;
import org.thingsboard.server.queue.TbQueueMsgHeaders;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class TbProtoJsQueueMsg<T extends com.google.protobuf.GeneratedMessageV3> extends TbProtoQueueMsg<T> {

    public TbProtoJsQueueMsg(UUID key, T value) {
        super(key, value);
    }

    public TbProtoJsQueueMsg(UUID key, T value, TbQueueMsgHeaders headers) {
        super(key, value, headers);
    }

    @Override
    public byte[] getData() {
        try {
            return JsonFormat.printer().print(value).getBytes(StandardCharsets.UTF_8);
        } catch (InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }
    }
}
