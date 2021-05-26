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
package org.thingsboard.server.transport.lwm2m.config;

import com.google.common.io.Resources;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.server.model.LwM2mModelProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.security.KeyStore;

@Slf4j
@Component
@ConditionalOnExpression("('${service.type:null}'=='tb-transport' && '${transport.lwm2m.enabled:false}'=='true') || '${service.type:null}'=='monolith' || '${service.type:null}'=='tb-core'")
public class LwM2MTransportServerConfig implements LwM2MSecureServerConfig {

    @Getter
    @Setter
    private LwM2mModelProvider modelProvider;

    @Getter
    @Value("${transport.lwm2m.timeout:}")
    private Long timeout;

    @Getter
    @Value("${transport.sessions.report_timeout}")
    private long sessionReportTimeout;

    @Getter
    @Value("${transport.lwm2m.recommended_ciphers:}")
    private boolean recommendedCiphers;

    @Getter
    @Value("${transport.lwm2m.recommended_supported_groups:}")
    private boolean recommendedSupportedGroups;

    @Getter
    @Value("${transport.lwm2m.response_pool_size:}")
    private int responsePoolSize;

    @Getter
    @Value("${transport.lwm2m.registered_pool_size:}")
    private int registeredPoolSize;

    @Getter
    @Value("${transport.lwm2m.registration_store_pool_size:}")
    private int registrationStorePoolSize;

    @Getter
    @Value("${transport.lwm2m.clean_period_in_sec:}")
    private int cleanPeriodInSec;

    @Getter
    @Value("${transport.lwm2m.update_registered_pool_size:}")
    private int updateRegisteredPoolSize;

    @Getter
    @Value("${transport.lwm2m.un_registered_pool_size:}")
    private int unRegisteredPoolSize;

    @Getter
    @Value("${transport.lwm2m.security.key_store_type:}")
    private String keyStoreType;

    @Getter
    @Value("${transport.lwm2m.security.key_store:}")
    private String keyStorePathFile;

    @Getter
    @Setter
    private KeyStore keyStoreValue;

    @Getter
    @Value("${transport.lwm2m.security.key_store_password:}")
    private String keyStorePassword;

    @Getter
    @Value("${transport.lwm2m.security.root_alias:}")
    private String rootCertificateAlias;

    @Getter
    @Value("${transport.lwm2m.security.enable_gen_new_key_psk_rpk:}")
    private Boolean enableGenNewKeyPskRpk;

    @Getter
    @Value("${transport.lwm2m.server.id:}")
    private Integer id;

    @Getter
    @Value("${transport.lwm2m.server.bind_address:}")
    private String host;

    @Getter
    @Value("${transport.lwm2m.server.bind_port:}")
    private Integer port;

    @Getter
    @Value("${transport.lwm2m.server.security.bind_address:}")
    private String secureHost;

    @Getter
    @Value("${transport.lwm2m.server.security.bind_port:}")
    private Integer securePort;

    @Getter
    @Value("${transport.lwm2m.server.security.public_x:}")
    private String publicX;

    @Getter
    @Value("${transport.lwm2m.server.security.public_y:}")
    private String publicY;

    @Getter
    @Value("${transport.lwm2m.server.security.private_encoded:}")
    private String privateEncoded;

    @Getter
    @Value("${transport.lwm2m.server.security.alias:}")
    private String certificateAlias;

    @Getter
    @Value("${transport.lwm2m.log_max_length:}")
    private int logMaxLength;


    @PostConstruct
    public void init() {
        URI uri = null;
        try {
            uri = Resources.getResource(keyStorePathFile).toURI();
            log.error("URI: {}", uri);
            File keyStoreFile = new File(uri);
            InputStream inKeyStore = new FileInputStream(keyStoreFile);
            keyStoreValue = KeyStore.getInstance(keyStoreType);
            keyStoreValue.load(inKeyStore, keyStorePassword == null ? null : keyStorePassword.toCharArray());
        } catch (Exception e) {
            log.info("Unable to lookup LwM2M keystore. Reason: {}, {}" , uri, e.getMessage());
        }
    }
}
