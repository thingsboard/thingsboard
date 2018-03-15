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
import org.thingsboard.server.common.data.SearchTextBasedWithAdditionalInfo;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.PageDataIterable;
import org.thingsboard.server.common.data.plugin.PluginMetaData;

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
            log.debug("[{}] Creating plugin actor", entity.getId());
            //TODO: remove this cast making UUIDBased subclass of EntityId an interface and vice versa.
            getOrCreateActor(context, (T) entity.getId());
            log.debug("[{}] Plugin actor created.", entity.getId());
        }
    }


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
