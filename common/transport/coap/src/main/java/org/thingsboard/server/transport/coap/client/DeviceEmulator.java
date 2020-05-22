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
package org.thingsboard.server.transport.coap.client;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Slf4j
public class DeviceEmulator {

    public static final String SN = "SN-" + new Random().nextInt(1000);
    public static final String MODEL = "Model " + new Random().nextInt(1000);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String host;
    private final int port;
    private final String token;

    private CoapClient attributesClient;
    private CoapClient telemetryClient;
    private CoapClient rpcClient;
    private String[] keys;
    private ExecutorService executor = Executors.newFixedThreadPool(1);
    private AtomicInteger seq = new AtomicInteger(100);

    private DeviceEmulator(String host, int port, String token, String keys) {
        this.host = host;
        this.port = port;
        this.token = token;
        this.attributesClient = new CoapClient(getFeatureTokenUrl(host, port, token, FeatureType.ATTRIBUTES));
        this.telemetryClient = new CoapClient(getFeatureTokenUrl(host, port, token, FeatureType.TELEMETRY));
        this.rpcClient = new CoapClient(getFeatureTokenUrl(host, port, token, FeatureType.RPC));
        this.keys = keys.split(",");
    }

    public void start() {
        executor.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    sendObserveRequest(rpcClient);
                    while (!Thread.interrupted()) {


                        sendRequest(attributesClient, createAttributesRequest());
                        sendRequest(telemetryClient, createTelemetryRequest());

                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    log.error("Error occurred while sending COAP requests", e);
                }
            }

            private void sendRequest(CoapClient client, JsonNode request) throws IOException, ConnectorException {
                CoapResponse telemetryResponse = client.setTimeout((long) 60000).post(mapper.writeValueAsString(request),
                        MediaTypeRegistry.APPLICATION_JSON);
                log.info("Response: {}, {}", telemetryResponse.getCode(), telemetryResponse.getResponseText());
            }

            private void sendObserveRequest(CoapClient client) throws JsonProcessingException {
                client.observe(new CoapHandler() {
                    @Override
                    public void onLoad(CoapResponse coapResponse) {
                        log.info("Command: {}, {}", coapResponse.getCode(), coapResponse.getResponseText());
                        try {
                            JsonNode node = mapper.readTree(coapResponse.getResponseText());
                            int requestId = node.get("id").asInt();
                            String method = node.get("method").asText();
                            ObjectNode params = (ObjectNode) node.get("params");
                            ObjectNode response = mapper.createObjectNode();
                            response.put("id", requestId);
                            response.set("response", params);
                            log.info("Command Response: {}, {}", requestId, mapper.writeValueAsString(response));
                            CoapClient commandResponseClient = new CoapClient(getFeatureTokenUrl(host, port, token, FeatureType.RPC));
                            commandResponseClient.post(new CoapHandler() {
                                @Override
                                public void onLoad(CoapResponse response) {
                                    log.info("Command Response Ack: {}, {}", response.getCode(), response.getResponseText());
                                }

                                @Override
                                public void onError() {
                                    //Do nothing
                                }
                            }, mapper.writeValueAsString(response), MediaTypeRegistry.APPLICATION_JSON);

                        } catch (IOException e) {
                            log.error("Error occurred while processing COAP response", e);
                        }
                    }

                    @Override
                    public void onError() {
                        //Do nothing
                    }
                });
            }

        });
    }

    private ObjectNode createAttributesRequest() {
        ObjectNode element = mapper.createObjectNode();
        element.put("serialNumber", SN);
        element.put("model", MODEL);
        return element;
    }

    private ArrayNode createTelemetryRequest() {
        ArrayNode rootNode = mapper.createArrayNode();
        for (String key : keys) {
            ObjectNode element = mapper.createObjectNode();
            element.put(key, seq.incrementAndGet());
            rootNode.add(element);
        }
        return rootNode;
    }

    protected void stop() {
        executor.shutdownNow();
    }

    public static void main(String args[]) {
        if (args.length != 4) {
            System.out.println("Usage: java -jar " + DeviceEmulator.class.getSimpleName() + ".jar host port device_token keys");
        }
        final DeviceEmulator emulator = new DeviceEmulator(args[0], Integer.parseInt(args[1]), args[2], args[3]);
        emulator.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                emulator.stop();
            }
        });
    }


    private String getFeatureTokenUrl(String host, int port, String token, FeatureType featureType) {
        return getBaseUrl(host, port) + token + "/" + featureType.name().toLowerCase();
    }

    private String getBaseUrl(String host, int port) {
        return "coap://" + host + ":" + port + "/api/v1/";
    }

}
