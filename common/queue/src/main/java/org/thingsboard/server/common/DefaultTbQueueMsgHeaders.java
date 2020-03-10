package org.thingsboard.server.common;

import org.thingsboard.server.TbQueueMsgHeaders;

import java.util.HashMap;
import java.util.Map;

public class DefaultTbQueueMsgHeaders implements TbQueueMsgHeaders {

    private final Map<String, byte[]> data = new HashMap<>();

    @Override
    public byte[] put(String key, byte[] value) {
        return data.put(key, value);
    }

    @Override
    public byte[] get(String key) {
        return data.get(key);
    }
}
