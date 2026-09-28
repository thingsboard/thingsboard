// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.encryptionkey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.encryptionkey.EncryptionKey;
import org.thingsboard.server.common.data.id.TenantId;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EncryptionServiceImplTest {

    private final TenantId tenantId = TenantId.fromUUID(UUID.fromString("9114e9ac-6c28-4019-a2a7-b948cb9500d5"));

    @Mock
    private EncryptionKeyDao encryptionKeyDao;

    @InjectMocks
    private EncryptionServiceImpl encryptionService;

    private static Stream<Arguments> encryptionTestData() {
        return Stream.of(
                Arguments.of("simple-text", "password1", "0123456789abcdef"),
                Arguments.of("complex!@#$%^&*()_+", "password2", "abcdef0123456789"),
                Arguments.of("unicode-text-тест-测试", "password3", "1a2b3c4d5e6f7a8b"),
                Arguments.of("", "password4", "a1b2c3d4e5f6a7b8")
        );
    }

    private static Stream<String> valuesToEncryptTestData() {
        return Stream.of(
                "Simple text value",
                "Complex value with symbols: !@#$%^&*()",
                "Value with numbers: 1234567890",
                "Unicode characters: тест-测试-こんにちは",
                "Very long text value that exceeds normal length and contains multiple words and sentences",
                "JSON format: {\"key\": \"value\", \"nested\": {\"array\": [1, 2, 3]}}",
                "XML format: <root><child attr=\"value\">content</child></root>",
                "Value with special whitespace\ttabs\nand\nnewlines",
                "",
                "Short"
        );
    }

    private EncryptionKey createCustomEncryptionKey(String password, String salt) {
        EncryptionKey key = new EncryptionKey();
        key.setTenantId(tenantId);
        key.setPassword(password);
        key.setSalt(salt);
        return key;
    }

    @Test
    public void testEncrypt() {
        when(encryptionKeyDao.findByTenantId(tenantId)).thenReturn(null);

        ArgumentCaptor<EncryptionKey> keyCaptor = ArgumentCaptor.forClass(EncryptionKey.class);

        encryptionService.createEncryptionKey(tenantId);

        verify(encryptionKeyDao).save(eq(tenantId), keyCaptor.capture());

        EncryptionKey createdKey = keyCaptor.getValue();
        when(encryptionKeyDao.findByTenantId(tenantId)).thenReturn(createdKey);

        byte[] result = encryptionService.encrypt(tenantId, SecretType.TEXT, "testString".getBytes(StandardCharsets.UTF_8));

        assertThat(result).isNotNull();
        assertThat(result.length).isGreaterThan(0);

        assertThat(createdKey.getTenantId()).isEqualTo(tenantId);
        assertThat(createdKey.getPassword()).isNotNull();
        assertThat(createdKey.getSalt()).isNotNull();
    }

    @ParameterizedTest
    @MethodSource("encryptionTestData")
    public void testEncryptParameterized(String testString, String password, String salt) {
        byte[] testBytes = testString.getBytes(StandardCharsets.UTF_8);

        EncryptionKey customKey = createCustomEncryptionKey(password, salt);

        when(encryptionKeyDao.findByTenantId(tenantId)).thenReturn(customKey);

        byte[] result = encryptionService.encrypt(tenantId, SecretType.TEXT, testBytes);

        assertThat(result).isNotNull();

        assertThat(result.length).isGreaterThan(0);

        assertThat(customKey.getTenantId()).isEqualTo(tenantId);
        assertThat(customKey.getPassword()).isEqualTo(password);
        assertThat(customKey.getSalt()).isEqualTo(salt);
    }

    @Test
    public void testDecryptToString() {
        String testString = "This is a test string";
        byte[] testBytes = testString.getBytes(StandardCharsets.UTF_8);
        when(encryptionKeyDao.findByTenantId(tenantId)).thenReturn(null);

        ArgumentCaptor<EncryptionKey> keyCaptor = ArgumentCaptor.forClass(EncryptionKey.class);

        encryptionService.createEncryptionKey(tenantId);

        verify(encryptionKeyDao).save(eq(tenantId), keyCaptor.capture());

        EncryptionKey createdKey = keyCaptor.getValue();
        when(encryptionKeyDao.findByTenantId(tenantId)).thenReturn(createdKey);

        byte[] encrypted = encryptionService.encrypt(tenantId, SecretType.TEXT, testBytes);

        String result = encryptionService.decryptToString(tenantId, SecretType.TEXT, encrypted);

        assertThat(result).isEqualTo(testString);
    }

    @ParameterizedTest
    @MethodSource("encryptionTestData")
    public void testDecryptToStringParameterized(String testString, String password, String salt) {
        byte[] testBytes = testString.getBytes(StandardCharsets.UTF_8);

        EncryptionKey customKey = createCustomEncryptionKey(password, salt);

        when(encryptionKeyDao.findByTenantId(tenantId)).thenReturn(customKey);

        byte[] encrypted = encryptionService.encrypt(tenantId, SecretType.TEXT, testBytes);

        String result = encryptionService.decryptToString(tenantId, SecretType.TEXT, encrypted);

        assertThat(result).isEqualTo(testString);

        assertThat(customKey.getTenantId()).isEqualTo(tenantId);
        assertThat(customKey.getPassword()).isEqualTo(password);
        assertThat(customKey.getSalt()).isEqualTo(salt);
    }

    @Test
    public void testCreateEncryptionKey_whenKeyDoesNotExist() {
        when(encryptionKeyDao.findByTenantId(tenantId)).thenReturn(null);

        encryptionService.createEncryptionKey(tenantId);

        ArgumentCaptor<EncryptionKey> keyCaptor = ArgumentCaptor.forClass(EncryptionKey.class);
        verify(encryptionKeyDao).save(eq(tenantId), keyCaptor.capture());
        EncryptionKey savedKey = keyCaptor.getValue();
        assertThat(savedKey.getTenantId()).isEqualTo(tenantId);
        assertThat(savedKey.getPassword()).isNotNull();
        assertThat(savedKey.getSalt()).isNotNull();
    }

    @Test
    public void testCreateEncryptionKey_whenKeyExists() {
        EncryptionKey existingKey = new EncryptionKey();
        existingKey.setTenantId(tenantId);
        existingKey.setPassword("existing-password");
        existingKey.setSalt("existing-salt");
        when(encryptionKeyDao.findByTenantId(tenantId)).thenReturn(existingKey);

        encryptionService.createEncryptionKey(tenantId);

        verify(encryptionKeyDao, never()).save(any(), any());
    }

    @ParameterizedTest
    @MethodSource("valuesToEncryptTestData")
    public void testEncryptDecryptSameValue(String valueToEncrypt) throws Exception {
        // Generate random password and salt using the methods from EncryptionService
        String password = generateSecurePassword();
        String salt = generateSalt();

        byte[] valueBytes = valueToEncrypt.getBytes(StandardCharsets.UTF_8);

        EncryptionKey customKey = createCustomEncryptionKey(password, salt);
        when(encryptionKeyDao.findByTenantId(tenantId)).thenReturn(customKey);

        byte[] encryptedValue = encryptionService.encrypt(tenantId, SecretType.TEXT, valueBytes);
        assertThat(encryptedValue).isNotNull();
        assertThat(encryptedValue.length).isGreaterThan(0);

        String decryptedValue = encryptionService.decryptToString(tenantId, SecretType.TEXT, encryptedValue);
        assertThat(decryptedValue).isEqualTo(valueToEncrypt);

        assertThat(customKey.getTenantId()).isEqualTo(tenantId);
        assertThat(customKey.getPassword()).isEqualTo(password);
        assertThat(customKey.getSalt()).isEqualTo(salt);
    }

    private String generateSecurePassword() throws Exception {
        java.lang.reflect.Method method = EncryptionServiceImpl.class.getDeclaredMethod("generateSecurePassword");
        method.setAccessible(true);
        return (String) method.invoke(encryptionService);
    }

    private String generateSalt() throws Exception {
        java.lang.reflect.Method method = EncryptionServiceImpl.class.getDeclaredMethod("generateSalt");
        method.setAccessible(true);
        return (String) method.invoke(encryptionService);
    }

}
