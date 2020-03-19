package org.thingsboard.server;

import com.google.protobuf.InvalidProtocolBufferException;

public interface TbQueueMsgDecoder<T extends TbQueueMsg> {

    T decode(TbQueueMsg msg) throws InvalidProtocolBufferException;
}
