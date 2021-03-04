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
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.elements.exception.ConnectorException;
import org.thingsboard.server.common.msg.session.FeatureType;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class GetAttributesConObserveClient {

    private CoapClient getAttributesClient;
    private ExecutorService executor = Executors.newFixedThreadPool(1);

    private GetAttributesConObserveClient(String host, int port, String token) {
        this.getAttributesClient = new CoapClient(getFeatureTokenUrl(host, port, token));
        this.getAttributesClient.useCONs();
    }

    public void start() {
        executor.submit(new Runnable() {

            @Override
            public void run() {
                try {
                    while (!Thread.interrupted()) {
                        sendRequest(getAttributesClient);
                        Thread.sleep(10000);
                    }
                } catch (Exception e) {
                    log.error("Error occurred while sending COAP requests", e);
                }
            }

            private void sendRequest(CoapClient client) throws IOException, ConnectorException {
                CoapResponse getAttributesResponse = client.setTimeout((long) 60000).get();
                log.info("Response: {}, {}", getAttributesResponse.getCode(), getAttributesResponse.getResponseText());
            }
        });
    }

    protected void stop() {
        executor.shutdownNow();
    }

    public static void main(String args[]) {
        if (args.length != 3) {
            System.out.println("Usage: java -jar " + GetAttributesConObserveClient.class.getSimpleName() + ".jar host port device_token keys");
        }
        /**
         * DeviceEmulator(String host, int port, String token, String keys)
         * args[]:
         * host = "localhost",
         * port = 0,
         * token = "{Tokrn device from thingboard}"), kSzbDRGwaZqZ6Y25gTLF
         * keys = "{Telemetry}"
         *
         */
        final GetAttributesConObserveClient emulator = new GetAttributesConObserveClient(args[0], Integer.parseInt(args[1]), args[2]);
        emulator.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                emulator.stop();
            }
        });
    }

    private String getFeatureTokenUrl(String host, int port, String token) {
        return getBaseUrl(host, port) + token + "/" + FeatureType.ATTRIBUTES.name().toLowerCase() + "?clientKeys=attribute1,attribute2&sharedKeys=shared1,shared2";
    }

    private String getBaseUrl(String host, int port) {
        return "coap://" + host + ":" + port + "/api/v1/";
    }

}
