// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.config.ssl;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

    private final List<Runnable> reloadCallbacks = new CopyOnWriteArrayList<>();

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

    public void onCertificateFileChanged() {
        log.info("{}: Certificate file changed. Reloading SSL credentials...", name);
        try {
            this.credentials.reload(this.trustsOnly);
        } catch (Exception e) {
            log.error("{}: Failed to reload SSL credentials", name, e);
            // Rethrow, so CertificateReloadManager's watcher counts this as a failure
            // and applies MAX_CONSECUTIVE_FAILURES backoff instead of treating it as a successful reload.
            throw new RuntimeException(name + ": Failed to reload SSL credentials", e);
        }
        log.info("{}: SSL credentials reloaded successfully.", name);

        for (Runnable callback : reloadCallbacks) {
            try {
                callback.run();
            } catch (Exception e) {
                log.error("{}: Error executing reload callback", name, e);
            }
        }
    }

    public void registerReloadCallback(Runnable callback) {
        this.reloadCallbacks.add(callback);
    }

}
