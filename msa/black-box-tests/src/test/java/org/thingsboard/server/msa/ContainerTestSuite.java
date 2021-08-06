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
package org.thingsboard.server.msa;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({"org.thingsboard.server.msa.*Test"})
@Slf4j
public class ContainerTestSuite {

    private static DockerComposeContainer<?> testContainer;

    @ClassRule
    public static ThingsBoardDbInstaller installTb = new ThingsBoardDbInstaller();

    @ClassRule
    public static DockerComposeContainer getTestContainer() {
        if (testContainer == null) {
            boolean skipTailChildContainers = Boolean.valueOf(System.getProperty("blackBoxTests.skipTailChildContainers"));
            try {
                String tbCoreLogRegexp = ".*Starting polling for events.*";
                String transportsLogRegexp = ".*Going to recalculate partitions.*";

                testContainer = new DockerComposeContainer<>(
                        new File(removeContainerName("./../../docker/docker-compose.yml")),
                        new File("./../../docker/docker-compose.postgres.yml"),
                        new File("./../../docker/docker-compose.postgres.volumes.yml"),
                        new File("./../../docker/docker-compose.kafka.yml"))
                        .withPull(false)
                        .withLocalCompose(true)
                        .withTailChildContainers(!skipTailChildContainers)
                        .withEnv(installTb.getEnv())
                        .withEnv("LOAD_BALANCER_NAME", "")
                        .withExposedService("haproxy", 80, Wait.forHttp("/swagger-ui.html").withStartupTimeout(Duration.ofSeconds(400)))
                        .waitingFor("tb-core1", Wait.forLogMessage(tbCoreLogRegexp, 1).withStartupTimeout(Duration.ofSeconds(400)))
                        .waitingFor("tb-core2", Wait.forLogMessage(tbCoreLogRegexp, 1).withStartupTimeout(Duration.ofSeconds(400)))
                        .waitingFor("tb-http-transport1", Wait.forLogMessage(transportsLogRegexp, 1).withStartupTimeout(Duration.ofSeconds(400)))
                        .waitingFor("tb-http-transport2", Wait.forLogMessage(transportsLogRegexp, 1).withStartupTimeout(Duration.ofSeconds(400)))
                        .waitingFor("tb-mqtt-transport1", Wait.forLogMessage(transportsLogRegexp, 1).withStartupTimeout(Duration.ofSeconds(400)))
                        .waitingFor("tb-mqtt-transport2", Wait.forLogMessage(transportsLogRegexp, 1).withStartupTimeout(Duration.ofSeconds(400)));
            } catch (Exception e) {
                log.error("Failed to create test container", e);
                throw e;
            }
        }
        return testContainer;
    }

    /**
     * This workaround is actual until issue will be resolved:
     * Support container_name in docker-compose file #2472 https://github.com/testcontainers/testcontainers-java/issues/2472
     * docker-compose files which contain container_name are not supported and the creation of DockerComposeContainer fails due to IllegalStateException.
     * This has been introduced in #1151 as a quick fix for unintuitive feedback. https://github.com/testcontainers/testcontainers-java/issues/1151
     * Using the latest testcontainers and waiting for the fix...
     * */
    private static String removeContainerName(String sourceFilename) {
        String outputFilename = null;
        try {
            String sourceContent = FileUtils.readFileToString(new File(sourceFilename), StandardCharsets.UTF_8);
            String outputContent = sourceContent.replace("container_name: \"${LOAD_BALANCER_NAME}\"", "");
            assertThat(outputContent, (not(containsString("container_name"))));

            Path tempFile = Files.createTempFile("docker-compose", ".yml"); // the file looks like /tmp/docker-compose713972234379430232.yml
            log.info("tempFile is {}", tempFile.toFile().getAbsolutePath());

            FileUtils.writeStringToFile(tempFile.toFile(), outputContent, StandardCharsets.UTF_8);
            outputFilename = tempFile.toFile().getAbsolutePath();
            assertThat(FileUtils.readFileToString(new File(outputFilename), StandardCharsets.UTF_8), is(outputContent));

        } catch (IOException e) {
            Assert.fail("failed to create tmp file " + e.getMessage());
        }
        return outputFilename;
    }
}
