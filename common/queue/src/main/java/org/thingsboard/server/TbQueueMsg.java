package org.thingsboard.server;

import java.util.UUID;

public interface TbQueueMsg {

    UUID getKey();

    TbQueueMsgHeaders getHeaders();

    byte[] getData();
}
