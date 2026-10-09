// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.rule.engine.transform;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public abstract class TbAbstractDuplicateMsgNode<C> extends TbAbstractTransformNode<C> {

    protected ListenableFuture<List<TbMsg>> duplicate(TbContext ctx, TbMsg msg) {
        ListenableFuture<List<EntityId>> newOriginatorsFuture = getNewOriginators(ctx, msg);
        return Futures.transform(newOriginatorsFuture, newOriginators -> {
            if (newOriginators == null || newOriginators.isEmpty()) {
                return Collections.emptyList();
            }
            if (newOriginators.size() == 1) {
                return Collections.singletonList(ctx.transformMsgOriginator(msg, newOriginators.get(0)));
            } else {
                return newOriginators.stream()
                        .map(newOriginator -> duplicateMsgForOriginator(msg, newOriginator))
                        .toList();
            }
        }, ctx.getDbCallbackExecutor());
    }

    private static TbMsg duplicateMsgForOriginator(TbMsg originalMsg, EntityId newOriginator) {
        return originalMsg.transform()
                .id(UUID.randomUUID()) // effectively creating a new message
                .originator(newOriginator)
                .callback(TbMsgCallback.EMPTY)
                .build();
    }

    protected abstract ListenableFuture<List<EntityId>> getNewOriginators(TbContext ctx, TbMsg msg);

}
