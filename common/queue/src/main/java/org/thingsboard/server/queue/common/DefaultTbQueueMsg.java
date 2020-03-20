package org.thingsboard.server.queue.common;

import com.google.gson.annotations.Expose;
import lombok.Data;
import org.thingsboard.server.queue.TbQueueMsg;
import org.thingsboard.server.queue.TbQueueMsgHeaders;

import java.util.UUID;

@Data
public class DefaultTbQueueMsg implements TbQueueMsg {
    private final UUID key;
    private final byte[] data;

    public DefaultTbQueueMsg(UUID key, byte[] data) {
        this.key = key;
        this.data = data;
    }

    @Expose(serialize = false, deserialize = false)
    private TbQueueMsgHeaders headers;
}
