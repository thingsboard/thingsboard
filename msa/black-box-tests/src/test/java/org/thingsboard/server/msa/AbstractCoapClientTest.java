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
package org.thingsboard.server.msa;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import org.eclipse.californium.core.config.CoapConfig;
import org.eclipse.californium.elements.config.Configuration;
import org.eclipse.californium.elements.config.Configuration.ModuleDefinitionsProvider;
import org.eclipse.californium.elements.config.IntegerDefinition;
import org.eclipse.californium.elements.config.TcpConfig;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.msg.session.FeatureType;

public abstract class AbstractCoapClientTest extends AbstractContainerTest{

    private static final String CONTAINER_COAP_BASE_URL = "coap://localhost:5683/api/v1/";
    private static final long CLIENT_REQUEST_TIMEOUT = 60000L;


    private static final String COAP_CLIENT_TEST = "COAP_CLIENT_TEST.";
    private static final IntegerDefinition COAP_PORT_DEF = CoapConfig.COAP_PORT;

    private static final ModuleDefinitionsProvider MODULE_DEFINITIONS_PROVIDER = new ModuleDefinitionsProvider() {

        @Override
        public String getModule() {
            return COAP_CLIENT_TEST;
        }

        @Override
        public void applyDefinitions(Configuration config) {
            TcpConfig.register();
            config.set(COAP_PORT_DEF, 5683);
        }
    };

    protected CoapClient client;

    protected byte[] createCoapClientAndPublish(String deviceName) throws Exception {
        String provisionRequestMsg = createTestProvisionMessage(deviceName);
        Configuration.addDefaultModule(MODULE_DEFINITIONS_PROVIDER);
        String featureTokenUrl = CONTAINER_COAP_BASE_URL + FeatureType.PROVISION.name().toLowerCase();
        client = new CoapClient(featureTokenUrl);
        return client.setTimeout(CLIENT_REQUEST_TIMEOUT)
                .post(provisionRequestMsg.getBytes(), MediaTypeRegistry.APPLICATION_JSON)
                .getPayload();
    }

    protected void disconnect() {
        if (client != null) {
            client.shutdown();
        }
    }

    private String createTestProvisionMessage(String deviceName) {
        ObjectNode provisionRequest = JacksonUtil.newObjectNode();
        provisionRequest.put("provisionDeviceKey", TEST_PROVISION_DEVICE_KEY);
        provisionRequest.put("provisionDeviceSecret", TEST_PROVISION_DEVICE_SECRET);
        if (deviceName != null) {
            provisionRequest.put("deviceName", deviceName);
        }
        return provisionRequest.toString();
    }
}


