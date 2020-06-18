/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TbTestWebSocketClient extends WebSocketClient {

    private volatile String lastMsg;
    private volatile boolean replyReceived;
    private CountDownLatch reply;

    public TbTestWebSocketClient(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {

    }

    @Override
    public void onMessage(String s) {
        if (!replyReceived) {
            replyReceived = true;
            lastMsg = s;
            if (reply != null) {
                reply.countDown();
            }
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {

    }

    @Override
    public void onError(Exception e) {

    }

    @Override
    public void send(String text) throws NotYetConnectedException {
        reply = new CountDownLatch(1);
        replyReceived = false;
        super.send(text);
    }

    public String waitForReply() {
        try {
            reply.await(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Failed to await reply", e);
        }
        return lastMsg;
    }
}
