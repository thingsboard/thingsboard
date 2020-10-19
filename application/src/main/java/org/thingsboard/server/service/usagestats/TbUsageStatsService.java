package org.thingsboard.server.service.usagestats;

import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

public interface TbUsageStatsService {

    void process(TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg> msg, TbCallback callback);
}
