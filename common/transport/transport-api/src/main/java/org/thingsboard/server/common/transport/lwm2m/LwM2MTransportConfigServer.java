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
package org.thingsboard.server.common.transport.lwm2m;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.model.ObjectLoader;
import org.eclipse.leshan.core.model.ObjectModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.List;

@Slf4j
@Component
//@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.api_enabled:true}'=='true' && '${transport.lwm2m.enabled}'=='true') || '${service.type:null}'=='tb-core'")
//@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || ('${service.type:null}'=='monolith' && '${transport.lwm2m.enabled}'=='true') || '${service.type:null}'=='tb-core'")
@ConditionalOnExpression("'${service.type:null}'=='tb-transport' || '${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core'")
public class LwM2MTransportConfigServer {

    @Getter
    @Value("${transport.lwm2m.timeout:}")
    private Long timeout;

    @Getter
    @Value("${transport.lwm2m.model_path_file:}")
    private String modelPathFile;

    @Getter
    private String MODEL_RESOURCE_PATH_DEFAULT = "/models";

    @Getter
    private String KEY_STORE_DEFAULT_RESOURCE_PATH = "credentials/serverKeyStore.jks";

    @Getter
    @Setter
    private List<ObjectModel> modelsValue;

    @Getter
    @Value("${transport.lwm2m.support_deprecated_ciphers_enable:}")
    private boolean supportDeprecatedCiphersEnable;

    @Getter
    @Value("${transport.lwm2m.secure.key_store_type:}")
    private String keyStoreType;

    @Getter
    @Value("${transport.lwm2m.secure.key_store_path_file:}")
    private String keyStorePathFile;

    @Getter
    @Setter
    private KeyStore keyStoreValue;

    @Getter
    @Value("${transport.lwm2m.secure.key_store_password:}")
    private String keyStorePasswordServer;

    @Getter
    @Value("${transport.lwm2m.secure.root_alias:}")
    private String rootAlias;

    @Getter
    @Value("${transport.lwm2m.secure.enable_gen_psk_rpk:}")
    private Boolean enableGenPskRpk;

    @Getter
    @Value("${transport.lwm2m.server.bind_address:}")
    private String serverHost;

    @Getter
    @Value("${transport.lwm2m.server.bind_port:}")
    private Integer serverPort;

    @Getter
    @Value("${transport.lwm2m.server.bind_port_cert:}")
    private Integer serverPortCert;

    @Getter
    @Value("${transport.lwm2m.server.secure.start_all:}")
    private boolean serverStartAll;

    @Getter
    @Value("${transport.lwm2m.server.secure.dtls_mode:}")
    private Integer serverDtlsMode;

    @Getter
    @Value("${transport.lwm2m.server.secure.bind_address:}")
    private String serverSecureHost;

    @Getter
    @Value("${transport.lwm2m.server.secure.bind_port:}")
    private Integer serverSecurePort;

    @Getter
    @Value("${transport.lwm2m.server.secure.bind_port_cert:}")
    private Integer serverSecurePortCert;

    @Getter
    @Value("${transport.lwm2m.server.secure.public_x:}")
    private String serverPublicX;

    @Getter
    @Value("${transport.lwm2m.server.secure.public_y:}")
    private String serverPublicY;

    @Getter
    @Value("${transport.lwm2m.server.secure.private_s:}")
    private String serverPrivateS;

    @Getter
    @Value("${transport.lwm2m.server.secure.alias:}")
    private String serverAlias;

    @Getter
    @Value("${transport.lwm2m.bootstrap.enable:}")
    private Boolean bootstrapEnable;

    @Getter
    @Value("${transport.lwm2m.secure.redis_url:}")
    private String redisUrl;

    @PostConstruct
    public void init() {
        modelsValue = ObjectLoader.loadDefault();
        File path = getPathModels();
        modelsValue.addAll(ObjectLoader.loadObjectsFromDir(path));
        getInKeyStore();
    }

    private File getPathModels() {
        return (modelPathFile != null && !modelPathFile.isEmpty()) ? new File(modelPathFile) :
                new File(getClass().getResource(MODEL_RESOURCE_PATH_DEFAULT).getPath());
    }

    private KeyStore getInKeyStore() {
        KeyStore keyStoreServer = null;
        try {
            if (keyStoreValue != null && keyStoreValue.size() > 0)
                return keyStoreValue;
        }
        catch (KeyStoreException e) {
        }
        try (InputStream inKeyStore = keyStorePathFile.isEmpty() ?
                ClassLoader.getSystemResourceAsStream(KEY_STORE_DEFAULT_RESOURCE_PATH) : new FileInputStream(new File(keyStorePathFile))) {
            keyStoreServer = KeyStore.getInstance(keyStoreType);
            keyStoreServer.load(inKeyStore, keyStorePasswordServer == null ? null : keyStorePasswordServer.toCharArray());
        } catch (Exception ex) {
            log.error("[{}] Unable to load KeyStore  files server", ex.getMessage());
        }
        keyStoreValue = keyStoreServer;
        return  keyStoreValue;
    }
}
