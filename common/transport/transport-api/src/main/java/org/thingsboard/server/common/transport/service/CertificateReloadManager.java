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

    private static final int MAX_CONSECUTIVE_FAILURES = 10;

    @Value("${transport.ssl.certificate.reload.enabled:true}")
    private boolean reloadEnabled;

    @Value("${transport.ssl.certificate.reload.check_interval:60}")
    private long checkIntervalInSeconds;

    @Autowired
    protected ApplicationContext applicationContext;

    private final Map<String, CertificateWatcher> watchers = new ConcurrentHashMap<>();
    private volatile ScheduledExecutorService scheduler;

    public void registerWatcher(String name, Path certPath, Runnable reloadCallback) {
        watchers.put(name, new CertificateWatcher(certPath, reloadCallback));
        log.info("Registered certificate watcher for: {}", name);
    }

    private void checkCertificates() {
        watchers.forEach((name, watcher) -> {
            try {
                watcher.checkAndReload(name);
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
        if (scheduler != null) {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        }
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!reloadEnabled) {
            log.trace("Auto-reload of certificates is disabled. Skipping initialization...");
            return;
        }
        log.info("Initializing Certificate Reload Manager...");

        discoverAndRegisterSslCredentials();

        scheduler = Executors.newSingleThreadScheduledExecutor(ThingsBoardThreadFactory.forName("certificate-reload-manager"));
        scheduler.scheduleWithFixedDelay(this::checkCertificates, checkIntervalInSeconds, checkIntervalInSeconds, TimeUnit.SECONDS);
    }

    static class CertificateWatcher {
        private final Path path;
        private final Runnable reloadCallback;
        private long lastModified;
        private String lastChecksum;
        private int consecutiveFailures;
        private String failedChecksum;

        CertificateWatcher(Path path, Runnable reloadCallback) {
            this.path = path;
            this.reloadCallback = reloadCallback;
            this.lastModified = getLastModifiedTime();
            this.lastChecksum = calculateChecksum();
            this.consecutiveFailures = 0;
        }

        synchronized void checkAndReload(String name) {
            long currentModified = getLastModifiedTime();
            if (currentModified == lastModified) {
                return;
            }

            String currentChecksum = calculateChecksum();
            if (currentChecksum.equals(lastChecksum)) {
                lastModified = currentModified;
                return;
            }

            if (!currentChecksum.equals(failedChecksum) && consecutiveFailures > 0) {
                // File content changed since last failure — reset and retry
                consecutiveFailures = 0;
                failedChecksum = null;
            }

            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                return;
            }

            try {
                log.info("Certificate change detected for: {}. Triggering reload...", name);
                reloadCallback.run();
                lastModified = currentModified;
                lastChecksum = currentChecksum;
                consecutiveFailures = 0;
                failedChecksum = null;
            } catch (Exception e) {
                consecutiveFailures++;
                failedChecksum = currentChecksum;
                log.error("Failed to reload certificate for {} (attempt {}/{}): {}",
                        name, consecutiveFailures, MAX_CONSECUTIVE_FAILURES, e.getMessage(), e);
            }
        }

        private long getLastModifiedTime() {
            try {
                if (!Files.exists(path)) {
                    return 0;
                }
                return Files.getLastModifiedTime(path).toMillis();
            } catch (IOException e) {
                return 0;
            }
        }

        private String calculateChecksum() {
            try {
                if (!Files.exists(path)) {
                    return "";
                }
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] bytes = Files.readAllBytes(path);
                byte[] hash = md.digest(bytes);
                return Base64.getEncoder().encodeToString(hash);
            } catch (Exception e) {
                log.warn("Failed to calculate checksum for certificate file: {}", path, e);
                return "";
            }
        }

    }

}
