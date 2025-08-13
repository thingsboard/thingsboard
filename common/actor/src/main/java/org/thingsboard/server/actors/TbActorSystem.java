/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
