package org.thingsboard.server.common;

import lombok.Data;
import org.thingsboard.server.TbQueueMsg;
import org.thingsboard.server.TbQueueMsgHeaders;

import java.util.UUID;

@Data
public class TbProtoQueueMsg<T extends com.google.protobuf.GeneratedMessageV3> implements TbQueueMsg {

    private final UUID key;
    private final T value;
    private final DefaultTbQueueMsgHeaders headers;

    public TbProtoQueueMsg(UUID key, T value) {
        this(key, value, new DefaultTbQueueMsgHeaders());
    }

    public TbProtoQueueMsg(UUID key, T value, DefaultTbQueueMsgHeaders headers) {
        this.key = key;
        this.value = value;
        this.headers = headers;
    }

    @Override
    public UUID getKey() {
        return key;
    }

    @Override
    public TbQueueMsgHeaders getHeaders() {
        return headers;
    }

    @Override
    public byte[] getData() {
        return value.toByteArray();
    }
}
