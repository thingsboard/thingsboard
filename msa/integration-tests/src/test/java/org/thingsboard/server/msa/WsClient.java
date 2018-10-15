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
package org.thingsboard.server.msa;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class WsClient extends WebSocketClient {
    private final BlockingQueue<String> events;
    private String message;

    public WsClient(URI serverUri) {
        super(serverUri);
        events = new ArrayBlockingQueue<>(100);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
    }

    @Override
    public void onMessage(String message) {
        events.add(message);
        this.message = message;
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        events.clear();
    }

    @Override
    public void onError(Exception ex) {
        ex.printStackTrace();
    }

    public String getLastMessage() {
        return this.message;
    }
}