package org.thingsboard.server;

import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.channels.NotYetConnectedException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WebSocketClientImpl extends WebSocketClient {

    private volatile String lastMsg;
    private CountDownLatch reply;
    private CountDownLatch update;

    public WebSocketClientImpl(URI serverUri) {
        super(serverUri);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {

    }

    @Override
    public void onMessage(String s) {
        log.debug("RECEIVED: {}", s);
        lastMsg = s;
        if (reply != null) {
            reply.countDown();
        }
        if (update != null) {
            update.countDown();
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        log.debug("CLOSED:");
    }

    @Override
    public void onError(Exception e) {
        log.error("ERROR:", e);
    }

    public void registerWaitForUpdate() {
        lastMsg = null;
        update = new CountDownLatch(1);
    }

    @Override
    public void send(String text) throws NotYetConnectedException {
        reply = new CountDownLatch(1);
        super.send(text);
    }

    public String waitForUpdate(long ms) {
        try {
            update.await(ms, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.debug("Failed to await reply", e);
        }
        return lastMsg;
    }

    public String waitForReply(int ms) {
        try {
            reply.await(ms, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.debug("Failed to await reply", e);
        }
        return lastMsg;
    }
}