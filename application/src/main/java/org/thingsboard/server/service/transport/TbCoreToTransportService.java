package org.thingsboard.server.service.transport;

import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.function.Consumer;

public interface TbCoreToTransportService {

    void process(String nodeId, TransportProtos.DeviceActorToTransportMsg msg);

    void process(String nodeId, TransportProtos.DeviceActorToTransportMsg msg, Runnable onSuccess, Consumer<Throwable> onFailure);

}
