/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.transport.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.transport.config.ssl.SslCredentials;
import org.thingsboard.server.common.transport.config.ssl.SslCredentialsConfig;
import org.thingsboard.server.queue.util.TbTransportComponent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@TbTransportComponent
public class CertificateReloadManager implements SmartInitializingSingleton, DisposableBean {

    @Value("${transport.ssl.certificate.reload.enabled:true}")
    private boolean reloadEnabled;

    @Value("${transport.ssl.certificate.reload.check_interval:60}")
    private long checkIntervalInSeconds;

    @Autowired
    protected ApplicationContext applicationContext;

    private final Map<String, CertificateWatcher> watchers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("certificate-reload-manager"));

    public void registerWatcher(String name, Path certPath, Runnable reloadCallback) {
        watchers.put(name, new CertificateWatcher(certPath, reloadCallback));
        log.info("Registered certificate watcher for: {}", name);
    }

    private void checkCertificates() {
        watchers.forEach((name, watcher) -> {
            try {
                if (watcher.hasChanged()) {
                    log.info("Certificate changed detected for: {}. Triggering reload...", name);
                    watcher.reload();
                }
            } catch (Exception e) {
                log.error("Error checking certificate for {}: {}", name, e.getMessage(), e);
            }
        });
    }

    private void discoverAndRegisterSslCredentials() {
        try {
            Map<String, SslCredentialsConfig> sslConfigBeans = applicationContext.getBeansOfType(SslCredentialsConfig.class);

            log.info("Found {} SslCredentialsConfig beans", sslConfigBeans.size());

            for (Map.Entry<String, SslCredentialsConfig> entry : sslConfigBeans.entrySet()) {
                String beanName = entry.getKey();
                SslCredentialsConfig config = entry.getValue();

                try {
                    if (!config.isEnabled()) {
                        log.debug("Skipping disabled SSL config: {} ({})", config.getName(), beanName);
                        continue;
                    }

                    SslCredentials credentials = config.getCredentials();
                    if (credentials == null) {
                        log.debug("Skipping uninitialized SSL config: {} ({})", config.getName(), beanName);
                        continue;
                    }

                    List<Path> filePaths = credentials.getCertificateFilePaths();
                    if (filePaths == null || filePaths.isEmpty()) {
                        log.debug("No certificate files to watch for: {} ({})", config.getName(), beanName);
                        continue;
                    }

                    for (Path filePath : filePaths) {
                        if (filePath != null && Files.exists(filePath)) {
                            String watcherKey = config.getName() + " - " + filePath.getFileName();
                            registerWatcher(watcherKey, filePath, config::onCertificateFileChanged);
                            log.info("Registered certificate watcher: {} -> {}", config.getName(), filePath);
                        } else {
                            log.warn("Certificate file does not exist: {} (from {})", filePath, config.getName());
                        }
                    }

                } catch (Exception e) {
                    log.error("Error registering watchers for SSL config: {} ({})", config.getName(), beanName, e);
                }
            }

        } catch (Exception e) {
            log.error("Error discovering SSL credentials configs", e);
        }
    }

    @Override
    public void destroy() throws Exception {
        scheduler.shutdown();
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!reloadEnabled) {
            log.trace("Auto-reload of certificates is disabled. Skipping initialization...");
            return;
        }
        log.info("Initializing Certificate Reload Manager...");

        discoverAndRegisterSslCredentials();

        scheduler.scheduleWithFixedDelay(this::checkCertificates, checkIntervalInSeconds, checkIntervalInSeconds, TimeUnit.SECONDS);
    }

    private static class CertificateWatcher {
        private final Path path;
        private final Runnable reloadCallback;
        private long lastModified;
        private String lastChecksum;

        CertificateWatcher(Path path, Runnable reloadCallback) {
            this.path = path;
            this.reloadCallback = reloadCallback;
            this.lastModified = getLastModifiedTime();
            this.lastChecksum = calculateChecksum();
        }

        boolean hasChanged() {
            long currentModified = getLastModifiedTime();
            if (currentModified != lastModified) {
                String currentChecksum = calculateChecksum();
                return !currentChecksum.equals(lastChecksum);
            }
            return false;
        }

        void reload() {
            reloadCallback.run();
            lastModified = getLastModifiedTime();
            lastChecksum = calculateChecksum();
        }

        private long getLastModifiedTime() {
            try {
                return Files.getLastModifiedTime(path).toMillis();
            } catch (IOException e) {
                return 0;
            }
        }

        private String calculateChecksum() {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] bytes = Files.readAllBytes(path);
                byte[] hash = md.digest(bytes);
                return Base64.getEncoder().encodeToString(hash);
            } catch (Exception e) {
                return "";
            }
        }

    }

}
