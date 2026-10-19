// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.script;

import com.google.protobuf.util.JsonFormat;
import org.thingsboard.server.gen.js.JsInvokeProtos;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.kafka.TbKafkaDecoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Created by ashvayka on 25.09.18.
 */
public class RemoteJsResponseDecoder implements TbKafkaDecoder<TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse>> {

    @Override
    public TbProtoQueueMsg<JsInvokeProtos.RemoteJsResponse> decode(TbQueueMsg msg) throws IOException {
        JsInvokeProtos.RemoteJsResponse.Builder builder = JsInvokeProtos.RemoteJsResponse.newBuilder();
        JsonFormat.parser().ignoringUnknownFields().merge(new String(msg.getData(), StandardCharsets.UTF_8), builder);
        return new TbProtoQueueMsg<>(msg.getKey(), builder.build(), msg.getHeaders());
    }
}
