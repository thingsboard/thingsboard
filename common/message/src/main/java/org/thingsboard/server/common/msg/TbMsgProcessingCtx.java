// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg;

import lombok.Getter;
import lombok.Setter;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.gen.MsgProtos;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 13.01.18.
 */
public final class TbMsgProcessingCtx implements Serializable {

    private final AtomicInteger ruleNodeExecCounter;
    private volatile LinkedList<TbMsgProcessingStackItem> stack;
    @Setter
    @Getter
    private volatile boolean traceAuthorized;

    public TbMsgProcessingCtx() {
        this(0);
    }

    public TbMsgProcessingCtx(int ruleNodeExecCounter) {
        this(ruleNodeExecCounter, null, false);
    }

    protected TbMsgProcessingCtx(int ruleNodeExecCounter, LinkedList<TbMsgProcessingStackItem> stack, boolean traceAuthorized) {
        this.ruleNodeExecCounter = new AtomicInteger(ruleNodeExecCounter);
        this.stack = stack;
        this.traceAuthorized = traceAuthorized;
    }

    public int getAndIncrementRuleNodeCounter() {
        return ruleNodeExecCounter.getAndIncrement();
    }

    public TbMsgProcessingCtx copy() {
        if (stack == null || stack.isEmpty()) {
            return new TbMsgProcessingCtx(ruleNodeExecCounter.get(), null, traceAuthorized);
        } else {
            return new TbMsgProcessingCtx(ruleNodeExecCounter.get(), new LinkedList<>(stack), traceAuthorized);
        }
    }

    public void push(RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        if (stack == null) {
            stack = new LinkedList<>();
        }
        stack.add(new TbMsgProcessingStackItem(ruleChainId, ruleNodeId));
    }

    public boolean isAlreadyInStack(RuleChainId ruleChainId, RuleNodeId ruleNodeId) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        for (TbMsgProcessingStackItem item : stack) {
            if (ruleChainId.equals(item.getRuleChainId()) && ruleNodeId.equals(item.getRuleNodeId())) {
                return true;
            }
        }
        return false;
    }

    public TbMsgProcessingStackItem pop() {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.removeLast();
    }

    public static TbMsgProcessingCtx fromProto(MsgProtos.TbMsgProcessingCtxProto ctx) {
        int ruleNodeExecCounter = ctx.getRuleNodeExecCounter();
        boolean traceAuthorized = ctx.getTraceAuthorized();
        if (ctx.getStackCount() > 0) {
            LinkedList<TbMsgProcessingStackItem> stack = new LinkedList<>();
            for (MsgProtos.TbMsgProcessingStackItemProto item : ctx.getStackList()) {
                stack.add(TbMsgProcessingStackItem.fromProto(item));
            }
            return new TbMsgProcessingCtx(ruleNodeExecCounter, stack, traceAuthorized);
        } else {
            return new TbMsgProcessingCtx(ruleNodeExecCounter, null, traceAuthorized);
        }
    }

    public MsgProtos.TbMsgProcessingCtxProto toProto() {
        var ctxBuilder = MsgProtos.TbMsgProcessingCtxProto.newBuilder();
        ctxBuilder.setRuleNodeExecCounter(ruleNodeExecCounter.get());
        ctxBuilder.setTraceAuthorized(traceAuthorized);
        if (stack != null) {
            for (TbMsgProcessingStackItem item : stack) {
                ctxBuilder.addStack(item.toProto());
            }
        }
        return ctxBuilder.build();
    }
}
