package com.iot.thingmodel.admin;

import com.iot.thingmodel.common.ApiException;
import com.iot.thingmodel.common.Constants;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class TenantAccountService {

    private final TenantAccountRepository repository;
    private final CredentialCipher cipher;

    public TenantAccountService(TenantAccountRepository repository, CredentialCipher cipher) {
        this.repository = repository;
        this.cipher = cipher;
    }

    public MaskedAccount register(UUID tenantId, String username, String password) {
        if (tenantId == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, Constants.ErrorCode.TENANT_ID_REQUIRED, "缺少租户 ID");
        }
        if (username == null || username.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    Constants.ErrorCode.USERNAME_REQUIRED,
                    "缺少服务账号用户名");
        }
        if (password == null || password.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    Constants.ErrorCode.PASSWORD_REQUIRED,
                    "缺少服务账号密码");
        }
        String normalizedUsername = username.trim();
        repository.upsert(tenantId, normalizedUsername, cipher.encrypt(password));
        return new MaskedAccount(tenantId, normalizedUsername, true);
    }

    public TenantCredentials credentials(UUID tenantId) {
        TenantAccountRepository.TenantAccountRow row = repository.find(tenantId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        Constants.ErrorCode.TENANT_ACCOUNT_MISSING,
                        "厂商租户未配置 ThingsBoard 服务账号"));
        if (!row.enabled()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    Constants.ErrorCode.TENANT_ACCOUNT_DISABLED,
                    "厂商租户服务账号已停用");
        }
        return new TenantCredentials(row.username(), cipher.decrypt(row.passwordCipher()));
    }

    public Optional<MaskedAccount> findMasked(UUID tenantId) {
        return repository.find(tenantId).map(row -> new MaskedAccount(row.tenantId(), row.username(), row.enabled()));
    }

    public record MaskedAccount(UUID tenantId, String username, boolean enabled) {
    }

    public static final class TenantCredentials {

        private final String username;
        private final String password;

        public TenantCredentials(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String username() {
            return username;
        }

        public String password() {
            return password;
        }

        @Override
        public String toString() {
            return "TenantCredentials{username='" + username
                    + "', password='" + Constants.Auth.MASKED_PASSWORD + "'}";
        }
    }
}
