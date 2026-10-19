// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.queue.processing;

import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public abstract class AbstractTbRuleEngineSubmitStrategy implements TbRuleEngineSubmitStrategy {

    protected final String queueName;
    protected List<IdMsgPair<TransportProtos.ToRuleEngineMsg>> orderedMsgList;
    private volatile boolean stopped;

    public AbstractTbRuleEngineSubmitStrategy(String queueName) {
        this.queueName = queueName;
    }

    protected abstract void doOnSuccess(UUID id);

    @Override
    public void init(List<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> msgs) {
        orderedMsgList = msgs.stream().map(msg -> new IdMsgPair<>(UUID.randomUUID(), msg)).collect(Collectors.toList());
    }

    @Override
    public ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> getPendingMap() {
        return orderedMsgList.stream().collect(Collectors.toConcurrentMap(pair -> pair.uuid, pair -> pair.msg));
    }

    @Override
    public void update(ConcurrentMap<UUID, TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> reprocessMap) {
        List<IdMsgPair<TransportProtos.ToRuleEngineMsg>> newOrderedMsgList = new ArrayList<>(reprocessMap.size());
        for (IdMsgPair<TransportProtos.ToRuleEngineMsg> pair : orderedMsgList) {
            if (reprocessMap.containsKey(pair.uuid)) {
                if (StringUtils.isNotEmpty(pair.getMsg().getValue().getFailureMessage())) {
                    var toRuleEngineMsg = TransportProtos.ToRuleEngineMsg.newBuilder(pair.getMsg().getValue())
                            .clearFailureMessage()
                            .clearRelationTypes()
                            .build();
                    var newMsg = new TbProtoQueueMsg<>(pair.getMsg().getKey(), toRuleEngineMsg, pair.getMsg().getHeaders());
                    newOrderedMsgList.add(new IdMsgPair<>(pair.getUuid(), newMsg));
                } else {
                    newOrderedMsgList.add(pair);
                }
            }
        }
        orderedMsgList = newOrderedMsgList;
    }

    @Override
    public void onSuccess(UUID id) {
        if (!stopped) {
            doOnSuccess(id);
        }
    }

    @Override
    public void stop() {
        stopped = true;
    }
}
