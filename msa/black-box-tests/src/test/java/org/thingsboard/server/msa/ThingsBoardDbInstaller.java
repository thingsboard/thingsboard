/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import org.junit.rules.ExternalResource;
import org.testcontainers.utility.Base58;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ThingsBoardDbInstaller extends ExternalResource {

    private final static String POSTGRES_DATA_VOLUME = "tb-postgres-test-data-volume";
    private final static String TB_LOG_VOLUME = "tb-log-test-volume";

    private final DockerComposeExecutor dockerCompose;

    private final String postgresDataVolume;
    private final String tbLogVolume;

    public ThingsBoardDbInstaller() {
        List<File> composeFiles = Arrays.asList(new File("./../../docker/docker-compose.yml"),
                new File("./../../docker/docker-compose.postgres.yml"),
                new File("./../../docker/docker-compose.postgres.volumes.yml"));

        String identifier = Base58.randomString(6).toLowerCase();
        String project = identifier + Base58.randomString(6).toLowerCase();

        postgresDataVolume = project + "_" + POSTGRES_DATA_VOLUME;
        tbLogVolume = project + "_" + TB_LOG_VOLUME;

        dockerCompose = new DockerComposeExecutor(composeFiles, project);

        Map<String, String> env = new HashMap<>();
        env.put("POSTGRES_DATA_VOLUME", postgresDataVolume);
        env.put("TB_LOG_VOLUME", tbLogVolume);
        dockerCompose.withEnv(env);
    }

    public String getPostgresDataVolume() {
        return postgresDataVolume;
    }

    public String getTbLogVolume() {
        return tbLogVolume;
    }

    @Override
    protected void before() throws Throwable {
        try {

            dockerCompose.withCommand("volume create " + postgresDataVolume);
            dockerCompose.invokeDocker();

            dockerCompose.withCommand("volume create " + tbLogVolume);
            dockerCompose.invokeDocker();

            dockerCompose.withCommand("up -d redis postgres");
            dockerCompose.invokeCompose();

            dockerCompose.withCommand("run --no-deps --rm -e INSTALL_TB=true -e LOAD_DEMO=true tb1");
            dockerCompose.invokeCompose();

        } finally {
            try {
                dockerCompose.withCommand("down -v");
                dockerCompose.invokeCompose();
            } catch (Exception e) {}
        }
    }

    @Override
    protected void after() {
        copyLogs(tbLogVolume, "./target/tb-logs/");

        dockerCompose.withCommand("volume rm -f " + postgresDataVolume + " " + tbLogVolume);
        dockerCompose.invokeDocker();
    }

    private void copyLogs(String volumeName, String targetDir) {
        File tbLogsDir = new File(targetDir);
        tbLogsDir.mkdirs();

        dockerCompose.withCommand("run -d --rm --name tb-logs-container -v " + volumeName + ":/root alpine tail -f /dev/null");
        dockerCompose.invokeDocker();

        dockerCompose.withCommand("cp tb-logs-container:/root/. "+tbLogsDir.getAbsolutePath());
        dockerCompose.invokeDocker();

        dockerCompose.withCommand("rm -f tb-logs-container");
        dockerCompose.invokeDocker();
    }

}
