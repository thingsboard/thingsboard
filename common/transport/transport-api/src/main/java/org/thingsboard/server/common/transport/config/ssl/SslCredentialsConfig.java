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
package org.thingsboard.server.common.transport.config.ssl;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class SslCredentialsConfig {

    private boolean enabled = true;
    private SslCredentialsType type;
    private PemSslCredentials pem;
    private KeystoreSslCredentials keystore;

    private SslCredentials credentials;

    private final String name;
    private final boolean trustsOnly;

    public SslCredentialsConfig(String name, boolean trustsOnly) {
        this.name = name;
        this.trustsOnly = trustsOnly;
    }

    @PostConstruct
    public void init() {
        if (this.enabled) {
            log.info("{}: Initializing SSL credentials.", name);
            if (SslCredentialsType.PEM.equals(type) && pem.canUse()) {
                this.credentials = this.pem;
            } else if (keystore.canUse()) {
                if (SslCredentialsType.PEM.equals(type)) {
                    log.warn("{}: Specified PEM configuration is not valid. Using SSL keystore configuration as fallback.", name);
                }
                this.credentials = this.keystore;
            } else {
                throw new RuntimeException(name + ": Invalid SSL credentials configuration. None of the PEM or KEYSTORE configurations can be used!");
            }
            try {
                this.credentials.init(this.trustsOnly);
            } catch (Exception e) {
                throw new RuntimeException(name + ": Failed to init SSL credentials configuration.", e);
            }
        } else {
            log.info("{}: Skipping initialization of disabled SSL credentials.", name);
        }
    }

}
