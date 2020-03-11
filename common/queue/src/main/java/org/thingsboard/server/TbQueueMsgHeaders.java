package org.thingsboard.server;

import java.util.Map;

public interface TbQueueMsgHeaders {

    byte[] put(String key, byte[] value);

    byte[] get(String key);

    Map<String, byte[]> getData();
}
