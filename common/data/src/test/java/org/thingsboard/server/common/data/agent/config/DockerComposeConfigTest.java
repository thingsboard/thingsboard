// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerComposeConfigTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ObjectNode newObjectNode() {
        return MAPPER.createObjectNode();
    }

    @Test
    void testGetEdgeRoutingKey() {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(createEdgeCompose("my-routing-key"));

        assertEquals("my-routing-key", config.getEdgeRoutingKey());
    }

    @Test
    void testGetEdgeRoutingKeyNoEdgeService() {
        ObjectNode env = newObjectNode();
        env.put("SOME_VAR", "value");
        ObjectNode service = newObjectNode();
        service.put("image", "postgres:16");
        service.set("environment", env);
        ObjectNode services = newObjectNode();
        services.set("db", service);
        ObjectNode compose = newObjectNode();
        compose.set("services", services);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);

        assertNull(config.getEdgeRoutingKey());
    }

    @Test
    void testGetEdgeRoutingKeyNoEnvironment() {
        ObjectNode service = newObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        ObjectNode services = newObjectNode();
        services.set("mytbedge", service);
        ObjectNode compose = newObjectNode();
        compose.set("services", services);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);

        assertNull(config.getEdgeRoutingKey());
    }

    @Test
    void testGetEdgeRoutingKeyNoRoutingKeyVar() {
        ObjectNode env = newObjectNode();
        env.put("CLOUD_ROUTING_SECRET", "secret");
        ObjectNode service = newObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        service.set("environment", env);
        ObjectNode services = newObjectNode();
        services.set("mytbedge", service);
        ObjectNode compose = newObjectNode();
        compose.set("services", services);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);

        assertNull(config.getEdgeRoutingKey());
    }

    @Test
    void testGetEdgeRoutingKeyNullCompose() {
        DockerComposeConfig config = new DockerComposeConfig();

        assertNull(config.getEdgeRoutingKey());
    }

    @Test
    void testGetEdgeRoutingKeyMultipleServices() {
        ObjectNode dbService = newObjectNode();
        dbService.put("image", "postgres:16");

        ObjectNode edgeEnv = newObjectNode();
        edgeEnv.put("CLOUD_ROUTING_KEY", "edge-key");
        ObjectNode edgeService = newObjectNode();
        edgeService.put("image", "thingsboard/tb-edge-pe:3.8.0");
        edgeService.set("environment", edgeEnv);

        ObjectNode services = newObjectNode();
        services.set("db", dbService);
        services.set("mytbedge", edgeService);
        ObjectNode compose = newObjectNode();
        compose.set("services", services);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);

        assertEquals("edge-key", config.getEdgeRoutingKey());
    }

    @Test
    void testGetEdgeRoutingKeyFirstEdgeServiceMissingEnvVar() {
        ObjectNode edge1 = newObjectNode();
        edge1.put("image", "thingsboard/tb-edge-pe:3.8.0");
        // no environment block

        ObjectNode edge2Env = newObjectNode();
        edge2Env.put("CLOUD_ROUTING_KEY", "second-key");
        ObjectNode edge2 = newObjectNode();
        edge2.put("image", "thingsboard/tb-edge-pe:3.9.0");
        edge2.set("environment", edge2Env);

        ObjectNode services = newObjectNode();
        services.set("edge1", edge1);
        services.set("edge2", edge2);
        ObjectNode compose = newObjectNode();
        compose.set("services", services);

        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);

        assertEquals("second-key", config.getEdgeRoutingKey());
    }

    // ==================== validate(): relative paths in volumes ====================

    @Test
    void testValidateAbsoluteBindVolume_valid() {
        DockerComposeConfig config = configWithStringVolumes("/opt/data:/var/lib/postgresql/data");

        assertDoesNotThrow(config::validate);
    }

    @Test
    void testValidateNamedVolume_valid() {
        DockerComposeConfig config = configWithStringVolumes("pgdata:/var/lib/postgresql/data");

        assertDoesNotThrow(config::validate);
    }

    @Test
    void testValidateAnonymousVolume_valid() {
        DockerComposeConfig config = configWithStringVolumes("/var/lib/postgresql/data");

        assertDoesNotThrow(config::validate);
    }

    @Test
    void testValidateVariableVolume_valid() {
        DockerComposeConfig config = configWithStringVolumes("${DATA_DIR}:/var/lib/postgresql/data");

        assertDoesNotThrow(config::validate);
    }

    @Test
    void testValidateNoVolumes_valid() {
        ObjectNode service = newObjectNode();
        service.put("image", "postgres:16");
        DockerComposeConfig config = configWithService(service);

        assertDoesNotThrow(config::validate);
    }

    @Test
    void testValidateLongSyntaxBindAbsolute_valid() {
        DockerComposeConfig config = configWithVolumes(longVolume("bind", "/opt/data", "/var/lib/postgresql/data"));

        assertDoesNotThrow(config::validate);
    }

    @Test
    void testValidateLongSyntaxNamedVolume_valid() {
        // a 'volume' type source is a named volume, never a host path - even if it looks relative
        DockerComposeConfig config = configWithVolumes(longVolume("volume", "./pgdata", "/var/lib/postgresql/data"));

        assertDoesNotThrow(config::validate);
    }

    @Test
    void testValidateCurrentDirRelativeVolume_throws() {
        DockerComposeConfig config = configWithStringVolumes("./data:/var/lib/postgresql/data");

        assertThatThrownBy(config::validate)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("relative host paths")
                .hasMessageContaining("./data");
    }

    @Test
    void testValidateParentDirRelativeVolume_throws() {
        DockerComposeConfig config = configWithStringVolumes("../data:/var/lib/postgresql/data");

        assertThatThrownBy(config::validate)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("../data");
    }

    @Test
    void testValidateHomeRelativeVolume_throws() {
        DockerComposeConfig config = configWithStringVolumes("~/data:/var/lib/postgresql/data");

        assertThatThrownBy(config::validate)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("~/data");
    }

    @Test
    void testValidateLongSyntaxBindRelative_throws() {
        DockerComposeConfig config = configWithVolumes(longVolume("bind", "./data", "/var/lib/postgresql/data"));

        assertThatThrownBy(config::validate)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("./data");
    }

    @Test
    void testValidateRelativeVolumeAmongValidOnes_throws() {
        DockerComposeConfig config = configWithStringVolumes(
                "/opt/data:/data", "pgdata:/var/lib", "./logs:/logs");

        assertThatThrownBy(config::validate)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("./logs");
    }

    /** The idiomatic way to ship a config file; it resolves against the agent host's working directory. */
    @Test
    void testValidateRelativeConfigFile_throws() {
        DockerComposeConfig config = configFromJson("""
                {
                  "services": { "nginx": { "image": "nginx:1.27" } },
                  "configs": { "nginx_conf": { "file": "./nginx.conf" } }
                }
                """);

        assertThatThrownBy(config::validate)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("./nginx.conf");
    }

    @Test
    void testValidateRelativeSecretFile_throws() {
        DockerComposeConfig config = configFromJson("""
                {
                  "services": { "nginx": { "image": "nginx:1.27" } },
                  "secrets": { "db_pass": { "file": "secrets/db.txt" } }
                }
                """);

        assertThatThrownBy(config::validate)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("secrets/db.txt");
    }

    /** A named volume backed by a relative bind is still a relative host path. */
    @Test
    void testValidateRelativeDriverOptDevice_throws() {
        DockerComposeConfig config = configFromJson("""
                {
                  "services": { "pg": { "image": "postgres:16" } },
                  "volumes": { "pgdata": { "driver": "local", "driver_opts": { "type": "none", "o": "bind", "device": "./data" } } }
                }
                """);

        assertThatThrownBy(config::validate)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("./data");
    }

    @Test
    void testValidateAbsoluteConfigFileAndDriverOptDevice_ok() {
        DockerComposeConfig config = configFromJson("""
                {
                  "services": { "nginx": { "image": "nginx:1.27" } },
                  "configs": { "nginx_conf": { "file": "/etc/tb/nginx.conf" } },
                  "volumes": { "pgdata": { "driver_opts": { "type": "none", "o": "bind", "device": "/opt/data" } } }
                }
                """);

        config.validate();
    }

    @Test
    void testValidateBareRelativeVolumeWithSeparator_throws() {
        DockerComposeConfig config = configWithStringVolumes("data/logs:/logs");

        assertThatThrownBy(config::validate)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("data/logs");
    }

    @Test
    void testIsRelativeHostPath_bareRelativePathsWithoutLeadingDot() {
        assertTrue(DockerComposeUtils.isRelativeHostPath("data/logs"));
        assertTrue(DockerComposeUtils.isRelativeHostPath("sub/../../etc"));
        assertTrue(DockerComposeUtils.isRelativeHostPath("conf\\nginx.conf"));
    }

    @Test
    void testIsRelativeHostPath_windowsAbsolutePathsAreNotRelative() {
        assertFalse(DockerComposeUtils.isRelativeHostPath("C:\\ProgramData\\tb"));
        assertFalse(DockerComposeUtils.isRelativeHostPath("C:/ProgramData/tb"));
        // a drive letter with no separator after the colon is not an absolute path
        assertTrue(DockerComposeUtils.isRelativeHostPath("C:data/logs"));
    }

    @Test
    void testIsRelativeHostPath_namedVolumesAndVariablesAndAbsolute() {
        assertFalse(DockerComposeUtils.isRelativeHostPath("pgdata"));
        assertFalse(DockerComposeUtils.isRelativeHostPath("${DATA_DIR}/logs"));
        assertFalse(DockerComposeUtils.isRelativeHostPath("/opt/data"));
        assertFalse(DockerComposeUtils.isRelativeHostPath(null));
        assertFalse(DockerComposeUtils.isRelativeHostPath("   "));
    }

    // ==================== validate(): compose content ====================

    @Test
    void testValidateNullCompose_throws() {
        DockerComposeConfig config = new DockerComposeConfig();

        assertThatThrownBy(config::validate)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("compose content must be specified");
    }

    @Test
    void testValidateNullNodeCompose_throws() {
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(NullNode.getInstance());

        assertThatThrownBy(config::validate)
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("compose content must be specified");
    }

    // ==================== copy() ====================

    @Test
    void testCopy_sharesNothingMutableWithTheOriginal() {
        DockerComposeConfig original = configFromJson("""
                {
                  "services": { "mytbedge": { "image": "thingsboard/tb-edge-pe:3.8.0",
                                              "environment": { "CLOUD_ROUTING_KEY": "rk-1" } } }
                }
                """);
        original.setComposeType("default");
        original.setArguments(new ArrayList<>(List.of(argument("device_uuid", "cloud_endpoint"))));

        DockerComposeConfig copy = (DockerComposeConfig) original.copy();

        assertEquals(original, copy);
        assertNotSame(original.getCompose(), copy.getCompose());
        assertNotSame(original.getArguments(), copy.getArguments());
        assertNotSame(original.getArguments().get(0), copy.getArguments().get(0));

        // the merge rules mutate the resolved copy in place, which must not reach the profile's own config
        DockerComposeUtils.upsertEnvVariables(copy.getCompose(),
                AgentApplicationType.EDGE.getMainImagePattern(), Map.of("CLOUD_ROUTING_KEY", "rk-2"));
        copy.getArguments().get(0).setKey("mutated");
        copy.getArguments().add(argument("second", "another_key"));

        assertEquals("rk-1", original.getEdgeRoutingKey());
        assertEquals("cloud_endpoint", original.getArguments().get(0).getKey());
        assertEquals(1, original.getArguments().size());
    }

    @Test
    void testCopy_nullComposeAndArguments() {
        DockerComposeConfig original = new DockerComposeConfig();

        DockerComposeConfig copy = (DockerComposeConfig) original.copy();

        assertNull(copy.getCompose());
        assertNull(copy.getArguments());
    }

    private AgentAppArgument argument(String name, String key) {
        AgentAppArgument argument = new AgentAppArgument();
        argument.setName(name);
        argument.setSourceType(AgentAppArgumentSource.RELATED_ENTITY);
        argument.setValueType(AgentAppArgumentValueType.ATTRIBUTE);
        argument.setKey(key);
        return argument;
    }

    private DockerComposeConfig configFromJson(String json) {
        try {
            DockerComposeConfig config = new DockerComposeConfig();
            config.setCompose(MAPPER.readTree(json));
            return config;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private DockerComposeConfig configWithStringVolumes(String... volumes) {
        ArrayNode volumeArray = MAPPER.createArrayNode();
        for (String volume : volumes) {
            volumeArray.add(volume);
        }
        return configWithVolumeArray(volumeArray);
    }

    private DockerComposeConfig configWithVolumes(ObjectNode... volumes) {
        ArrayNode volumeArray = MAPPER.createArrayNode();
        for (ObjectNode volume : volumes) {
            volumeArray.add(volume);
        }
        return configWithVolumeArray(volumeArray);
    }

    private DockerComposeConfig configWithVolumeArray(ArrayNode volumeArray) {
        ObjectNode service = newObjectNode();
        service.put("image", "postgres:16");
        service.set("volumes", volumeArray);
        return configWithService(service);
    }

    private DockerComposeConfig configWithService(ObjectNode service) {
        ObjectNode services = newObjectNode();
        services.set("db", service);
        ObjectNode compose = newObjectNode();
        compose.set("services", services);
        DockerComposeConfig config = new DockerComposeConfig();
        config.setCompose(compose);
        return config;
    }

    private ObjectNode longVolume(String type, String source, String target) {
        ObjectNode volume = newObjectNode();
        volume.put("type", type);
        volume.put("source", source);
        volume.put("target", target);
        return volume;
    }

    private ObjectNode createEdgeCompose(String routingKey) {
        ObjectNode env = newObjectNode();
        env.put("CLOUD_ROUTING_KEY", routingKey);
        ObjectNode service = newObjectNode();
        service.put("image", "thingsboard/tb-edge-pe:3.8.0");
        service.set("environment", env);
        ObjectNode services = newObjectNode();
        services.set("mytbedge", service);
        ObjectNode compose = newObjectNode();
        compose.set("services", services);
        return compose;
    }
}
