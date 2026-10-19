// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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


