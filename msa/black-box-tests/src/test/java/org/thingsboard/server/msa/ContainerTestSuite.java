// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.msa;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.thingsboard.server.common.data.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.testng.Assert.fail;
import static org.thingsboard.server.msa.TestUtils.addComposeVersion;

@Slf4j
public class ContainerTestSuite {
    final static boolean IS_VALKEY_CLUSTER = Boolean.parseBoolean(System.getProperty("blackBoxTests.redisCluster"));
    final static boolean IS_VALKEY_SENTINEL = Boolean.parseBoolean(System.getProperty("blackBoxTests.redisSentinel"));
    final static boolean IS_VALKEY_SSL = Boolean.parseBoolean(System.getProperty("blackBoxTests.redisSsl"));
    final static boolean IS_HYBRID_MODE = Boolean.parseBoolean(System.getProperty("blackBoxTests.hybridMode"));
    final static boolean IS_CITUS = Boolean.parseBoolean(System.getProperty("blackBoxTests.citus"));
    private static final String TB_CORE_LOG_REGEXP = ".*Starting polling for events.*";
    private static final String TB_IE_LOG_REGEXP = ".*Started ThingsboardIntegrationExecutorApplication.*";
    private static final String TRANSPORTS_LOG_REGEXP = ".*Going to recalculate partitions.*";
    private static final String TB_VC_LOG_REGEXP = TRANSPORTS_LOG_REGEXP;
    private static final String INTEGRATION_LOG_REGEXP = ".*Sending a connect request to the TB!.*";
    private static final String TB_JS_EXECUTOR_LOG_REGEXP = ".*template started.*";
    private static final String TB_EDQS_LOG_REGEXP = ".*All partitions processed.*";
    private static final String TB_REPORT_LOG_REGEXP = ".*Going to recalculate partitions.*";
    private static final String TRENDZ_LOG_REGEXP = ".*Started TrendzApplication.*";
    private static final String TRENDZ_PYTHON_EXECUTOR_LOG_REGEXP = ".*Started PythonExecutorApplication.*";
    private static final Duration CONTAINER_STARTUP_TIMEOUT = Duration.ofSeconds(400);

    private DockerComposeContainerImpl testContainer;
    private ThingsBoardDbInstaller installTb;
    private boolean isActive;

    private static ContainerTestSuite containerTestSuite;

    private ContainerTestSuite() {
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public static ContainerTestSuite getInstance() {
        if (containerTestSuite == null) {
            containerTestSuite = new ContainerTestSuite();
        }
        return containerTestSuite;
    }

    public void start() {
        log.info("System property of blackBoxTests.redisCluster is {}", IS_VALKEY_CLUSTER);
        log.info("System property of blackBoxTests.redisSentinel is {}", IS_VALKEY_SENTINEL);
        log.info("System property of blackBoxTests.redisSsl is {}", IS_VALKEY_SSL);
        log.info("System property of blackBoxTests.hybridMode is {}", IS_HYBRID_MODE);
        log.info("System property of blackBoxTests.citus is {}", IS_CITUS);
        if (IS_CITUS && IS_HYBRID_MODE) {
            throw new IllegalStateException("blackBoxTests.citus and blackBoxTests.hybridMode are mutually exclusive: Citus is a distributed-PostgreSQL layout and cannot run with the Cassandra-backed hybrid mode.");
        }
        boolean skipTailChildContainers = Boolean.parseBoolean(System.getProperty("blackBoxTests.skipTailChildContainers"));
        try {
            final String targetDir = FileUtils.getTempDirectoryPath() + "/" + "ContainerTestSuite-" + UUID.randomUUID() + "/";
            log.info("targetDir {}", targetDir);
            // The lock must span the copy, not just the checkout: a concurrent run resets the cache tree in place.
            try (ComposeRepository.Checkout compose = ComposeRepository.checkout()) {
                FileUtils.copyDirectory(compose.getPath().toFile(), new File(targetDir), file -> !".git".equals(file.getName()));
            }
            // The compose branch this runs against still names the images tb-pe-*, which nothing builds any more.
            // Renaming them here is the whole difference between that branch and the 4.4 one; it becomes a no-op
            // once tb.compose.ref points at a branch that carries the new names.
            replaceInFile(targetDir, ".env", Map.of("tb-pe-", "tb-"));
            replaceInFile(targetDir + "advanced/docker-compose.yml", "    container_name: \"${LOAD_BALANCER_NAME}\"", "", "container_name");
            FileUtils.copyDirectory(new File("src/test/resources"), new File(targetDir));

            installTb = new ThingsBoardDbInstaller(targetDir);
            // Marked active before the volumes exist, not after the containers are up: the teardown that saves
            // the container logs and removes the volumes is guarded on this flag, and everything from here on
            // can fail - so a bring-up failure is exactly when those logs are needed, and when the volumes and
            // the half-started compose project would otherwise be left behind.
            setActive(true);
            installTb.createVolumes();

            if (IS_VALKEY_SSL) {
                addToFile(targetDir, "cache-valkey.env",
                        Map.of("TB_REDIS_SSL_ENABLED", "true",
                                "TB_REDIS_SSL_PEM_CERT", "/valkey/certs/valkeyCA.crt"));
            }

            List<File> composeFiles = new ArrayList<>(Arrays.asList(
                    new File(targetDir + "advanced/docker-compose.yml"),
                    new File(targetDir + "advanced/docker-compose.edqs.yml"),
                    new File(targetDir + "advanced/docker-compose.edqs.volumes.yml"),
                    new File(targetDir + "advanced/docker-compose.volumes.yml"),
                    new File(targetDir + "advanced/" + (IS_HYBRID_MODE ? "docker-compose.hybrid.yml" : "docker-compose.postgres.yml")),
                    new File(targetDir + (IS_HYBRID_MODE ? "docker-compose.hybrid-test-extras.yml" : "docker-compose.postgres-test-extras.yml")),
                    new File(targetDir + "advanced/docker-compose.postgres.volumes.yml"),
                    new File(targetDir + "docker-compose.integration.yml"),
                    new File(targetDir + "docker-compose.mosquitto.yml"),
                    new File(targetDir + "docker-compose.opc-ua.yml"),
                    new File(targetDir + "advanced/docker-compose.kafka.yml"),
                    new File(targetDir + "advanced/docker-compose.trendz.yml"),
                    new File(targetDir + "advanced/" + resolveValkeyComposeFile()),
                    new File(targetDir + "advanced/" + resolveValkeyComposeVolumesFile()),
                    new File(targetDir + ("docker-selenium.yml"))
            ));
            if (IS_CITUS) {
                composeFiles.add(new File(targetDir + "advanced/docker-compose.citus.yml"));
            }
            addToFile(targetDir, "queue-kafka.env", Map.of("TB_QUEUE_PREFIX", "test"));
            addToFile(targetDir, "tb-edqs.env", Map.of("TB_QUEUE_PREFIX", "test"));
            // Keyless: no licence key, no licence server, no identity to keep out of a public repo. The secret
            // has to be emptied rather than left alone, because any non-empty value wins over the flag - so
            // this uses the verifying form, which fails the run rather than quietly leaving the placeholder
            // in place should a later compose branch write the assignment differently.
            replaceInFile(targetDir + "tb-node.env", "TB_LICENSE_SECRET=YOUR_LICENSE_KEY_HERE",
                    "TB_LICENSE_SECRET=", "YOUR_LICENSE_KEY_HERE");
            addToFile(targetDir, "tb-node.env", Map.of("NON_PRODUCTION_USE", "true"));

            if (IS_HYBRID_MODE) {
                composeFiles.add(new File(targetDir + "advanced/docker-compose.cassandra.volumes.yml"));
            }

            addComposeVersion(composeFiles, "3.0");

            testContainer = new DockerComposeContainerImpl(targetDir, composeFiles)
                    .withPull(false)
                    .withLocalCompose(true)
                    .withOptions("--compatibility")
                    .withTailChildContainers(!skipTailChildContainers)
                    .withEnv(installTb.getEnv())
                    .withEnv("TB_QUEUE_TYPE", "kafka")
                    .withEnv("LOAD_BALANCER_NAME", "")
                    .withExposedService("haproxy", 80, Wait.forHttp("/swagger-ui.html").withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .withExposedService("tb-http-integration", 8082)
                    .withExposedService("tb-integration-executor1", 8082)
                    .withExposedService("tb-mqtt-integration", 8082)
                    .withExposedService("broker", 1883)
                    .waitingFor("tb-core1", Wait.forLogMessage(TB_CORE_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-core2", Wait.forLogMessage(TB_CORE_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-rule-engine1", Wait.forLogMessage(TRANSPORTS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-rule-engine2", Wait.forLogMessage(TRANSPORTS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-http-transport1", Wait.forLogMessage(TRANSPORTS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-http-transport2", Wait.forLogMessage(TRANSPORTS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-mqtt-transport1", Wait.forLogMessage(TRANSPORTS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-mqtt-transport2", Wait.forLogMessage(TRANSPORTS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-coap-transport", Wait.forLogMessage(TRANSPORTS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-lwm2m-transport", Wait.forLogMessage(TRANSPORTS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-mqtt-integration", Wait.forLogMessage(INTEGRATION_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-http-integration", Wait.forLogMessage(INTEGRATION_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-tcp-integration", Wait.forLogMessage(INTEGRATION_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-udp-integration", Wait.forLogMessage(INTEGRATION_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-coap-integration", Wait.forLogMessage(INTEGRATION_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-integration-executor1", Wait.forLogMessage(TB_IE_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-integration-executor2", Wait.forLogMessage(TB_IE_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-vc-executor1", Wait.forLogMessage(TB_VC_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-vc-executor2", Wait.forLogMessage(TB_VC_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-js-executor", Wait.forLogMessage(TB_JS_EXECUTOR_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-edqs1", Wait.forLogMessage(TB_EDQS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-edqs2", Wait.forLogMessage(TB_EDQS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-report1", Wait.forLogMessage(TB_REPORT_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-report2", Wait.forLogMessage(TB_REPORT_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("trendz", Wait.forLogMessage(TRENDZ_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("trendz-python-executor", Wait.forLogMessage(TRENDZ_PYTHON_EXECUTOR_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));
            testContainer.start();
        } catch (Throwable e) {
            // Throwable, not Exception: the verifying replaceInFile above asserts, and an AssertionError from
            // it would otherwise leave the suite without the teardown that removes the volumes it created.
            log.error("Failed to create test container", e);
            fail("Failed to create test container", e);
        }
    }

    private static String resolveValkeyComposeFile() {
        if (IS_VALKEY_CLUSTER) {
            return "docker-compose.valkey-cluster.yml";
        }
        if (IS_VALKEY_SENTINEL) {
            return "docker-compose.valkey-sentinel.yml";
        }
        if (IS_VALKEY_SSL) {
            return "docker-compose.valkey-ssl.yml";
        }
        return "docker-compose.valkey.yml";
    }

    private static String resolveValkeyComposeVolumesFile() {
        if (IS_VALKEY_CLUSTER) {
            return "docker-compose.valkey-cluster.volumes.yml";
        }
        if (IS_VALKEY_SENTINEL) {
            return "docker-compose.valkey-sentinel.volumes.yml";
        }
        if (IS_VALKEY_SSL) {
            return "docker-compose.valkey-ssl.volumes.yml";
        }
        return "docker-compose.valkey.volumes.yml";
    }

    public void stop() {
        if (!isActive) {
            return;
        }
        try {
            // Null when the bring-up failed before the compose container was built. The volumes exist by then -
            // the flag is set alongside them - so the cleanup below still has work to do.
            if (testContainer != null) {
                testContainer.stop();
            }
        } finally {
            // In a finally: a stop that throws must not be what leaves the volumes of this run behind.
            installTb.saveLogsAndRemoveVolumes();
            if (testContainer != null) {
                testContainer.cleanup();
            }
            setActive(false);
        }
    }

    private static void replaceInFile(String targetDir, String fileName, Map<String, String> replacements) throws IOException {
        Path envFilePath = Path.of(targetDir, fileName);
        String data = Files.readString(envFilePath);
        for (var entry : replacements.entrySet()) {
            data = data.replace(entry.getKey(), entry.getValue());
        }
        Files.write(envFilePath, data.getBytes(StandardCharsets.UTF_8));
    }

    private static void addToFile(String targetDir, String fileName, Map<String, String> properties) throws IOException {
        Path envFilePath = Path.of(targetDir, fileName);
        StringBuilder data = new StringBuilder(Files.readString(envFilePath));
        for (var entry : properties.entrySet()) {
            data.append("\n").append(entry.getKey()).append("=").append(entry.getValue());
        }
        Files.write(envFilePath, data.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static String getSysProp(String propertyName) {
        var value = System.getProperty(propertyName);
        if (StringUtils.isEmpty(value)) {
            throw new RuntimeException("Please define system property: " + propertyName + "!");
        }
        return value;
    }

    private static void tryDeleteDir(String targetDir) {
        try {
            log.info("Trying to delete temp dir {}", targetDir);
            FileUtils.deleteDirectory(new File(targetDir));
        } catch (IOException e) {
            log.error("Can't delete temp directory {}", targetDir, e);
        }
    }

    /**
     * This workaround is actual until issue will be resolved:
     * Support container_name in docker-compose file #2472 https://github.com/testcontainers/testcontainers-java/issues/2472
     * docker-compose files which contain container_name are not supported and the creation of DockerComposeContainer fails due to IllegalStateException.
     * This has been introduced in #1151 as a quick fix for unintuitive feedback. https://github.com/testcontainers/testcontainers-java/issues/1151
     * Using the latest testcontainers and waiting for the fix...
     */
    private static void replaceInFile(String sourceFilename, String target, String replacement, String verifyPhrase) {
        try {
            File file = new File(sourceFilename);
            String sourceContent = FileUtils.readFileToString(file, StandardCharsets.UTF_8);

            String outputContent = sourceContent.replace(target, replacement);
            assertThat(outputContent, (not(containsString(target))));
            assertThat(outputContent, (not(containsString(verifyPhrase))));

            FileUtils.writeStringToFile(file, outputContent, StandardCharsets.UTF_8);
            assertThat(FileUtils.readFileToString(file, StandardCharsets.UTF_8), is(outputContent));
        } catch (IOException e) {
            log.error("failed to update file {}", sourceFilename, e);
            fail("failed to update file", e);
        }
    }

    public DockerComposeContainer<?> getTestContainer() {
        return testContainer;
    }

    static class DockerComposeContainerImpl extends DockerComposeContainer<DockerComposeContainerImpl> {

        private final String targetDir;

        public DockerComposeContainerImpl(String targetDir, List<File> composeFiles) {
            super(composeFiles);
            this.targetDir = targetDir;
        }

        @Override
        public void stop() {
            super.stop();
        }

        public void cleanup() {
            tryDeleteDir(this.targetDir);
        }
    }
}
