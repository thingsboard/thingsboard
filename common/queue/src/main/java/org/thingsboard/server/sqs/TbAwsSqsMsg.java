package org.thingsboard.server.sqs;

import com.google.gson.annotations.Expose;
import lombok.Data;
import org.thingsboard.server.TbQueueMsg;
import org.thingsboard.server.TbQueueMsgHeaders;

import java.util.UUID;

@Data
public class TbAwsSqsMsg implements TbQueueMsg {
    private final UUID key;
    private final byte[] data;

    public TbAwsSqsMsg(UUID key, byte[] data) {
        this.key = key;
        this.data = data;
    }

    @Expose(serialize = false, deserialize = false)
    private TbQueueMsgHeaders headers;

}
