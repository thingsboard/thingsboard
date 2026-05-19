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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
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

    @Value("${transport.ssl.certificate.reload.check_interval_seconds:60}")
    private long checkIntervalInSeconds;

    @Autowired
    protected ApplicationContext applicationContext;

    private final Map<String, CertificateWatcher> watchers = new ConcurrentHashMap<>();
    private volatile ScheduledExecutorService scheduler;

    public void registerWatcher(String name, Path certPath, Runnable reloadCallback) {
        registerWatcher(name, List.of(certPath), reloadCallback);
    }

    public void registerWatcher(String name, List<Path> certPaths, Runnable reloadCallback) {
        watchers.put(name, new CertificateWatcher(certPaths, reloadCallback));
        log.info("Registered certificate watcher for: {} (watching {} file(s))", name, certPaths.size());
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
                        log.debug("No file-system certificate paths to watch for: {} ({}) — certificates may be classpath-based", config.getName(), beanName);
                        continue;
                    }

                    // Register all configured paths, including those that don't exist yet — the watcher uses
                    // mtime=0 / checksum="" as baseline, so files that appear later (e.g. delayed mounts) are
                    // picked up and trigger a reload on the next poll.
                    List<Path> pathsToWatch = new ArrayList<>(filePaths.size());
                    for (Path filePath : filePaths) {
                        if (filePath == null) {
                            continue;
                        }
                        pathsToWatch.add(filePath);
                        if (!Files.exists(filePath)) {
                            log.warn("Certificate file does not exist yet: {} (from {}) — will be watched and picked up when it appears",
                                    filePath, config.getName());
                        }
                    }

                    if (!pathsToWatch.isEmpty()) {
                        registerWatcher(config.getName(), pathsToWatch, config::onCertificateFileChanged);
                        log.info("Registered certificate watcher: {} -> {}", config.getName(), pathsToWatch);
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
        private final List<Path> paths;
        private final Runnable reloadCallback;
        private final Map<Path, Long> lastModifiedMap;
        private final Map<Path, String> lastChecksumMap;
        private int consecutiveFailures;
        private String failedCombinedChecksum;

        CertificateWatcher(List<Path> paths, Runnable reloadCallback) {
            this.paths = paths;
            this.reloadCallback = reloadCallback;
            this.lastModifiedMap = new HashMap<>();
            this.lastChecksumMap = new HashMap<>();
            for (Path path : paths) {
                lastModifiedMap.put(path, getLastModifiedTime(path));
                lastChecksumMap.put(path, calculateChecksum(path));
            }
            this.consecutiveFailures = 0;
        }

        synchronized void checkAndReload(String name) {
            boolean anyModifiedChanged = false;
            for (Path path : paths) {
                long currentModified = getLastModifiedTime(path);
                Long lastModified = lastModifiedMap.getOrDefault(path, 0L);
                if (currentModified != lastModified) {
                    anyModifiedChanged = true;
                    break;
                }
            }
            if (!anyModifiedChanged) {
                return;
            }

            // Capture mtimes and checksums together before the callback runs.
            // Pairing a post-callback mtime with a pre-callback checksum would let a write-during-reload be missed on the next poll.
            Map<Path, Long> currentModifiedTimes = new HashMap<>();
            Map<Path, String> currentChecksums = new HashMap<>();
            StringBuilder combined = new StringBuilder();
            for (Path path : paths) {
                currentModifiedTimes.put(path, getLastModifiedTime(path));
                String checksum = calculateChecksum(path);
                currentChecksums.put(path, checksum);
                if (!combined.isEmpty()) {
                    combined.append("|");
                }
                combined.append(path).append("=").append(checksum);
            }
            String combinedChecksum = combined.toString();

            // Build old combined checksum for comparison
            StringBuilder oldCombined = new StringBuilder();
            for (Path path : paths) {
                if (!oldCombined.isEmpty()) {
                    oldCombined.append("|");
                }
                oldCombined.append(path).append("=").append(lastChecksumMap.getOrDefault(path, ""));
            }
            String oldCombinedChecksum = oldCombined.toString();

            if (combinedChecksum.equals(oldCombinedChecksum)) {
                // Content unchanged, just update modification times
                for (Path path : paths) {
                    lastModifiedMap.put(path, currentModifiedTimes.get(path));
                }
                return;
            }

            if (!combinedChecksum.equals(failedCombinedChecksum) && consecutiveFailures > 0) {
                // File content has changed since the last failure - reset and retry
                consecutiveFailures = 0;
                failedCombinedChecksum = null;
            }

            if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                // Update modification times to avoid re-checking mtime and re-computing checksums every poll cycle
                for (Path path : paths) {
                    lastModifiedMap.put(path, currentModifiedTimes.get(path));
                }
                return;
            }

            try {
                log.info("Certificate change detected for: {}. Triggering reload...", name);
                reloadCallback.run();
                for (Path path : paths) {
                    lastModifiedMap.put(path, currentModifiedTimes.get(path));
                    lastChecksumMap.put(path, currentChecksums.get(path));
                }
                consecutiveFailures = 0;
                failedCombinedChecksum = null;
            } catch (Exception e) {
                consecutiveFailures++;
                failedCombinedChecksum = combinedChecksum;
                // Deliberately NOT updating the lastModifiedMap here, so the next poll cycle retries
                // (mtime mismatch passes the early gate, checksum matches failedCombinedChecksum).
                log.error("Failed to reload certificate for {} (attempt {}/{}): {}",
                        name, consecutiveFailures, MAX_CONSECUTIVE_FAILURES, e.getMessage(), e);
            }
        }

        private long getLastModifiedTime(Path path) {
            try {
                if (!Files.exists(path)) {
                    return 0;
                }
                return Files.getLastModifiedTime(path).toMillis();
            } catch (IOException e) {
                return 0;
            }
        }

        private String calculateChecksum(Path path) {
            try {
                if (!Files.exists(path)) {
                    return "";
                }
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] buf = new byte[8192];
                try (InputStream is = Files.newInputStream(path)) {
                    int bytesRead;
                    while ((bytesRead = is.read(buf)) != -1) {
                        md.update(buf, 0, bytesRead);
                    }
                }
                return Base64.getEncoder().encodeToString(md.digest());
            } catch (Exception e) {
                log.warn("Failed to calculate checksum for certificate file: {}", path, e);
                return "";
            }
        }

    }

}
