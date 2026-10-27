// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.extension;

import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtensionModeTest {

    @Test
    void fromKeyMapsKebabCaseAndDefaultsToDisabled() {
        assertThat(ExtensionMode.fromKey("docker-compose")).isEqualTo(ExtensionMode.DOCKER_COMPOSE);
        assertThat(ExtensionMode.fromKey("private-cloud")).isEqualTo(ExtensionMode.PRIVATE_CLOUD);
        assertThat(ExtensionMode.fromKey("public-cloud")).isEqualTo(ExtensionMode.PUBLIC_CLOUD);
        assertThat(ExtensionMode.fromKey(" PUBLIC-CLOUD ")).isEqualTo(ExtensionMode.PUBLIC_CLOUD);
        assertThat(ExtensionMode.fromKey(null)).isEqualTo(ExtensionMode.DISABLED);
        assertThat(ExtensionMode.fromKey("")).isEqualTo(ExtensionMode.DISABLED);
    }

    @Test
    void fromKeyRejectsUnknown() {
        assertThatThrownBy(() -> ExtensionMode.fromKey("k8s"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown extensions.mode");
    }

    @Test
    void onlyPublicCloudEnforcesSlugSuffix() {
        assertThat(ExtensionMode.PUBLIC_CLOUD.enforcesSlugSuffix()).isTrue();
        assertThat(ExtensionMode.PRIVATE_CLOUD.enforcesSlugSuffix()).isFalse();
        assertThat(ExtensionMode.DOCKER_COMPOSE.enforcesSlugSuffix()).isFalse();
        assertThat(ExtensionMode.DISABLED.enforcesSlugSuffix()).isFalse();
    }

    @Test
    void getKeyIsKebabCase() {
        assertThat(ExtensionMode.DOCKER_COMPOSE.getKey()).isEqualTo("docker-compose");
        assertThat(ExtensionMode.PUBLIC_CLOUD.getKey()).isEqualTo("public-cloud");
    }

    // The gates and fromKey() must classify every value the same way, or a node boots with the feature
    // "on" and no reconciler bean to inject. Blank is real: 'TB_EXTENSIONS_MODE=' resolves to ''.
    @Test
    void beanGatesMatchFromKeyForEveryNormalizationCase() {
        for (String value : new String[]{"disabled", "DISABLED", " docker-compose ", "private-cloud",
                "PUBLIC-CLOUD", "", "   "}) {
            ExtensionMode mode = ExtensionMode.fromKey(value);
            assertThat(evaluateGate(ExtensionMode.WHEN_ENABLED, value))
                    .as("WHEN_ENABLED for '%s'", value)
                    .isEqualTo(mode != ExtensionMode.DISABLED);
            assertThat(evaluateGate(ExtensionMode.WHEN_DOCKER, value))
                    .as("WHEN_DOCKER for '%s'", value)
                    .isEqualTo(mode == ExtensionMode.DOCKER_COMPOSE);
            assertThat(evaluateGate(ExtensionMode.WHEN_KUBERNETES, value))
                    .as("WHEN_KUBERNETES for '%s'", value)
                    .isEqualTo(mode == ExtensionMode.PRIVATE_CLOUD || mode == ExtensionMode.PUBLIC_CLOUD);
        }
    }

    private static boolean evaluateGate(String gate, String propertyValue) {
        // as Spring does: resolve the placeholder first, run SpEL on the result
        String substituted = gate.replace("${extensions.mode:disabled}", propertyValue);
        return Boolean.TRUE.equals(new SpelExpressionParser().parseExpression(substituted).getValue(Boolean.class));
    }

}
