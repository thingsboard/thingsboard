/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.actors.shared;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.ContextAwareActor;
import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.PageDataIterable;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ashvayka on 15.03.18.
 */
@Slf4j
public abstract class EntityActorsManager<T extends EntityId, A extends UntypedActor, M extends SearchTextBased<? extends UUIDBased>> {

    protected final ActorSystemContext systemContext;
    protected final Map<T, ActorRef> actors;

    public EntityActorsManager(ActorSystemContext systemContext) {
        this.systemContext = systemContext;
        this.actors = new HashMap<>();
    }

    protected abstract TenantId getTenantId();

    protected abstract String getDispatcherName();

    protected abstract Creator<A> creator(T entityId);

    protected abstract PageDataIterable.FetchFunction<M> getFetchEntitiesFunction();

    public void init(ActorContext context) {
        for (M entity : new PageDataIterable<>(getFetchEntitiesFunction(), ContextAwareActor.ENTITY_PACK_LIMIT)) {
            T entityId = (T) entity.getId();
            log.debug("[{}|{}] Creating entity actor", entityId.getEntityType(), entityId.getId());
            //TODO: remove this cast making UUIDBased subclass of EntityId an interface and vice versa.
            ActorRef actorRef = getOrCreateActor(context, entityId);
            visit(entity, actorRef);
            log.debug("[{}|{}] Entity actor created.", entityId.getEntityType(), entityId.getId());
        }
    }

    public void visit(M entity, ActorRef actorRef) {}

    public ActorRef getOrCreateActor(ActorContext context, T entityId) {
        return actors.computeIfAbsent(entityId, eId ->
                context.actorOf(Props.create(creator(eId))
                        .withDispatcher(getDispatcherName()), eId.toString()));
    }

    public void broadcast(Object msg) {
        actors.values().forEach(actorRef -> actorRef.tell(msg, ActorRef.noSender()));
    }

    public void remove(T id) {
        actors.remove(id);
    }

}
