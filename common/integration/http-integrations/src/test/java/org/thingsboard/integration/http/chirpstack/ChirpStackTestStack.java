// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.http.chirpstack;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Brings up an isolated ChirpStack v4 stack via Testcontainers:
 * postgres -> redis -> mosquitto -> chirpstack -> chirpstack-rest-api.
 *
 * All images are official (Docker Hub: chirpstack/*, postgres, redis, eclipse-mosquitto).
 * Configs live under src/test/resources/chirpstack/ and mirror the official chirpstack-docker
 * compose example.
 *
 * The {@code chirpstack-rest-api} gRPC gateway image is intentionally pinned to a single
 * version (see {@link #CHIRPSTACK_REST_API_IMAGE}) while the {@code chirpstack} server image
 * is parameterized via the constructor. The CS gRPC API surface is stable across the 4.x
 * line, so a fixed gateway version proxies any 4.x server correctly — and reusing one
 * gateway image avoids pulling a separate REST-API image per server version in the
 * compatibility matrix.
 *
 * Lifecycle is explicit (start/stop). Intended for a single @BeforeAll per test class.
 */
@Slf4j
public class ChirpStackTestStack implements AutoCloseable {

    public static final String DEFAULT_CHIRPSTACK_VERSION = "4.18.0";

    private static final String CHIRPSTACK_IMAGE_PREFIX = "chirpstack/chirpstack:";
    private static final String CHIRPSTACK_REST_API_IMAGE = "chirpstack/chirpstack-rest-api:4.18.0";
    private static final String POSTGRES_IMAGE = "postgres:18";
    private static final String REDIS_IMAGE = "redis:7.4.7-alpine";
    private static final String MOSQUITTO_IMAGE = "eclipse-mosquitto:2.0.22";

    private static final int POSTGRES_PORT = 5432;
    private static final int REDIS_PORT = 6379;
    private static final int MOSQUITTO_PORT = 1883;
    private static final int CHIRPSTACK_GRPC_PORT = 8080;
    private static final int CHIRPSTACK_REST_PORT = 8090;

    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(2);

    private final Network network = Network.newNetwork();
    private final String chirpstackVersion;

    public ChirpStackTestStack() {
        this(DEFAULT_CHIRPSTACK_VERSION);
    }

    public ChirpStackTestStack(String chirpstackVersion) {
        this.chirpstackVersion = chirpstackVersion;
    }

    private GenericContainer<?> postgres;
    private GenericContainer<?> redis;
    private GenericContainer<?> mosquitto;
    private GenericContainer<?> chirpstack;
    private GenericContainer<?> chirpstackRest;

    @Getter
    private String restApiUrl;

    @Getter
    private String mqttHost;

    @Getter
    private Integer mqttPort;

    @Getter
    private String apiToken;

    public void start() {
        log.info("Starting ChirpStack test stack...");

        postgres = new GenericContainer<>(POSTGRES_IMAGE)
                .withNetwork(network)
                .withNetworkAliases("postgres")
                .withEnv("POSTGRES_USER", "chirpstack")
                .withEnv("POSTGRES_PASSWORD", "chirpstack")
                .withEnv("POSTGRES_DB", "chirpstack")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("chirpstack/postgres-initdb.sh", 0755),
                        "/docker-entrypoint-initdb.d/001-chirpstack-extensions.sh")
                .withExposedPorts(POSTGRES_PORT)
                .waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\s", 2)
                        .withStartupTimeout(STARTUP_TIMEOUT));
        postgres.start();
        log.info("postgres started");

        redis = new GenericContainer<>(REDIS_IMAGE)
                .withNetwork(network)
                .withNetworkAliases("redis")
                .withExposedPorts(REDIS_PORT)
                .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*\\s", 1)
                        .withStartupTimeout(STARTUP_TIMEOUT));
        redis.start();
        log.info("redis started");

        mosquitto = new GenericContainer<>(MOSQUITTO_IMAGE)
                .withNetwork(network)
                .withNetworkAliases("mosquitto")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("chirpstack/mosquitto.conf"),
                        "/mosquitto/config/mosquitto.conf")
                .withExposedPorts(MOSQUITTO_PORT)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(STARTUP_TIMEOUT));
        mosquitto.start();
        log.info("mosquitto started");

        chirpstack = new GenericContainer<>(CHIRPSTACK_IMAGE_PREFIX + chirpstackVersion)
                .withNetwork(network)
                .withNetworkAliases("chirpstack")
                .withCommand("-c", "/etc/chirpstack")
                .withEnv("POSTGRESQL_HOST", "postgres")
                .withEnv("REDIS_HOST", "redis")
                .withEnv("MQTT_BROKER_HOST", "mosquitto")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("chirpstack/chirpstack.toml"),
                        "/etc/chirpstack/chirpstack.toml")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("chirpstack/region_eu868.toml"),
                        "/etc/chirpstack/region_eu868.toml")
                .withExposedPorts(CHIRPSTACK_GRPC_PORT)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(STARTUP_TIMEOUT));
        chirpstack.start();
        log.info("chirpstack started");

        chirpstackRest = new GenericContainer<>(CHIRPSTACK_REST_API_IMAGE)
                .withNetwork(network)
                .withNetworkAliases("chirpstack-rest-api")
                .withCommand("--server", "chirpstack:8080", "--bind", "0.0.0.0:8090", "--insecure")
                .withExposedPorts(CHIRPSTACK_REST_PORT)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(STARTUP_TIMEOUT));
        chirpstackRest.start();
        log.info("chirpstack-rest-api started");

        restApiUrl = "http://" + chirpstackRest.getHost() + ":" + chirpstackRest.getMappedPort(CHIRPSTACK_REST_PORT);
        mqttHost = mosquitto.getHost();
        mqttPort = mosquitto.getMappedPort(MOSQUITTO_PORT);

        apiToken = createGlobalApiKey();
        log.info("ChirpStack test stack ready. REST API at {}, MQTT at {}:{}", restApiUrl, mqttHost, mqttPort);
    }

    /**
     * Invokes the official `chirpstack create-api-key` CLI inside the running container
     * to produce a global (admin) API token. This is the supported bootstrap path:
     * InternalService.Login is gRPC-only and not exposed via chirpstack-rest-api,
     * so REST clients must authenticate with a pre-created API key.
     */
    private String createGlobalApiKey() {
        try {
            Container.ExecResult result = chirpstack.execInContainer(
                    "chirpstack", "-c", "/etc/chirpstack", "create-api-key", "--name", "test");
            if (result.getExitCode() != 0) {
                throw new IllegalStateException("create-api-key failed: " + result.getStderr());
            }
            String stdout = result.getStdout();
            Matcher m = Pattern.compile("token:\\s*(\\S+)").matcher(stdout);
            if (!m.find()) {
                throw new IllegalStateException("Could not parse token from chirpstack create-api-key output: " + stdout);
            }
            return m.group(1);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create ChirpStack API key", e);
        }
    }

    @Override
    public void close() {
        log.info("Stopping ChirpStack test stack...");
        if (chirpstackRest != null) chirpstackRest.stop();
        if (chirpstack != null) chirpstack.stop();
        if (mosquitto != null) mosquitto.stop();
        if (redis != null) redis.stop();
        if (postgres != null) postgres.stop();
        network.close();
    }

}
