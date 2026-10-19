// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.external;

import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;

public abstract class TbAbstractExternalNode implements TbNode {

    protected boolean forceAck;

    public void init(TbContext ctx) {
        this.forceAck = ctx.isExternalNodeForceAck();
    }

    protected void tellSuccess(TbContext ctx, TbMsg tbMsg) {
        if (forceAck) {
            ctx.enqueueForTellNext(tbMsg.copyWithNewCtx().build(), TbNodeConnectionType.SUCCESS);
        } else {
            ctx.tellSuccess(tbMsg);
        }
    }

    protected void tellFailure(TbContext ctx, TbMsg tbMsg, Throwable t) {
        if (forceAck) {
            if (t == null) {
                ctx.enqueueForTellNext(tbMsg.copyWithNewCtx().build(), TbNodeConnectionType.FAILURE);
            } else {
                ctx.enqueueForTellFailure(tbMsg.copyWithNewCtx().build(), t);
            }
        } else {
            if (t == null) {
                ctx.tellNext(tbMsg, TbNodeConnectionType.FAILURE);
            } else {
                ctx.tellFailure(tbMsg, t);
            }
        }
    }

    protected TbMsg ackIfNeeded(TbContext ctx, TbMsg msg) {
        if (forceAck) {
            ctx.ack(msg);
            return msg.copyWithNewCtx().build();
        } else {
            return msg;
        }
    }

}
