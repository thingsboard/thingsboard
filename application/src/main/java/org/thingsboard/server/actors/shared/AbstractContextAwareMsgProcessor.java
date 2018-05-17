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
import akka.actor.Scheduler;
import akka.event.LoggingAdapter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.actors.ActorSystemContext;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public abstract class AbstractContextAwareMsgProcessor {

    protected final ActorSystemContext systemContext;
    protected final LoggingAdapter logger;
    protected final ObjectMapper mapper = new ObjectMapper();

    protected AbstractContextAwareMsgProcessor(ActorSystemContext systemContext, LoggingAdapter logger) {
        super();
        this.systemContext = systemContext;
        this.logger = logger;
    }

    protected ActorRef getAppActor() {
        return systemContext.getAppActor();
    }

    protected Scheduler getScheduler() {
        return systemContext.getScheduler();
    }

    protected ExecutionContextExecutor getSystemDispatcher() {
        return systemContext.getActorSystem().dispatcher();
    }

    protected void schedulePeriodicMsgWithDelay(ActorContext ctx, Object msg, long delayInMs, long periodInMs) {
        schedulePeriodicMsgWithDelay(ctx, msg, delayInMs, periodInMs, ctx.self());
    }

    protected void schedulePeriodicMsgWithDelay(ActorContext ctx, Object msg, long delayInMs, long periodInMs, ActorRef target) {
        logger.debug("Scheduling periodic msg {} every {} ms with delay {} ms", msg, periodInMs, delayInMs);
        getScheduler().schedule(Duration.create(delayInMs, TimeUnit.MILLISECONDS), Duration.create(periodInMs, TimeUnit.MILLISECONDS), target, msg, getSystemDispatcher(), null);
    }


    protected void scheduleMsgWithDelay(ActorContext ctx, Object msg, long delayInMs) {
        scheduleMsgWithDelay(ctx, msg, delayInMs, ctx.self());
    }

    protected void scheduleMsgWithDelay(ActorContext ctx, Object msg, long delayInMs, ActorRef target) {
        logger.debug("Scheduling msg {} with delay {} ms", msg, delayInMs);
        getScheduler().scheduleOnce(Duration.create(delayInMs, TimeUnit.MILLISECONDS), target, msg, getSystemDispatcher(), null);
    }

    @Data
    @AllArgsConstructor
    private static class ComponentConfiguration {
        private final String clazz;
        private final String name;
        private final String configuration;
    }

}
