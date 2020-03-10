package org.thingsboard.server;

public interface TbQueueMsgHeaders {

    byte[] put(String key, byte[] value);

    byte[] get(String key);
}
