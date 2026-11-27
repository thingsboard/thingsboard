// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DownlinkCaptureServer implements AutoCloseable {

    public record Request(String query, String body) {}

    private final HttpServer server;
    private final BlockingQueue<Request> requests = new LinkedBlockingQueue<>();

    public DownlinkCaptureServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            requests.add(new Request(exchange.getRequestURI().getRawQuery(), body));
            byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
    }

    public String getUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    public Request takeRequest() throws InterruptedException {
        Request request = requests.poll(10, TimeUnit.SECONDS);
        if (request == null) {
            throw new AssertionError("No downlink request received");
        }
        return request;
    }

    @Override
    public void close() {
        server.stop(0);
    }

}
