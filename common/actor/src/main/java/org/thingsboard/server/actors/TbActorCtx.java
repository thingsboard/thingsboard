// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.msg.TbActorMsg;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

public interface TbActorCtx extends TbActorRef {

    TbActorId getSelf();

    TbActorRef getParentRef();

    void tell(TbActorId target, TbActorMsg msg);

    void stop(TbActorId target);

    TbActorRef getOrCreateChildActor(TbActorId actorId, Supplier<String> dispatcher, Supplier<TbActorCreator> creator, Supplier<Boolean> createCondition);

    void broadcastToChildren(TbActorMsg msg);

    void broadcastToChildren(TbActorMsg msg, boolean highPriority);

    void broadcastToChildrenByType(TbActorMsg msg, EntityType entityType);

    void broadcastToChildren(TbActorMsg msg, Predicate<TbActorId> childFilter);

    List<TbActorId> filterChildren(Predicate<TbActorId> childFilter);
}
