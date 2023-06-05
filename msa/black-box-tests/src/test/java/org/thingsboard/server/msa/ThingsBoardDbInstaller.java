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
import org.testcontainers.utility.Base58;
import org.thingsboard.server.common.data.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class ThingsBoardDbInstaller {

    final static boolean IS_REDIS_CLUSTER = Boolean.parseBoolean(System.getProperty("blackBoxTests.redisCluster"));
    final static boolean IS_REDIS_SENTINEL = Boolean.parseBoolean(System.getProperty("blackBoxTests.redisSentinel"));
    final static boolean IS_HYBRID_MODE = Boolean.parseBoolean(System.getProperty("blackBoxTests.hybridMode"));
    private final static String POSTGRES_DATA_VOLUME = "tb-postgres-test-data-volume";

    private final static String CASSANDRA_DATA_VOLUME = "tb-cassandra-test-data-volume";
    private final static String REDIS_DATA_VOLUME = "tb-redis-data-volume";
    private final static String REDIS_CLUSTER_DATA_VOLUME = "tb-redis-cluster-data-volume";
    private final static String REDIS_SENTINEL_DATA_VOLUME = "tb-redis-sentinel-data-volume";
    private final static String TB_LOG_VOLUME = "tb-log-test-volume";
    private final static String TB_COAP_TRANSPORT_LOG_VOLUME = "tb-coap-transport-log-test-volume";
    private final static String TB_LWM2M_TRANSPORT_LOG_VOLUME = "tb-lwm2m-transport-log-test-volume";
    private final static String TB_HTTP_TRANSPORT_LOG_VOLUME = "tb-http-transport-log-test-volume";
    private final static String TB_MQTT_TRANSPORT_LOG_VOLUME = "tb-mqtt-transport-log-test-volume";
    private final static String TB_SNMP_TRANSPORT_LOG_VOLUME = "tb-snmp-transport-log-test-volume";
    private final static String TB_VC_EXECUTOR_LOG_VOLUME = "tb-vc-executor-log-test-volume";
    private final static String JAVA_OPTS = "-Xmx512m";

    private final DockerComposeExecutor dockerCompose;

    private final String postgresDataVolume;
    private final String cassandraDataVolume;

    private final String redisDataVolume;
    private final String redisClusterDataVolume;
    private final String redisSentinelDataVolume;
    private final String tbLogVolume;
    private final String tbCoapTransportLogVolume;
    private final String tbLwm2mTransportLogVolume;
    private final String tbHttpTransportLogVolume;
    private final String tbMqttTransportLogVolume;
    private final String tbSnmpTransportLogVolume;
    private final String tbVcExecutorLogVolume;
    private final Map<String, String> env;

    public ThingsBoardDbInstaller() {
        log.info("System property of blackBoxTests.redisCluster is {}", IS_REDIS_CLUSTER);
        log.info("System property of blackBoxTests.redisCluster is {}", IS_REDIS_SENTINEL);
        log.info("System property of blackBoxTests.hybridMode is {}", IS_HYBRID_MODE);
        List<File> composeFiles = new ArrayList<>(Arrays.asList(
                new File("./../../docker/docker-compose.yml"),
                new File("./../../docker/docker-compose.volumes.yml"),
                IS_HYBRID_MODE
                       ? new File("./../../docker/docker-compose.hybrid.yml")
                       : new File("./../../docker/docker-compose.postgres.yml"),
                new File("./../../docker/docker-compose.postgres.volumes.yml"),
                resolveComposeFile(),
                resolveComposeVolumesFile()
        ));
        if (IS_HYBRID_MODE) {
            composeFiles.add(new File("./../../docker/docker-compose.cassandra.volumes.yml"));
            composeFiles.add(new File("src/test/resources/docker-compose.hybrid-test-extras.yml"));
        } else {
            composeFiles.add(new File("src/test/resources/docker-compose.postgres-test-extras.yml"));
        }

        String identifier = Base58.randomString(6).toLowerCase();
        String project = identifier + Base58.randomString(6).toLowerCase();

        postgresDataVolume = project + "_" + POSTGRES_DATA_VOLUME;
        cassandraDataVolume = project + "_" + CASSANDRA_DATA_VOLUME;
        redisDataVolume = project + "_" + REDIS_DATA_VOLUME;
        redisClusterDataVolume = project + "_" + REDIS_CLUSTER_DATA_VOLUME;
        redisSentinelDataVolume = project + "_" + REDIS_SENTINEL_DATA_VOLUME;
        tbLogVolume = project + "_" + TB_LOG_VOLUME;
        tbCoapTransportLogVolume = project + "_" + TB_COAP_TRANSPORT_LOG_VOLUME;
        tbLwm2mTransportLogVolume = project + "_" + TB_LWM2M_TRANSPORT_LOG_VOLUME;
        tbHttpTransportLogVolume = project + "_" + TB_HTTP_TRANSPORT_LOG_VOLUME;
        tbMqttTransportLogVolume = project + "_" + TB_MQTT_TRANSPORT_LOG_VOLUME;
        tbSnmpTransportLogVolume = project + "_" + TB_SNMP_TRANSPORT_LOG_VOLUME;
        tbVcExecutorLogVolume = project + "_" + TB_VC_EXECUTOR_LOG_VOLUME;

        dockerCompose = new DockerComposeExecutor(composeFiles, project);

        env = new HashMap<>();
        env.put("JAVA_OPTS", JAVA_OPTS);
        env.put("POSTGRES_DATA_VOLUME", postgresDataVolume);
        if (IS_HYBRID_MODE) {
            env.put("CASSANDRA_DATA_VOLUME", cassandraDataVolume);
        }
        env.put("TB_LOG_VOLUME", tbLogVolume);
        env.put("TB_COAP_TRANSPORT_LOG_VOLUME", tbCoapTransportLogVolume);
        env.put("TB_LWM2M_TRANSPORT_LOG_VOLUME", tbLwm2mTransportLogVolume);
        env.put("TB_HTTP_TRANSPORT_LOG_VOLUME", tbHttpTransportLogVolume);
        env.put("TB_MQTT_TRANSPORT_LOG_VOLUME", tbMqttTransportLogVolume);
        env.put("TB_SNMP_TRANSPORT_LOG_VOLUME", tbSnmpTransportLogVolume);
        env.put("TB_VC_EXECUTOR_LOG_VOLUME", tbVcExecutorLogVolume);
        if (IS_REDIS_CLUSTER) {
            for (int i = 0; i < 6; i++) {
                env.put("REDIS_CLUSTER_DATA_VOLUME_" + i, redisClusterDataVolume + '-' + i);
            }
        } else if (IS_REDIS_SENTINEL) {
            env.put("REDIS_SENTINEL_DATA_VOLUME_MASTER", redisSentinelDataVolume + "-" + "master");
            env.put("REDIS_SENTINEL_DATA_VOLUME_SLAVE", redisSentinelDataVolume + "-" + "slave");
            env.put("REDIS_SENTINEL_DATA_VOLUME_SENTINEL", redisSentinelDataVolume + "-" + "sentinel");
        } else {
            env.put("REDIS_DATA_VOLUME", redisDataVolume);
        }
        dockerCompose.withEnv(env);
    }

    private static File resolveComposeVolumesFile() {
        if (IS_REDIS_CLUSTER) {
            return new File("./../../docker/docker-compose.redis-cluster.volumes.yml");
        }
        if (IS_REDIS_SENTINEL) {
            return new File("./../../docker/docker-compose.redis-sentinel.volumes.yml");
        }
        return new File("./../../docker/docker-compose.redis.volumes.yml");
    }

    private static File resolveComposeFile() {
        if (IS_REDIS_CLUSTER) {
            return new File("./../../docker/docker-compose.redis-cluster.yml");
        }
        if (IS_REDIS_SENTINEL) {
            return new File("./../../docker/docker-compose.redis-sentinel.yml");
        }
        return new File("./../../docker/docker-compose.redis.yml");
    }

    public Map<String, String> getEnv() {
        return env;
    }

    public void createVolumes()  {
        try {

            dockerCompose.withCommand("volume create " + postgresDataVolume);
            dockerCompose.invokeDocker();

            if (IS_HYBRID_MODE) {
                dockerCompose.withCommand("volume create " + cassandraDataVolume);
                dockerCompose.invokeDocker();
            }

            dockerCompose.withCommand("volume create " + tbLogVolume);
            dockerCompose.invokeDocker();

            dockerCompose.withCommand("volume create " + tbCoapTransportLogVolume);
            dockerCompose.invokeDocker();

            dockerCompose.withCommand("volume create " + tbLwm2mTransportLogVolume);
            dockerCompose.invokeDocker();

            dockerCompose.withCommand("volume create " + tbHttpTransportLogVolume);
            dockerCompose.invokeDocker();

            dockerCompose.withCommand("volume create " + tbMqttTransportLogVolume);
            dockerCompose.invokeDocker();

            dockerCompose.withCommand("volume create " + tbSnmpTransportLogVolume);
            dockerCompose.invokeDocker();

            dockerCompose.withCommand("volume create " + tbVcExecutorLogVolume);
            dockerCompose.invokeDocker();

            StringBuilder additionalServices = new StringBuilder();
            if (IS_HYBRID_MODE) {
                additionalServices.append(" cassandra");
            }
            if (IS_REDIS_CLUSTER) {
                for (int i = 0; i < 6; i++) {
                    additionalServices.append(" redis-node-").append(i);
                    dockerCompose.withCommand("volume create " + redisClusterDataVolume + '-' + i);
                    dockerCompose.invokeDocker();
                }
            } else if (IS_REDIS_SENTINEL) {
                additionalServices.append(" redis-master");
                dockerCompose.withCommand("volume create " + redisSentinelDataVolume +"-" + "master");
                dockerCompose.invokeDocker();

                additionalServices.append(" redis-slave");
                dockerCompose.withCommand("volume create " + redisSentinelDataVolume + '-' + "slave");
                dockerCompose.invokeDocker();

                additionalServices.append(" redis-sentinel");
                dockerCompose.withCommand("volume create " + redisSentinelDataVolume + '-' + "sentinel");
                dockerCompose.invokeDocker();
            } else {
                additionalServices.append(" redis");
                dockerCompose.withCommand("volume create " + redisDataVolume);
                dockerCompose.invokeDocker();
            }

            dockerCompose.withCommand("up -d postgres" + additionalServices);
            dockerCompose.invokeCompose();

            dockerCompose.withCommand("run --no-deps --rm -e INSTALL_TB=true -e LOAD_DEMO=true tb-core1");
            dockerCompose.invokeCompose();

        } finally {
            try {
                dockerCompose.withCommand("down -v");
                dockerCompose.invokeCompose();
            } catch (Exception ignored) {}
        }
    }

    public void savaLogsAndRemoveVolumes() {
        copyLogs(tbLogVolume, "./target/tb-logs/");
        copyLogs(tbCoapTransportLogVolume, "./target/tb-coap-transport-logs/");
        copyLogs(tbLwm2mTransportLogVolume, "./target/tb-lwm2m-transport-logs/");
        copyLogs(tbHttpTransportLogVolume, "./target/tb-http-transport-logs/");
        copyLogs(tbMqttTransportLogVolume, "./target/tb-mqtt-transport-logs/");
        copyLogs(tbSnmpTransportLogVolume, "./target/tb-snmp-transport-logs/");
        copyLogs(tbVcExecutorLogVolume, "./target/tb-vc-executor-logs/");

        dockerCompose.withCommand("volume rm -f " + postgresDataVolume + " " + tbLogVolume +
                " " + tbCoapTransportLogVolume + " " + tbLwm2mTransportLogVolume + " " + tbHttpTransportLogVolume +
                " " + tbMqttTransportLogVolume + " " + tbSnmpTransportLogVolume + " " + tbVcExecutorLogVolume + resolveComposeVolumeLog());
        dockerCompose.invokeDocker();
    }

    private String resolveComposeVolumeLog() {
        if (IS_REDIS_CLUSTER) {
            return IntStream.range(0, 6).mapToObj(i -> " " + redisClusterDataVolume + "-" + i).collect(Collectors.joining());
        }
        if (IS_REDIS_SENTINEL) {
            return redisSentinelDataVolume + "-" + "master " + " " +
                    redisSentinelDataVolume + "-" + "slave" + " " +
                    redisSentinelDataVolume + " " + "sentinel";
        }
        return redisDataVolume;
    }

    private void copyLogs(String volumeName, String targetDir) {
        File tbLogsDir = new File(targetDir);
        tbLogsDir.mkdirs();

        String logsContainerName = "tb-logs-container-" + StringUtils.randomAlphanumeric(10);

        dockerCompose.withCommand("run -d --rm --name " + logsContainerName + " -v " + volumeName + ":/root alpine tail -f /dev/null");
        dockerCompose.invokeDocker();

        dockerCompose.withCommand("cp " + logsContainerName + ":/root/. "+tbLogsDir.getAbsolutePath());
        dockerCompose.invokeDocker();

        dockerCompose.withCommand("rm -f " + logsContainerName);
        dockerCompose.invokeDocker();
    }

}
