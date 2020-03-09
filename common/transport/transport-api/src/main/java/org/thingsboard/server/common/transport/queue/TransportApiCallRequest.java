package org.thingsboard.server.common.transport.queue;

import org.thingsboard.server.TbQueueMsg;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TransportApiCallRequest extends TransportApiCall implements TbQueueMsg {
    public static final String REQUEST_ID_HEADER = "requestId";
    public static final String RESPONSE_TOPIC_HEADER = "responseTopic";

    private final UUID requestId;
    private final Map<String, byte[]> headers;
    private final TransportProtos.TransportApiRequestMsg msg;

    public TransportApiCallRequest(UUID requestId, String responseTopic, TransportProtos.TransportApiRequestMsg msg) {
        this.requestId = requestId;
        this.headers = new HashMap<>();
        this.headers.put(REQUEST_ID_HEADER, uuidToBytes(requestId));
        this.headers.put(RESPONSE_TOPIC_HEADER, stringToBytes(responseTopic));
        this.msg = msg;
    }

    @Override
    public UUID getKey() {
        return requestId;
    }

    @Override
    public Map<String, byte[]> getHeaders() {
        return null;
    }

    @Override
    public byte[] getData() {
        return msg.toByteArray();
    }
}
