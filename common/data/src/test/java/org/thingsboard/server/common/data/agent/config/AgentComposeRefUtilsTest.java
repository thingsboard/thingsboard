// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AgentComposeRefUtilsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String COMPOSE = """
            {
              "services": {
                "mytbedge": {
                  "image": "thingsboard/tb-edge-pe:4.3.0EDGEPE",
                  "environment": { "SPRING_DATASOURCE_URL": "jdbc:postgresql://postgres:5432/tb", "TB_QUEUE_TYPE": "in-memory" },
                  "volumes": ["tb-edge-data:/data", "tb-edge-logs:/var/log/tb-edge"]
                },
                "postgres": {
                  "image": "postgres:16"
                }
              }
            }
            """;

    private static JsonNode compose() {
        try {
            return MAPPER.readTree(COMPOSE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void isComposeRefRecognizesOnlyComposeRefs() {
        assertThat(AgentComposeRefUtils.isComposeRef("${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).volumes}")).isTrue();
        assertThat(AgentComposeRefUtils.isComposeRef("${compose.services.postgres.image}")).isTrue();
        assertThat(AgentComposeRefUtils.isComposeRef("tb-edge-data:/data")).isFalse();
        assertThat(AgentComposeRefUtils.isComposeRef("${tb.device_uuid}")).isFalse();
        assertThat(AgentComposeRefUtils.isComposeRef(null)).isFalse();
    }

    @Test
    void resolvesServiceVolumesArrayViaImageRegex() {
        assertThat(AgentComposeRefUtils.resolveAsList(compose(), "${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).volumes}"))
                .containsExactly("tb-edge-data:/data", "tb-edge-logs:/var/log/tb-edge");
    }

    @Test
    void resolvesServiceEnvironmentMapAsKeyValueList() {
        assertThat(AgentComposeRefUtils.resolveAsList(compose(), "${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).environment}"))
                .containsExactlyInAnyOrder("SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/tb", "TB_QUEUE_TYPE=in-memory");
    }

    @Test
    void resolvesScalarViaPlainFieldNavigation() {
        assertThat(AgentComposeRefUtils.resolveAsList(compose(), "${compose.services.postgres.image}"))
                .containsExactly("postgres:16");
    }

    @Test
    void unmatchedRegexResolvesToEmptyListFailOpen() {
        assertThat(AgentComposeRefUtils.resolveAsList(compose(), "${compose.svcImgRegex(nope:.+).volumes}")).isEmpty();
    }

    @Test
    void missingPropertyResolvesToEmptyList() {
        assertThat(AgentComposeRefUtils.resolveAsList(compose(), "${compose.svcImgRegex(postgres:.+).volumes}")).isEmpty();
    }

    @Test
    void nonRefResolvesToNull() {
        assertThat(AgentComposeRefUtils.resolve(compose(), "tb-edge-data:/data")).isNull();
    }

    /** DockerComposeConfig.validate() accepts long-syntax volumes, so they must not resolve to empty strings. */
    @Test
    void resolvesLongSyntaxVolumesToShortForm() {
        String compose = """
                {
                  "services": {
                    "mytbedge": {
                      "image": "thingsboard/tb-edge-pe:4.3.0EDGEPE",
                      "volumes": [
                        { "type": "volume", "source": "pgdata", "target": "/data" },
                        { "type": "bind", "source": "./conf", "target": "/config", "read_only": true },
                        { "type": "tmpfs", "target": "/tmp" }
                      ]
                    }
                  }
                }
                """;
        assertThat(AgentComposeRefUtils.resolveAsList(parse(compose), "${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).volumes}"))
                .containsExactly("pgdata:/data", "./conf:/config:ro", "/tmp");
    }

    @Test
    void skipsListElementsThatCarryNothingUsable() {
        String compose = """
                {
                  "services": {
                    "mytbedge": {
                      "image": "thingsboard/tb-edge-pe:4.3.0EDGEPE",
                      "volumes": [ "ok:/data", { "type": "volume", "source": "pgdata" }, null, [] ]
                    }
                  }
                }
                """;
        assertThat(AgentComposeRefUtils.resolveAsList(parse(compose), "${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).volumes}"))
                .containsExactly("ok:/data");
    }

    /** 'TB_X:' in YAML parses to a null node, which must become an empty value and not the literal "null". */
    @Test
    void resolvesNullEnvironmentValueAsEmptyString() {
        String compose = """
                {
                  "services": {
                    "mytbedge": {
                      "image": "thingsboard/tb-edge-pe:4.3.0EDGEPE",
                      "environment": { "TB_X": null, "TB_Y": "y" }
                    }
                  }
                }
                """;
        assertThat(AgentComposeRefUtils.resolveAsList(parse(compose), "${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).environment}"))
                .containsExactlyInAnyOrder("TB_X=", "TB_Y=y");
    }

    private static JsonNode parse(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
