package org.thingsboard.server.service.queue.processing;

import lombok.Data;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

@Data
public class TbRuleEngineProcessingDecision {

    private final boolean commit;
    private final ConcurrentMap<UUID, TbProtoQueueMsg<ToRuleEngineMsg>> reprocessMap;

}
