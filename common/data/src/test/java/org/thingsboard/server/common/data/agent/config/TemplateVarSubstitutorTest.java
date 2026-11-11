// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TemplateVarSubstitutorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<String, Object> VARS = Map.of(
            "version", "4.3.1.2EDGEPE",
            "composeLine", "4.3",
            "requiresUpdateDb", true,
            "retries", 3,
            "ratio", 1.5);

    @Test
    void wholeStringTokenEmitsTypedNodes() {
        JsonNode result = substitute("""
                {
                  "bool": "${var.requiresUpdateDb}",
                  "long": "${var.retries}",
                  "double": "${var.ratio}",
                  "text": "${var.version}"
                }
                """);

        assertThat(result.get("bool").isBoolean()).isTrue();
        assertThat(result.get("bool").booleanValue()).isTrue();
        assertThat(result.get("long").isNumber()).isTrue();
        assertThat(result.get("long").longValue()).isEqualTo(3L);
        assertThat(result.get("double").isNumber()).isTrue();
        assertThat(result.get("double").doubleValue()).isEqualTo(1.5);
        assertThat(result.get("text").isTextual()).isTrue();
        assertThat(result.get("text").textValue()).isEqualTo("4.3.1.2EDGEPE");
    }

    @Test
    void embeddedTokenIsReplacedTextually() {
        JsonNode result = substitute("""
                {
                  "path": "compose/edge/${var.composeLine}/docker-compose.yml",
                  "image": "thingsboard/tb-edge-pe:${var.version}",
                  "flag": "db=${var.requiresUpdateDb}"
                }
                """);

        assertThat(result.get("path").textValue()).isEqualTo("compose/edge/4.3/docker-compose.yml");
        assertThat(result.get("image").textValue()).isEqualTo("thingsboard/tb-edge-pe:4.3.1.2EDGEPE");
        assertThat(result.get("flag").isTextual()).isTrue();
        assertThat(result.get("flag").textValue()).isEqualTo("db=true");
    }

    @Test
    void negationInvertsTruthiness() {
        JsonNode result = substitute("""
                {
                  "whole": "${var.!requiresUpdateDb}",
                  "embedded": "skipDb=${var.!requiresUpdateDb}",
                  "nonBoolean": "${var.!version}"
                }
                """);

        assertThat(result.get("whole").isBoolean()).isTrue();
        assertThat(result.get("whole").booleanValue()).isFalse();
        assertThat(result.get("embedded").textValue()).isEqualTo("skipDb=false");
        assertThat(result.get("nonBoolean").booleanValue()).isTrue();
    }

    @Test
    void unknownVariableIsLeftAsALiteralPlaceholder() {
        JsonNode result = substitute("""
                {
                  "whole": "${var.unknown}",
                  "embedded": "prefix-${var.unknown}-suffix",
                  "mixed": "${var.composeLine}/${var.unknown}"
                }
                """);

        assertThat(result.get("whole").textValue()).isEqualTo("${var.unknown}");
        assertThat(result.get("embedded").textValue()).isEqualTo("prefix-${var.unknown}-suffix");
        assertThat(result.get("mixed").textValue()).isEqualTo("4.3/${var.unknown}");
    }

    @Test
    void nullVariableValueBecomesJsonNull() {
        Map<String, Object> vars = new HashMap<>();
        vars.put("nextVersion", null);

        JsonNode result = TemplateVarSubstitutor.substitute(
                readTree("{\"next\": \"${var.nextVersion}\", \"label\": \"next=${var.nextVersion}\"}"), vars);

        assertThat(result.get("next").isNull()).isTrue();
        assertThat(result.get("label").textValue()).isEqualTo("next=null");
    }

    @Test
    void composeReferencesAreLeftIntact() {
        JsonNode result = substitute("""
                {"binds": ["${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).volumes}"]}
                """);

        assertThat(result.get("binds").get(0).textValue())
                .isEqualTo("${compose.svcImgRegex(thingsboard/tb-edge-pe:.+).volumes}");
    }

    @Test
    void nestedObjectsAndArraysAreVisited() {
        JsonNode result = substitute("""
                {"steps": [{"state": {"image": {"value": "tb:${var.version}"}}}]}
                """);

        assertThat(result.at("/steps/0/state/image/value").textValue()).isEqualTo("tb:4.3.1.2EDGEPE");
    }

    @Test
    void inputNodeIsNotMutated() {
        JsonNode input = readTree("{\"image\": \"tb:${var.version}\"}");

        JsonNode result = TemplateVarSubstitutor.substitute(input, VARS);

        assertThat(input.get("image").textValue()).isEqualTo("tb:${var.version}");
        assertThat(result.get("image").textValue()).isEqualTo("tb:4.3.1.2EDGEPE");
    }

    @Test
    void nullTemplateReturnsNull() {
        assertThat(TemplateVarSubstitutor.substitute(null, VARS)).isNull();
    }

    private static JsonNode substitute(String json) {
        return TemplateVarSubstitutor.substitute(readTree(json), VARS);
    }

    private static JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }
}
