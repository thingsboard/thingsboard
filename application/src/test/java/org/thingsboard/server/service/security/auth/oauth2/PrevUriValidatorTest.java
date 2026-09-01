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
package org.thingsboard.server.service.security.auth.oauth2;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

public class PrevUriValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/",
            "/login",
            "/dashboards/3fa13530-6597-11ed-bd76-8bd591f0ec3e",
            "/dashboards/3fa13530-6597-11ed-bd76-8bd591f0ec3e?state=someState&page=1",
            "/settings/outgoing-mail",
            "/some%20path?q=a+b",
            "/dashboards/3fa13530-6597-11ed-bd76-8bd591f0ec3e?state=W3siaWQiOiJhL2IifV0%3D"
    })
    public void testValidPrevUri(String prevUri) {
        assertThat(PrevUriValidator.isValid(prevUri)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "@evil.com/",
            "evil.com",
            "https://evil.com",
            "//evil.com",
            "/\\evil.com",
            "/\tevil.com",
            "/ evil.com",
            "/dashboards\\..\\evil.com",
            "/login\nLocation: https://evil.com",
            "/login\r\nSet-Cookie: a=b",
            "/dashboards//evil.com",
            "/dashboards%2Fevil.com",
            "/dashboards%5cevil.com",
            "/dashboards;jsessionid=1",
            "/dashboards?title=a,b",
            "/dashboards,list",
            "/dashboards\"list",
            "/dashboards/3fa13530-6597-11ed-bd76-8bd591f0ec3e#fragment",
            "/панель"
    })
    public void testInvalidPrevUri(String prevUri) {
        assertThat(PrevUriValidator.isValid(prevUri)).isFalse();
    }

    @Test
    public void testPrevUriLengthLimit() {
        assertThat(PrevUriValidator.isValid("/" + "a".repeat(2047))).isTrue();
        assertThat(PrevUriValidator.isValid("/" + "a".repeat(2048))).isFalse();
    }

}
