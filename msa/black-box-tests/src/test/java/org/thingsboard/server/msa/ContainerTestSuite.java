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
    private static final String SOURCE_DIR = "./../../docker/";
    private static final String TB_CORE_LOG_REGEXP = ".*Starting polling for events.*";
    private static final String TRANSPORTS_LOG_REGEXP = ".*Going to recalculate partitions.*";
    private static final String TB_VC_LOG_REGEXP = TRANSPORTS_LOG_REGEXP;
    private static final String TB_EDQS_LOG_REGEXP = ".*All partitions processed.*";
    private static final String TB_JS_EXECUTOR_LOG_REGEXP = ".*template started.*";
    private static final Duration CONTAINER_STARTUP_TIMEOUT = Duration.ofSeconds(400);

    private DockerComposeContainerImpl testContainer;
    private ThingsBoardDbInstaller installTb;
    private boolean isActive;

    private static ContainerTestSuite containerTestSuite;

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    private ContainerTestSuite() {
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
        boolean skipTailChildContainers = Boolean.parseBoolean(System.getProperty("blackBoxTests.skipTailChildContainers"));
        try {
            final String targetDir = FileUtils.getTempDirectoryPath() + "/" + "ContainerTestSuite-" + UUID.randomUUID() + "/";
            log.info("targetDir {}", targetDir);
            FileUtils.copyDirectory(new File(SOURCE_DIR), new File(targetDir));
            replaceInFile(targetDir + "docker-compose.yml", "    container_name: \"${LOAD_BALANCER_NAME}\"", "", "container_name");

            FileUtils.copyDirectory(new File("src/test/resources"), new File(targetDir));

            installTb = new ThingsBoardDbInstaller(targetDir);
            installTb.createVolumes();

            if (IS_VALKEY_SSL) {
                addToFile(targetDir, "cache-valkey.env",
                        Map.of("TB_REDIS_SSL_ENABLED", "true",
                                "TB_REDIS_SSL_PEM_CERT", "/valkey/certs/valkeyCA.crt"));
            }

            List<File> composeFiles = new ArrayList<>(Arrays.asList(
                    new File(targetDir + "docker-compose.yml"),
                    new File(targetDir + "docker-compose.edqs.yml"),
                    new File(targetDir + "docker-compose.edqs.volumes.yml"),
                    new File(targetDir + "docker-compose.volumes.yml"),
                    new File(targetDir + "docker-compose.mosquitto.yml"),
                    new File(targetDir + (IS_HYBRID_MODE ? "docker-compose.hybrid.yml" : "docker-compose.postgres.yml")),
                    new File(targetDir + (IS_HYBRID_MODE ? "docker-compose.hybrid-test-extras.yml" : "docker-compose.postgres-test-extras.yml")),
                    new File(targetDir + "docker-compose.postgres.volumes.yml"),
                    new File(targetDir + "docker-compose.kafka.yml"),
                    new File(targetDir + resolveValkeyComposeFile()),
                    new File(targetDir + resolveValkeyComposeVolumesFile()),
                    new File(targetDir + ("docker-selenium.yml"))
            ));
            addToFile(targetDir, "queue-kafka.env", Map.of("TB_QUEUE_PREFIX", "test"));
            addToFile(targetDir, "tb-edqs.env", Map.of("TB_QUEUE_PREFIX", "test"));

            if (IS_HYBRID_MODE) {
                composeFiles.add(new File(targetDir + "docker-compose.cassandra.volumes.yml"));
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
                    .waitingFor("tb-vc-executor1", Wait.forLogMessage(TB_VC_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-vc-executor2", Wait.forLogMessage(TB_VC_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-js-executor", Wait.forLogMessage(TB_JS_EXECUTOR_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-edqs1", Wait.forLogMessage(TB_EDQS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-edqs2", Wait.forLogMessage(TB_EDQS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));
            testContainer.start();
            setActive(true);
        } catch (Exception e) {
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
        if (isActive) {
            testContainer.stop();
            installTb.saveLogsAndRemoveVolumes();
            testContainer.cleanup();
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
