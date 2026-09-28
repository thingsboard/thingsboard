// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AgentArgumentSubstitutorTest {

    @Test
    void substitutesKnownPlaceholder() {
        String compose = "{\"environment\":{\"DEVICE_ID\":\"${tb.device_uuid}\"}}";
        String result = AgentArgumentSubstitutor.substitute(compose, Map.of("device_uuid", "abc-123"), null);
        assertThat(result).isEqualTo("{\"environment\":{\"DEVICE_ID\":\"abc-123\"}}");
    }

    @Test
    void leavesNativeDockerVariablesUntouched() {
        String compose = "{\"environment\":{\"PATH\":\"${PATH}\"}}";
        String result = AgentArgumentSubstitutor.substitute(compose, Map.of("device_uuid", "abc-123"), null);
        assertThat(result).isEqualTo(compose);
    }

    @Test
    void leavesUnknownNamespacedPlaceholderUntouched() {
        String compose = "{\"x\":\"${tb.unknown}\"}";
        String result = AgentArgumentSubstitutor.substitute(compose, Map.of("device_uuid", "abc-123"), null);
        assertThat(result).isEqualTo(compose);
    }

    @Test
    void jsonEscapesValue() {
        String compose = "{\"x\":\"${tb.token}\"}";
        String result = AgentArgumentSubstitutor.substitute(compose, Map.of("token", "a\"b\\c"), null);
        assertThat(result).isEqualTo("{\"x\":\"a\\\"b\\\\c\"}");
    }

    @Test
    void substitutesMultiplePlaceholders() {
        String compose = "{\"a\":\"${tb.one}\",\"b\":\"${tb.two}\"}";
        String result = AgentArgumentSubstitutor.substitute(compose, Map.of("one", "1", "two", "2"), null);
        assertThat(result).isEqualTo("{\"a\":\"1\",\"b\":\"2\"}");
    }

    @Test
    void jsonFormatInjectsRawArrayForWholeValue() {
        String compose = "{\"services\":{\"edge\":{\"ports\":\"${tb.edge_ports}\"}}}";
        String value = "[\"18080:8080\",\"11883:1883\"]";
        String result = AgentArgumentSubstitutor.substitute(compose, Map.of("edge_ports", value),
                List.of(arg("edge_ports", AgentAppArgumentFormat.JSON)));
        assertThat(result).isEqualTo("{\"services\":{\"edge\":{\"ports\":[\"18080:8080\",\"11883:1883\"]}}}");
    }

    @Test
    void jsonFormatFallsBackToQuotedStringWhenNotJson() {
        String compose = "{\"x\":\"${tb.v}\"}";
        String result = AgentArgumentSubstitutor.substitute(compose, Map.of("v", "not-json"),
                List.of(arg("v", AgentAppArgumentFormat.JSON)));
        assertThat(result).isEqualTo("{\"x\":\"not-json\"}");
    }

    @Test
    void jsonFormatEmbeddedPlaceholderStaysString() {
        String compose = "{\"image\":\"repo:${tb.tag}\"}";
        String result = AgentArgumentSubstitutor.substitute(compose, Map.of("tag", "1.0"),
                List.of(arg("tag", AgentAppArgumentFormat.JSON)));
        assertThat(result).isEqualTo("{\"image\":\"repo:1.0\"}");
    }

    @Test
    void stringFormatWholeValueStaysQuotedEvenWhenJsonLike() {
        String compose = "{\"x\":\"${tb.v}\"}";
        String result = AgentArgumentSubstitutor.substitute(compose, Map.of("v", "[\"a\"]"),
                List.of(arg("v", AgentAppArgumentFormat.STRING)));
        assertThat(result).isEqualTo("{\"x\":\"[\\\"a\\\"]\"}");
    }

    @Test
    void returnsContentUnchangedWhenNoArguments() {
        String compose = "{\"x\":\"${tb.one}\"}";
        assertThat(AgentArgumentSubstitutor.substitute(compose, Map.of(), null)).isEqualTo(compose);
        assertThat(AgentArgumentSubstitutor.substitute(compose, null, null)).isEqualTo(compose);
    }

    @Test
    void handlesNullAndEmptyContent() {
        assertThat(AgentArgumentSubstitutor.substitute(null, Map.of("one", "1"), null)).isNull();
        assertThat(AgentArgumentSubstitutor.substitute("", Map.of("one", "1"), null)).isEmpty();
    }

    private AgentAppArgument arg(String name, AgentAppArgumentFormat format) {
        AgentAppArgument argument = new AgentAppArgument();
        argument.setName(name);
        argument.setFormat(format);
        return argument;
    }

}
