package org.thingsboard.server.service.queue.processing;

import lombok.Getter;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

public class TbRuleEngineProcessingResult {

    @Getter
    private boolean success;
    @Getter
    private boolean timeout;
    @Getter
    private ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> pendingMap;
    @Getter
    private ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> successMap;
    @Getter
    private ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> failureMap;

    public TbRuleEngineProcessingResult(boolean timeout,
                                        ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> pendingMap,
                                        ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> successMap,
                                        ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> failureMap) {
        this.timeout = timeout;
        this.pendingMap = pendingMap;
        this.successMap = successMap;
        this.failureMap = failureMap;
        this.success = !timeout && pendingMap.isEmpty() && failureMap.isEmpty();
    }
}
