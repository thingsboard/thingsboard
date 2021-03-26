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

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.Utils;
import org.eclipse.californium.elements.DtlsEndpointContext;
import org.eclipse.californium.elements.EndpointContext;
import org.eclipse.californium.elements.exception.ConnectorException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;

public class NoSecClient {

    public NoSecClient() {
    }

    public void test(String host, Integer port, String accessToken, String clientKeys, String sharedKeys) {
        CoapResponse response = null;
        try {
            URI uri = new URI(getFutureUrl(host, port, accessToken, clientKeys, sharedKeys));
            CoapClient client = new CoapClient(uri);
            response = client.get();
            client.shutdown();
        } catch (URISyntaxException e) {
            System.err.println("Invalid URI: " + e.getMessage());
            System.exit(-1);
        } catch (ConnectorException | IOException e) {
            System.err.println("Error occurred while sending request: " + e);
            System.exit(-1);
        }

        if (response != null) {

            System.out.println(response.getCode() + " - " + response.getCode().name());
            System.out.println(response.getOptions());
            System.out.println(response.getResponseText());
            System.out.println();
            System.out.println("ADVANCED:");
            EndpointContext context = response.advanced().getSourceContext();
            Principal identity = context.getPeerIdentity();
            if (identity != null) {
                System.out.println(context.getPeerIdentity());
            } else {
                System.out.println("anonymous");
            }
            System.out.println(context.get(DtlsEndpointContext.KEY_CIPHER));
            System.out.println(Utils.prettyPrint(response));
        } else {
            System.out.println("No response received.");
        }
    }

    private String getFutureUrl(String host, Integer port, String accessToken, String clientKeys, String sharedKeys) {
        return "coap://" + host + ":" + port + "/api/v1/" + accessToken + "/attributes?clientKeys=" + clientKeys + "&sharedKeys=" + sharedKeys;
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("Usage: java -cp ... org.thingsboard.server.transport.coap.client.NoSecClient " +
                "host port accessToken clientKeys sharedKeys");

        String host = args[0];
        Integer port = Integer.parseInt(args[1]);
        String accessToken = args[2];
        String clientKeys = args[3];
        String sharedKeys = args[4];

        NoSecClient client = new NoSecClient();
        client.test(host, port, accessToken, clientKeys, sharedKeys);
    }
}
