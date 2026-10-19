// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors;

import org.thingsboard.server.common.msg.TbActorMsg;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Predicate;

public interface TbActorSystem {

    ScheduledExecutorService getScheduler();

    void createDispatcher(String dispatcherId, ExecutorService executor);

    void destroyDispatcher(String dispatcherId);

    TbActorRef getActor(TbActorId actorId);

    TbActorRef createRootActor(String dispatcherId, TbActorCreator creator);

    TbActorRef createChildActor(String dispatcherId, TbActorCreator creator, TbActorId parent);

    void tell(TbActorId target, TbActorMsg actorMsg);

    void tellWithHighPriority(TbActorId target, TbActorMsg actorMsg);

    void stop(TbActorRef actorRef);

    void stop(TbActorId actorId);

    void stop();

    void broadcastToChildren(TbActorId parent, TbActorMsg msg);

    void broadcastToChildren(TbActorId parent, TbActorMsg msg, boolean highPriority);

    void broadcastToChildren(TbActorId parent, Predicate<TbActorId> childFilter, TbActorMsg msg);

    List<TbActorId> filterChildren(TbActorId parent, Predicate<TbActorId> childFilter);
}
