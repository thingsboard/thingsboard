/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

class AttributeScopeTest {
    @ParameterizedTest
    @MethodSource("providedScopes")
    void givenValidScopeStr_whenParseFrom_thenReturnsAttributesScope(String scopeStr, AttributeScope scope) {
        assertThat(AttributeScope.parseFrom(scopeStr)).isEqualTo(scope);
    }

    @Test
    void givenScopeNull_whenParseFrom_thenThrowsException() {
        assertThatThrownBy(() -> AttributeScope.parseFrom(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to parse scope! Provided value is null!");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "scope", "server-scope", "  ", "SHARED-SCOPE", "CLIENT SCOPE"})
    void givenInvalidScope_whenParseFrom_thenThrowsException(String scope) {
        assertThatThrownBy(() -> AttributeScope.parseFrom(scope))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Failed to parse scope! No enum constant for name: " + scope);
    }

    private static Stream<Arguments> providedScopes() {
        return Stream.of(
                Arguments.of("SHARED_SCOPE", AttributeScope.SHARED_SCOPE),
                Arguments.of("SERVER_SCOPE", AttributeScope.SERVER_SCOPE),
                Arguments.of("CLIENT_SCOPE", AttributeScope.CLIENT_SCOPE),
                Arguments.of("  CLIENT_SCOPE ", AttributeScope.CLIENT_SCOPE),
                Arguments.of(" sErver_SCoPE", AttributeScope.SERVER_SCOPE),
                Arguments.of("shared_scope", AttributeScope.SHARED_SCOPE)
        );
    }
}
