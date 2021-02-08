/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import org.eclipse.californium.elements.exception.ConnectorException;
import org.thingsboard.server.common.msg.session.FeatureType;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AttributesUpdateConObserveClient {

    private CoapClient client;
    private ExecutorService executor = Executors.newFixedThreadPool(1);
    private final CountDownLatch responsesLatch = new CountDownLatch(2);

    private AttributesUpdateConObserveClient(String host, int port, String token) {
        String featureTokenUrl = getFeatureTokenUrl(host, port, token);
        log.info("featureTokenUrl: {}", featureTokenUrl);
        this.client = new CoapClient(featureTokenUrl);
        this.client.useCONs();
    }

    public void start() {
        executor.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    Request request = Request.newGet().setObserve();
                    request.setType(CoAP.Type.CON);
                    sendObserveRequest(client, request);
                } catch (Exception e) {
                    log.error("Error occurred while sending COAP requests", e);
                }
            }

            private void sendObserveRequest(CoapClient client, Request request) throws ConnectorException, IOException, InterruptedException {
                CoapObserveRelation coapObserveRelation = client.observeAndWait(request, new CoapHandler() {
                    @Override
                    public void onLoad(CoapResponse coapResponse) {
                        log.info("[Observation response] code: [{}], responseText: [{}], response observe number: [{}]",
                                coapResponse.getCode(), coapResponse.getResponseText(), coapResponse.getOptions().getObserve());
                        responsesLatch.countDown();
                    }

                    @Override
                    public void onError() {
                        log.info("Request failure!");
                    }
                });
                responsesLatch.await();
                coapObserveRelation.proactiveCancel();
                log.info("[CoapObserveRelation] proactive cancel called!");
            }
        });
    }

    public void stop() {
        client.shutdown();
        executor.shutdownNow();
    }

    public static void main(String args[]) {
        if (args.length != 3) {
            log.error("Usage: java -jar {}.jar host port device_token", AttributesUpdateConObserveClient.class.getSimpleName());
        }
        final AttributesUpdateConObserveClient emulator = new AttributesUpdateConObserveClient(args[0], Integer.parseInt(args[1]), args[2]);
        emulator.start();
        Runtime.getRuntime().addShutdownHook(new Thread(emulator::stop));
    }

    private String getFeatureTokenUrl(String host, int port, String token) {
        return getBaseUrl(host, port) + token + "/" + FeatureType.ATTRIBUTES.name().toLowerCase();
    }

    private String getBaseUrl(String host, int port) {
        return "coap://" + host + ":" + port + "/api/v1/";
    }
}
