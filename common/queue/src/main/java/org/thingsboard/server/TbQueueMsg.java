package org.thingsboard.server;

import java.util.Map;
import java.util.UUID;

public interface TbQueueMsg {

    UUID getKey();

    Map<String, byte[]> getHeaders();

    byte[] getData();
}
