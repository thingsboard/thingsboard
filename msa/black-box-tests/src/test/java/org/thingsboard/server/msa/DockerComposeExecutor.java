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


import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SystemUtils;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.utility.CommandLine;
import org.zeroturnaround.exec.InvalidExitValueException;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.stream.Collectors.joining;

@Slf4j
public class DockerComposeExecutor {

    String ENV_PROJECT_NAME = "COMPOSE_PROJECT_NAME";
    String ENV_COMPOSE_FILE = "COMPOSE_FILE";

    private static final String COMPOSE_EXECUTABLE = SystemUtils.IS_OS_WINDOWS ? "docker-compose.exe" : "docker-compose";
    private static final String DOCKER_EXECUTABLE = SystemUtils.IS_OS_WINDOWS ? "docker.exe" : "docker";

    private final List<File> composeFiles;
    private final String identifier;
    private String cmd = "";
    private Map<String, String> env = new HashMap<>();

    public DockerComposeExecutor(List<File> composeFiles, String identifier) {
        validateFileList(composeFiles);
        this.composeFiles = composeFiles;
        this.identifier = identifier;
    }

    public DockerComposeExecutor withCommand(String cmd) {
        this.cmd = cmd;
        return this;
    }

    public DockerComposeExecutor withEnv(Map<String, String> env) {
        this.env = env;
        return this;
    }

    public void invokeCompose() {
        // bail out early
        if (!CommandLine.executableExists(COMPOSE_EXECUTABLE)) {
            throw new ContainerLaunchException("Local Docker Compose not found. Is " + COMPOSE_EXECUTABLE + " on the PATH?");
        }
        final Map<String, String> environment = Maps.newHashMap(env);
        environment.put(ENV_PROJECT_NAME, identifier);
        final Stream<String> absoluteDockerComposeFilePaths = composeFiles.stream().map(File::getAbsolutePath).map(Objects::toString);
        final String composeFileEnvVariableValue = absoluteDockerComposeFilePaths.collect(joining(File.pathSeparator + ""));
        log.debug("Set env COMPOSE_FILE={}", composeFileEnvVariableValue);
        final File pwd = composeFiles.get(0).getAbsoluteFile().getParentFile().getAbsoluteFile();
        environment.put(ENV_COMPOSE_FILE, composeFileEnvVariableValue);
        log.info("Local Docker Compose is running command: {}", cmd);
        final List<String> command = Splitter.onPattern(" ").omitEmptyStrings().splitToList(COMPOSE_EXECUTABLE + " " + cmd);
        try {
            new ProcessExecutor().command(command).redirectOutput(Slf4jStream.of(log).asInfo()).redirectError(Slf4jStream.of(log).asError()).environment(environment).directory(pwd).exitValueNormal().executeNoTimeout();
            log.info("Docker Compose has finished running");
        } catch (InvalidExitValueException e) {
            throw new ContainerLaunchException("Local Docker Compose exited abnormally with code " + e.getExitValue() + " whilst running command: " + cmd);
        } catch (Exception e) {
            throw new ContainerLaunchException("Error running local Docker Compose command: " + cmd, e);
        }
    }

    public void invokeDocker() {
        // bail out early
        if (!CommandLine.executableExists(DOCKER_EXECUTABLE)) {
            throw new ContainerLaunchException("Local Docker not found. Is " + DOCKER_EXECUTABLE + " on the PATH?");
        }
        final File pwd = composeFiles.get(0).getAbsoluteFile().getParentFile().getAbsoluteFile();
        log.info("Local Docker is running command: {}", cmd);
        final List<String> command = Splitter.onPattern(" ").omitEmptyStrings().splitToList(DOCKER_EXECUTABLE + " " + cmd);
        try {
            new ProcessExecutor().command(command).redirectOutput(Slf4jStream.of(log).asInfo()).redirectError(Slf4jStream.of(log).asError()).directory(pwd).exitValueNormal().executeNoTimeout();
            log.info("Docker has finished running");
        } catch (InvalidExitValueException e) {
            throw new ContainerLaunchException("Local Docker exited abnormally with code " + e.getExitValue() + " whilst running command: " + cmd);
        } catch (Exception e) {
            throw new ContainerLaunchException("Error running local Docker command: " + cmd, e);
        }
    }

    void validateFileList(List<File> composeFiles) {
        checkNotNull(composeFiles);
        checkArgument(!composeFiles.isEmpty(), "No docker compose file have been provided");
    }


}
