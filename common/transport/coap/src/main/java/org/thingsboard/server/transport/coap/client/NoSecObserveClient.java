/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.transport.coap.client;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.thingsboard.common.util.ThingsBoardThreadFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class NoSecObserveClient {

    private static final long INFINIT_EXCHANGE_LIFETIME = 0L;

    private CoapClient coapClient;
    private CoapObserveRelation observeRelation;
    private ExecutorService executor = Executors.newFixedThreadPool(1, ThingsBoardThreadFactory.forName(getClass().getSimpleName()));
    private CountDownLatch latch;

    public NoSecObserveClient(String host, int port, String accessToken) throws URISyntaxException {
        URI uri = new URI(getFutureUrl(host, port, accessToken));
        this.coapClient = new CoapClient(uri);
        coapClient.setTimeout(INFINIT_EXCHANGE_LIFETIME);
        this.latch = new CountDownLatch(5);
    }

    public void start() {
        executor.submit(() -> {
            try {
                Request request = Request.newGet();
                request.setObserve();
                observeRelation = coapClient.observe(request, new CoapHandler() {
                    @Override
                    public void onLoad(CoapResponse response) {
                        String responseText = response.getResponseText();
                        CoAP.ResponseCode code = response.getCode();
                        Integer observe = response.getOptions().getObserve();
                        log.info("CoAP Response received! " +
                                        "responseText: {}, " +
                                        "code: {}, " +
                                        "observe seq number: {}",
                                responseText,
                                code,
                                observe);
                        latch.countDown();
                    }

                    @Override
                    public void onError() {
                        log.error("Ack error!");
                        latch.countDown();
                    }
                });
            } catch (Exception e) {
                log.error("Error occurred while sending COAP requests: ");
            }
        });
        try {
            latch.await();
            observeRelation.proactiveCancel();
        } catch (InterruptedException e) {
            log.error("Error occurred: ", e);
        }
    }

    private String getFutureUrl(String host, Integer port, String accessToken) {
        return "coap://" + host + ":" + port + "/api/v1/" + accessToken + "/attributes";
    }

    public static void main(String[] args) throws URISyntaxException {
        log.info("Usage: java -cp ... org.thingsboard.server.transport.coap.client.NoSecObserveClient " +
                "host port accessToken");

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String accessToken = args[2];

        final NoSecObserveClient client = new NoSecObserveClient(host, port, accessToken);
        client.start();
    }
}
