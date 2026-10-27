package com.iot.thingmodel.admin;

import com.iot.thingmodel.common.ApiException;
import com.iot.thingmodel.common.Constants;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantAccountServiceTest {

    private final TenantAccountRepository repository = mock(TenantAccountRepository.class);
    private final CredentialCipher cipher = mock(CredentialCipher.class);
    private final TenantAccountService service = new TenantAccountService(repository, cipher);
    private final UUID tenantId = UUID.randomUUID();

    @Test
    void rejectsMissingTenantId() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.register(null, "user", "password"));

        assertEquals(Constants.ErrorCode.TENANT_ID_REQUIRED, ex.getCode());
    }

    @Test
    void rejectsBlankUsername() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.register(tenantId, " ", "password"));

        assertEquals(Constants.ErrorCode.USERNAME_REQUIRED, ex.getCode());
    }

    @Test
    void rejectsBlankPassword() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.register(tenantId, "user", " "));

        assertEquals(Constants.ErrorCode.PASSWORD_REQUIRED, ex.getCode());
    }

    @Test
    void registersEncryptedAccount() {
        when(cipher.encrypt("secret")).thenReturn("cipher");

        TenantAccountService.MaskedAccount account =
                service.register(tenantId, " vendor@example.com ", "secret");

        assertEquals(tenantId, account.tenantId());
        assertEquals("vendor@example.com", account.username());
        assertTrue(account.enabled());
        verify(repository).upsert(tenantId, "vendor@example.com", "cipher");
    }

    @Test
    void rejectsMissingCredentials() {
        when(repository.find(tenantId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.credentials(tenantId));

        assertEquals(Constants.ErrorCode.TENANT_ACCOUNT_MISSING, ex.getCode());
    }

    @Test
    void rejectsDisabledAccount() {
        when(repository.find(tenantId)).thenReturn(Optional.of(
                new TenantAccountRepository.TenantAccountRow(
                        tenantId, "user", "cipher", false, Instant.now(), Instant.now())));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.credentials(tenantId));

        assertEquals(Constants.ErrorCode.TENANT_ACCOUNT_DISABLED, ex.getCode());
    }

    @Test
    void decryptsCredentials() {
        when(repository.find(tenantId)).thenReturn(Optional.of(
                new TenantAccountRepository.TenantAccountRow(
                        tenantId, "user", "cipher", true, Instant.now(), Instant.now())));
        when(cipher.decrypt("cipher")).thenReturn("secret");

        TenantAccountService.TenantCredentials credentials = service.credentials(tenantId);

        assertEquals("user", credentials.username());
        assertEquals("secret", credentials.password());
        assertTrue(credentials.toString().contains("***"));
        assertFalse(credentials.toString().contains("secret"));
    }

    @Test
    void findMaskedReturnsAccount() {
        when(repository.find(tenantId)).thenReturn(Optional.of(
                new TenantAccountRepository.TenantAccountRow(
                        tenantId, "user", "cipher", true, Instant.now(), Instant.now())));

        TenantAccountService.MaskedAccount account = service.findMasked(tenantId).orElseThrow();

        assertEquals("user", account.username());
        assertTrue(account.enabled());
    }
}
