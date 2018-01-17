package org.thingsboard.server.actors.shared.application;

import akka.actor.ActorContext;
import akka.actor.ActorRef;
import lombok.extern.slf4j.Slf4j;
import akka.actor.Props;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.application.ApplicationActor;
import org.thingsboard.server.common.data.id.TenantId;

@Slf4j
public abstract class ApplicationManager {
    protected final ActorSystemContext systemContext;

    public ApplicationManager(ActorSystemContext systemContext) {
        this.systemContext = systemContext;
    }

    abstract TenantId getTenantId();

    public ActorRef getOrCreateApplicationActor(ActorContext context){
        return context.actorOf(Props.create(new ApplicationActor.ActorCreator(systemContext, getTenantId())));
    }
}
