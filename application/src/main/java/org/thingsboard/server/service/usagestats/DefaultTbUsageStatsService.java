package org.thingsboard.server.service.usagestats;

import org.springframework.stereotype.Service;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

@Service
public class DefaultTbUsageStatsService implements TbUsageStatsService {
    @Override
    public void process(TbProtoQueueMsg<TransportProtos.ToUsageStatsServiceMsg> msg, TbCallback callback) {
        
    }

}
