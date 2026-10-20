// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.id.SecretId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willReturn;

@SpringBootTest(classes = SecretDataValidator.class)
class SecretValidatorTest {

    private final TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    private final SecretId secretId = new SecretId(UUID.fromString("060fbe45-fbb2-4549-abf3-f72a6be3cb9f"));

    @MockitoBean
    TenantService tenantService;

    @MockitoBean
    SecretService secretService;

    @MockitoSpyBean
    SecretDataValidator validator;

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void testValidNamePattern(String input, boolean valid) {
        Secret secret = new Secret();
        secret.setName(input);
        secret.setType(SecretType.TEXT);
        secret.setValue("secretValue");
        secret.setTenantId(tenantId);

        if (valid) {
            assertDoesNotThrow(() -> validator.validateDataImpl(tenantId, secret),
                    "Validation should not throw an exception for valid input: " + input);
        } else {
            assertThrows(DataValidationException.class,
                    () -> validator.validateDataImpl(tenantId, secret), "Validation should throw an exception for invalid input: " + input);
        }
    }

    private static Stream<Arguments> provideTestCases() {
        return Stream.of(
                Arguments.of("Simple name", true),
                Arguments.of("Name with numbers 123", true),
                Arguments.of("Name with symbols !@#$%^&*()_+-=[]\\|:\"'<>,./?", true),

                Arguments.of("中文名称", true),
                Arguments.of("Українська назва", true),
                Arguments.of("日本語の名前", true),
                Arguments.of("한국어 이름", true),
                Arguments.of("اسم عربي", true),
                Arguments.of("שם עברי", true),
                Arguments.of("Ελληνικό όνομα", true),
                Arguments.of("Tiếng Việt", true),
                Arguments.of("ไทย", true),

                Arguments.of("Name with {", false),
                Arguments.of("Name with }", false),
                Arguments.of("Name with ;", false),
                Arguments.of("Name with \n", false),
                Arguments.of("Name with \t", false),
                Arguments.of("Name with \r", false),
                Arguments.of("Name with \u0000", false),
                Arguments.of("Name with \u001F", false),

                Arguments.of("中文{name", false),
                Arguments.of("Українська;назва", false)
        );
    }

    @Test
    void testValidateUpdateNonExistingSecret() {
        Secret secret = new Secret(secretId);
        secret.setName("Test Secret");
        secret.setType(SecretType.TEXT);
        secret.setValue("secretValue");
        secret.setTenantId(tenantId);

        given(secretService.findSecretById(tenantId, secretId)).willReturn(null);

        assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, secret),
                "Can't update non existing secret!");
    }

    @Test
    void testValidateUpdateChangedName() {
        Secret oldSecret = new Secret(secretId);
        oldSecret.setName("Old Name");
        oldSecret.setType(SecretType.TEXT);
        oldSecret.setValue("oldSecretValue");
        oldSecret.setTenantId(tenantId);

        Secret newSecret = new Secret(secretId);
        newSecret.setName("New Name");
        newSecret.setType(SecretType.TEXT);
        newSecret.setValue("newSecretValue");
        newSecret.setTenantId(tenantId);

        given(secretService.findSecretById(tenantId, secretId)).willReturn(oldSecret);

        assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newSecret),
                "Can't update secret name!");
    }

    @Test
    void testValidateUpdateChangedType() {
        Secret oldSecret = new Secret(secretId);
        oldSecret.setName("Test Secret");
        oldSecret.setType(SecretType.TEXT);
        oldSecret.setValue("oldSecretValue");
        oldSecret.setTenantId(tenantId);

        Secret newSecret = new Secret(secretId);
        newSecret.setName("Test Secret");
        newSecret.setType(SecretType.TEXT_FILE);
        newSecret.setValue("newSecretValue");
        newSecret.setTenantId(tenantId);

        given(secretService.findSecretById(tenantId, secretId)).willReturn(oldSecret);

        assertThrows(DataValidationException.class,
                () -> validator.validateUpdate(tenantId, newSecret),
                "Can't update secret type!");
    }

    @Test
    void testValidateUpdateValid() {
        Secret oldSecret = new Secret(secretId);
        oldSecret.setName("Test Secret");
        oldSecret.setType(SecretType.TEXT);
        oldSecret.setValue("oldSecretValue");
        oldSecret.setTenantId(tenantId);

        Secret newSecret = new Secret(secretId);
        newSecret.setName("Test Secret");
        newSecret.setType(SecretType.TEXT);
        newSecret.setValue("newSecretValue");
        newSecret.setTenantId(tenantId);

        given(secretService.findSecretById(tenantId, secretId)).willReturn(oldSecret);

        Secret result = validator.validateUpdate(tenantId, newSecret);

        assertEquals(oldSecret, result);
    }

}
