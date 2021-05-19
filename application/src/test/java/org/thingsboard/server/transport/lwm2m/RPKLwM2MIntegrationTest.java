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
package org.thingsboard.server.transport.lwm2m;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.util.Hex;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.transport.lwm2m.secure.credentials.RPKClientCredentialsConfig;

import javax.annotation.PostConstruct;

import static org.eclipse.leshan.client.object.Security.rpk;

@Slf4j
public class RPKLwM2MIntegrationTest extends SecurityAbstractLw2mIntegrationTest {

    private final int port = 5689;
    public static final String RPK_TRANSPORT_CONFIGURATION_JSON_FILE_PATH = "lwm2m/transportConfiguration/RPKTransportConfiguration.json";
    public static final ObjectMapper mapper = JacksonUtil.OBJECT_MAPPER;
    public String transportConfigurationJsonAsString;
    public ObjectNode transportConfigurationJsonNode;

    public RPKLwM2MIntegrationTest() {

    }

    @PostConstruct
    private void init() throws JsonProcessingException {
        transportConfigurationJsonNode = initTransportConfigurationObjectNode(RPK_TRANSPORT_CONFIGURATION_JSON_FILE_PATH);

        ObjectNode lwm2mServerConfiguration = getLvm2mServerConfigurationFromTransportConfiguration(transportConfigurationJsonNode);
        String serverPublicKeyRPKAsString = Hex.encodeHexString(serverPublicKeyRPK.getEncoded());
        lwm2mServerConfiguration.put(SERVER_PUBLIC_KEY, serverPublicKeyRPKAsString);
        transportConfigurationJsonAsString = JacksonUtil.toString(transportConfigurationJsonNode);

        log.info(
                String.format(
                        "Setup LWM2M RPK test with transport configuration : \n%s",
                        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(transportConfigurationJsonNode)
                )
        );
    }

    @Test
    public void testConnectAndObserveTelemetry() throws Exception {

        RPKClientCredentialsConfig lwM2MClientCredentialsConfig = initRpkClientCredentialsConfig();
        testConnectAndObserveTelemetry(
                transportConfigurationJsonAsString,
                getEndpoint(),
                lwM2MClientCredentialsConfig,
                rpk(
                        getServerUri(), 123,
                        clientPublicKeyRPK.getEncoded(),
                        clientPrivateKeyRPK.getEncoded(),
                        serverX509Cert.getPublicKey().getEncoded()
                )
        );
    }

    @NotNull
    private RPKClientCredentialsConfig initRpkClientCredentialsConfig() {
        RPKClientCredentialsConfig lwM2MClientCredentialsConfig = new RPKClientCredentialsConfig();
        String key = Hex.encodeHexString(clientPublicKeyRPK.getEncoded());
        lwM2MClientCredentialsConfig.setKeyAsString(key);
        return lwM2MClientCredentialsConfig;
    }

    @Override
    public String getEndpoint() {
        return super.getEndpoint();
    }

    @Override
    public int getPort() {
        return port;
    }
}
