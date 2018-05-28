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
package org.thingsboard.server.actors.rpc;

import akka.actor.ActorRef;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.gen.cluster.ClusterAPIProtos;
import org.thingsboard.server.service.cluster.rpc.GrpcSession;
import org.thingsboard.server.service.cluster.rpc.GrpcSessionListener;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class BasicRpcSessionListener implements GrpcSessionListener {

    private final ActorService service;
    private final ActorRef manager;
    private final ActorRef self;

    public BasicRpcSessionListener(ActorService service, ActorRef manager, ActorRef self) {
        this.service = service;
        this.manager = manager;
        this.self = self;
    }

    @Override
    public void onConnected(GrpcSession session) {
        log.info("{} session started -> {}", getType(session), session.getRemoteServer());
        if (!session.isClient()) {
            manager.tell(new RpcSessionConnectedMsg(session.getRemoteServer(), session.getSessionId()), self);
        }
    }

    @Override
    public void onDisconnected(GrpcSession session) {
        log.info("{} session closed -> {}", getType(session), session.getRemoteServer());
        manager.tell(new RpcSessionDisconnectedMsg(session.isClient(), session.getRemoteServer()), self);
    }

    @Override
    public void onReceiveClusterGrpcMsg(GrpcSession session, ClusterAPIProtos.ClusterMessage clusterMessage) {
        log.trace("{} Service [{}] received session actor msg {}", getType(session),
                session.getRemoteServer(),
                clusterMessage);
        service.onReceivedMsg(session.getRemoteServer(), clusterMessage);
    }

    @Override
    public void onError(GrpcSession session, Throwable t) {
        log.warn("{} session got error -> {}", getType(session), session.getRemoteServer(), t);
        manager.tell(new RpcSessionClosedMsg(session.isClient(), session.getRemoteServer()), self);
        session.close();
    }

    private static String getType(GrpcSession session) {
        return session.isClient() ? "Client" : "Server";
    }


}
