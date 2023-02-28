/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.testng.Assert.fail;

@Slf4j
public class ContainerTestSuite {
    final static boolean IS_REDIS_CLUSTER = Boolean.parseBoolean(System.getProperty("blackBoxTests.redisCluster"));
    final static boolean IS_HYBRID_MODE = Boolean.parseBoolean(System.getProperty("blackBoxTests.hybridMode"));
    final static String QUEUE_TYPE = System.getProperty("blackBoxTests.queue", "kafka");
    private static final String SOURCE_DIR = "./../../docker/";
    private static final String TB_CORE_LOG_REGEXP = ".*Starting polling for events.*";
    private static final String TRANSPORTS_LOG_REGEXP = ".*Going to recalculate partitions.*";
    private static final String TB_VC_LOG_REGEXP = TRANSPORTS_LOG_REGEXP;
    private static final String TB_JS_EXECUTOR_LOG_REGEXP = ".*template started.*";
    private static final Duration CONTAINER_STARTUP_TIMEOUT = Duration.ofSeconds(400);

    private  DockerComposeContainer<?> testContainer;
    private  ThingsBoardDbInstaller installTb;
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
        installTb = new ThingsBoardDbInstaller();
        installTb.createVolumes();
        log.info("System property of blackBoxTests.redisCluster is {}", IS_REDIS_CLUSTER);
        log.info("System property of blackBoxTests.hybridMode is {}", IS_HYBRID_MODE);
        boolean skipTailChildContainers = Boolean.valueOf(System.getProperty("blackBoxTests.skipTailChildContainers"));
        try {
            final String targetDir = FileUtils.getTempDirectoryPath() + "/" + "ContainerTestSuite-" + UUID.randomUUID() + "/";
            log.info("targetDir {}", targetDir);
            FileUtils.copyDirectory(new File(SOURCE_DIR), new File(targetDir));
            replaceInFile(targetDir + "docker-compose.yml", "    container_name: \"${LOAD_BALANCER_NAME}\"", "", "container_name");

            FileUtils.copyDirectory(new File("src/test/resources"), new File(targetDir));

            class DockerComposeContainerImpl<SELF extends DockerComposeContainer<SELF>> extends DockerComposeContainer<SELF> {
                public DockerComposeContainerImpl(List<File> composeFiles) {
                    super(composeFiles);
                }

                @Override
                public void stop() {
                    super.stop();
                    tryDeleteDir(targetDir);
                }
            }

            List<File> composeFiles = new ArrayList<>(Arrays.asList(
                    new File(targetDir + "docker-compose.yml"),
                    new File(targetDir + "docker-compose.volumes.yml"),
                    new File(targetDir + (IS_HYBRID_MODE ? "docker-compose.hybrid.yml" : "docker-compose.postgres.yml")),
                    new File(targetDir + (IS_HYBRID_MODE ? "docker-compose.hybrid-test-extras.yml" : "docker-compose.postgres-test-extras.yml")),
                    new File(targetDir + "docker-compose.postgres.volumes.yml"),
                    new File(targetDir + "docker-compose." + QUEUE_TYPE + ".yml"),
                    new File(targetDir + (IS_REDIS_CLUSTER ? "docker-compose.redis-cluster.yml" : "docker-compose.redis.yml")),
                    new File(targetDir + (IS_REDIS_CLUSTER ? "docker-compose.redis-cluster.volumes.yml" : "docker-compose.redis.volumes.yml")),
                    new File(targetDir + ("docker-selenium.yml"))
            ));

            Map<String, String> queueEnv = new HashMap<>();
            queueEnv.put("TB_QUEUE_TYPE", QUEUE_TYPE);
            switch (QUEUE_TYPE) {
                case "kafka":
                    composeFiles.add(new File(targetDir + "docker-compose.kafka.yml"));
                    break;
                case "aws-sqs":
                    replaceInFile(targetDir, "queue-aws-sqs.env",
                            Map.of("YOUR_KEY", getSysProp("blackBoxTests.awsKey"),
                                    "YOUR_SECRET", getSysProp("blackBoxTests.awsSecret"),
                                    "YOUR_REGION", getSysProp("blackBoxTests.awsRegion")));
                    break;
                case "rabbitmq":
                    composeFiles.add(new File(targetDir + "docker-compose.rabbitmq-server.yml"));
                    replaceInFile(targetDir, "queue-rabbitmq.env",
                            Map.of("localhost", "rabbitmq"));
                    break;
                case "service-bus":
                    replaceInFile(targetDir, "queue-service-bus.env",
                            Map.of("YOUR_NAMESPACE_NAME", getSysProp("blackBoxTests.serviceBusNamespace"),
                                    "YOUR_SAS_KEY_NAME", getSysProp("blackBoxTests.serviceBusSASPolicy")));
                    replaceInFile(targetDir, "queue-service-bus.env",
                            Map.of("YOUR_SAS_KEY", getSysProp("blackBoxTests.serviceBusPrimaryKey")));
                    break;
                case "pubsub":
                    replaceInFile(targetDir, "queue-pubsub.env",
                            Map.of("YOUR_PROJECT_ID", getSysProp("blackBoxTests.pubSubProjectId"),
                                    "YOUR_SERVICE_ACCOUNT", getSysProp("blackBoxTests.pubSubServiceAccount")));
                    break;
                default:
                    throw new RuntimeException("Unsupported queue type: " + QUEUE_TYPE);
            }

            if (IS_HYBRID_MODE) {
                composeFiles.add(new File(targetDir + "docker-compose.cassandra.volumes.yml"));
            }

            testContainer = new DockerComposeContainerImpl<>(composeFiles)
                    .withPull(false)
                    .withLocalCompose(true)
                    .withTailChildContainers(!skipTailChildContainers)
                    .withEnv(installTb.getEnv())
                    .withEnv(queueEnv)
                    .withEnv("LOAD_BALANCER_NAME", "")
                    .withExposedService("haproxy", 80, Wait.forHttp("/swagger-ui.html").withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-core1", Wait.forLogMessage(TB_CORE_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-core2", Wait.forLogMessage(TB_CORE_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-http-transport1", Wait.forLogMessage(TRANSPORTS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-http-transport2", Wait.forLogMessage(TRANSPORTS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-mqtt-transport1", Wait.forLogMessage(TRANSPORTS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-mqtt-transport2", Wait.forLogMessage(TRANSPORTS_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-vc-executor1", Wait.forLogMessage(TB_VC_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-vc-executor2", Wait.forLogMessage(TB_VC_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT))
                    .waitingFor("tb-js-executor", Wait.forLogMessage(TB_JS_EXECUTOR_LOG_REGEXP, 1).withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));
            testContainer.start();
            setActive(true);
        } catch (Exception e) {
            log.error("Failed to create test container", e);
            fail("Failed to create test container");
        }
    }
    public void stop() {
        if (isActive) {
            testContainer.stop();
            installTb.savaLogsAndRemoveVolumes();
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
            log.error("Can't delete temp directory " + targetDir, e);
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
            log.error("failed to update file " + sourceFilename, e);
            fail("failed to update file");
        }
    }

    public DockerComposeContainer<?> getTestContainer() {
        return testContainer;
    }
}
