// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
