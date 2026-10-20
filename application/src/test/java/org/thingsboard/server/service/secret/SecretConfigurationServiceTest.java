// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.secret;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.dao.encryptionkey.EncryptionService;
import org.thingsboard.server.dao.secret.SecretService;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class SecretConfigurationServiceTest {

    private Pattern SECRET_PATTERN;

    @Mock
    private SecretService secretService;

    @Mock
    private EncryptionService encryptionService;

    @InjectMocks
    private DefaultSecretConfigurationService secretConfigurationService;

    private TenantId tenantId;

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        tenantId = new TenantId(java.util.UUID.randomUUID());

        Field patternField = DefaultSecretConfigurationService.class.getDeclaredField("SECRET_PATTERN");
        patternField.setAccessible(true);
        SECRET_PATTERN = (Pattern) patternField.get(null);
    }

    @ParameterizedTest
    @MethodSource("provideSecretPatternTestCases")
    public void testSecretPattern(String input, boolean shouldMatch, String expectedName) {
        Matcher matcher = SECRET_PATTERN.matcher(input);
        assertEquals(shouldMatch, matcher.find(), "Matching failed for input: " + input);

        if (shouldMatch) {
            assertEquals(expectedName, matcher.group(1), "Name extraction failed for input: " + input);
        }
    }

    private static Stream<Arguments> provideSecretPatternTestCases() {
        return Stream.of(
                // Valid placeholders with basic Latin characters
                Arguments.of("${secret:mySecret;type:TEXT}", true, "mySecret"),
                Arguments.of("${secret:api_key;type:TEXT_FILE}", true, "api_key"),
                Arguments.of("${secret:db_password;type:TEXT}", true, "db_password"),
                Arguments.of("${secret:token with spaces;type:TEXT_FILE}", true, "token with spaces"),
                Arguments.of("${secret:special!@#$%^&*()_+-=[]|:\"'<>,./?;type:TEXT}", true, "special!@#$%^&*()_+-=[]|:\"'<>,./?"),

                // Valid placeholders with international characters
                Arguments.of("${secret:中文密码;type:TEXT}", true, "中文密码"),
                Arguments.of("${secret:Український_ключ;type:TEXT_FILE}", true, "Український_ключ"),
                Arguments.of("${secret:日本語のパスワード;type:TEXT}", true, "日本語のパスワード"),
                Arguments.of("${secret:한국어 비밀번호;type:TEXT_FILE}", true, "한국어 비밀번호"),
                Arguments.of("${secret:كلمة السر العربية;type:TEXT}", true, "كلمة السر العربية"),

                // Invalid placeholders - wrong format
                Arguments.of("${secret:mySecret:type:TEXT}", false, null),
                Arguments.of("${secret=mySecret;type=TEXT}", false, null),
                Arguments.of("{secret:mySecret;type:TEXT}", false, null),
                Arguments.of("$secret:mySecret;type:TEXT}", false, null),
                Arguments.of("${secret:mySecret;type:TEXT", false, null),

                // Invalid placeholders - prohibited characters
                Arguments.of("${secret:my{Secret;type:TEXT}", false, null),
                Arguments.of("${secret:mySecret};type:TEXT}", false, null)
        );
    }

    @Test
    public void testReplaceSecretUsage_SecretFound() {
        String secretName = "mySecret";
        String input = "${secret:mySecret;type:TEXT}";
        String decryptedValue = "decryptedPassword123";
        byte[] encryptedValue = "encryptedValue" .getBytes();

        Secret secret = new Secret();
        secret.setName(secretName);
        secret.setType(SecretType.TEXT);
        secret.setEncryptedValue(encryptedValue);

        when(secretService.findSecretByName(eq(tenantId), eq(secretName))).thenReturn(secret);
        when(encryptionService.decryptToString(eq(tenantId), eq(SecretType.TEXT), eq(encryptedValue)))
                .thenReturn(decryptedValue);

        String result = secretConfigurationService.replaceSecretUsage(tenantId, input);

        assertEquals(decryptedValue, result);
    }

    @Test
    public void testReplaceSecretUsage_SecretNotFound() {
        String secretName = "missingSecret";
        String input = "${secret:missingSecret;type:TEXT}";

        when(secretService.findSecretByName(eq(tenantId), eq(secretName))).thenReturn(null);

        String result = secretConfigurationService.replaceSecretUsage(tenantId, input);

        assertEquals("", result);
    }

    @Test
    public void testReplaceSecretUsage_NoSecretPattern() {
        String input = "This is a plain string without any secret";

        String result = secretConfigurationService.replaceSecretUsage(tenantId, input);

        assertEquals(input, result);
    }

    @Test
    public void testReplaceSecretUsage_WithTextBeforeAndAfter() {
        String secretName = "apiKey";
        String input = "prefix_${secret:apiKey;type:TEXT}_suffix";
        String decryptedValue = "abc123";
        byte[] encryptedApiKey = "encryptedApiKey" .getBytes();

        Secret secret = new Secret();
        secret.setName(secretName);
        secret.setType(SecretType.TEXT);
        secret.setEncryptedValue(encryptedApiKey);

        when(secretService.findSecretByName(eq(tenantId), eq(secretName))).thenReturn(secret);
        when(encryptionService.decryptToString(eq(tenantId), eq(SecretType.TEXT), eq(encryptedApiKey)))
                .thenReturn(decryptedValue);

        String result = secretConfigurationService.replaceSecretUsage(tenantId, input);

        assertEquals("prefix_abc123_suffix", result);
    }

    @Test
    public void testReplaceSecretUsage_WithSpecialCharactersInReplacement() {
        String secretName = "specialSecret";
        String input = "${secret:specialSecret;type:TEXT}";
        String decryptedValue = "$100 & special\\chars";
        byte[] encryptedSpecial = "encryptedSpecial" .getBytes();

        Secret secret = new Secret();
        secret.setName(secretName);
        secret.setType(SecretType.TEXT);
        secret.setEncryptedValue(encryptedSpecial);

        when(secretService.findSecretByName(eq(tenantId), eq(secretName))).thenReturn(secret);
        when(encryptionService.decryptToString(eq(tenantId), eq(SecretType.TEXT), eq(encryptedSpecial)))
                .thenReturn(decryptedValue);

        String result = secretConfigurationService.replaceSecretUsage(tenantId, input);

        assertEquals(decryptedValue, result);
    }

    @Test
    public void testReplaceSecretUsage_EmptyString() {
        String input = "";

        String result = secretConfigurationService.replaceSecretUsage(tenantId, input);

        assertEquals("", result);
    }

    @Test
    public void testReplaceSecretUsage_WithInternationalCharactersInSecretName() {
        // Arrange
        String secretName = "中文密码";
        String input = "${secret:中文密码;type:TEXT}";
        String decryptedValue = "chinesePassword";
        byte[] encryptedChinese = "encryptedChinese" .getBytes();

        Secret secret = new Secret();
        secret.setName(secretName);
        secret.setType(SecretType.TEXT);
        secret.setEncryptedValue(encryptedChinese);

        when(secretService.findSecretByName(eq(tenantId), eq(secretName))).thenReturn(secret);
        when(encryptionService.decryptToString(eq(tenantId), eq(SecretType.TEXT), eq(encryptedChinese)))
                .thenReturn(decryptedValue);

        String result = secretConfigurationService.replaceSecretUsage(tenantId, input);

        assertEquals(decryptedValue, result);
    }

    @Test
    public void testReplaceSecretUsage_ConnectionStringWithPassword() {
        String secretName = "dbPassword";
        String input = "jdbc:postgresql://localhost:5432/db?password=${secret:dbPassword;type:TEXT}&user=admin";
        String decryptedValue = "mySecurePassword!123";
        byte[] encryptedDbPassword = "encryptedDbPassword" .getBytes();

        Secret secret = new Secret();
        secret.setName(secretName);
        secret.setType(SecretType.TEXT);
        secret.setEncryptedValue(encryptedDbPassword);

        when(secretService.findSecretByName(eq(tenantId), eq(secretName))).thenReturn(secret);
        when(encryptionService.decryptToString(eq(tenantId), eq(SecretType.TEXT), eq(encryptedDbPassword)))
                .thenReturn(decryptedValue);

        String result = secretConfigurationService.replaceSecretUsage(tenantId, input);

        assertEquals("jdbc:postgresql://localhost:5432/db?password=mySecurePassword!123&user=admin", result);
    }

    @Test
    public void testReplaceSecretUsage_MultipleSecrets() {
        String secretName1 = "secret1";
        String secretName2 = "secret2";
        String input = "Test ${secret:secret1;type:TEXT} and ${secret:secret2;type:TEXT} here";
        String decryptedValue1 = "value1";
        String decryptedValue2 = "value2";
        byte[] encrypted1 = "encrypted1" .getBytes();
        byte[] encrypted2 = "encrypted2" .getBytes();

        Secret secret1 = new Secret();
        secret1.setName(secretName1);
        secret1.setType(SecretType.TEXT);
        secret1.setEncryptedValue(encrypted1);

        Secret secret2 = new Secret();
        secret2.setName(secretName2);
        secret2.setType(SecretType.TEXT);
        secret2.setEncryptedValue(encrypted2);

        when(secretService.findSecretByName(eq(tenantId), eq(secretName1))).thenReturn(secret1);
        when(secretService.findSecretByName(eq(tenantId), eq(secretName2))).thenReturn(secret2);
        when(encryptionService.decryptToString(eq(tenantId), eq(SecretType.TEXT), eq(encrypted1)))
                .thenReturn(decryptedValue1);
        when(encryptionService.decryptToString(eq(tenantId), eq(SecretType.TEXT), eq(encrypted2)))
                .thenReturn(decryptedValue2);

        String result = secretConfigurationService.replaceSecretUsage(tenantId, input);

        assertEquals("Test value1 and value2 here", result);
    }

    @Test
    public void testReplaceSecretUsage_TextBeforeSecret() {
        String secretName = "mySecret";
        String input = "Test ${secret:mySecret;type:TEXT}";
        String decryptedValue = "replacedValue";
        byte[] encrypted = "encrypted" .getBytes();

        Secret secret = new Secret();
        secret.setName(secretName);
        secret.setType(SecretType.TEXT);
        secret.setEncryptedValue(encrypted);

        when(secretService.findSecretByName(eq(tenantId), eq(secretName))).thenReturn(secret);
        when(encryptionService.decryptToString(eq(tenantId), eq(SecretType.TEXT), eq(encrypted)))
                .thenReturn(decryptedValue);

        String result = secretConfigurationService.replaceSecretUsage(tenantId, input);

        assertEquals("Test replacedValue", result);
    }

}
