// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.SecretType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.dao.secret.SecretService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.exception.DataValidationException;

import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class SecretDataValidator extends DataValidator<Secret> {

    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[^{};\\p{Cntrl}]+$");

    private static final int MAX_FILE_SIZE_BYTES = 512 * 1024; // 0.5 MB
    private static final int MAX_TEXT_SIZE_LENGTH = 2048;

    @Lazy
    private final SecretService secretService;

    @Override
    protected void validateDataImpl(TenantId tenantId, Secret secret) {
        if (!VALID_NAME_PATTERN.matcher(secret.getName()).matches()) {
            throw new DataValidationException("Secret name contains unsupported characters. It must not include '{', '}', ';' or control characters.");
        }
        String value = secret.getValue();
        if (value != null) {
            if (SecretType.TEXT.equals(secret.getType())) {
                if (value.length() > MAX_TEXT_SIZE_LENGTH) {
                    throw new DataValidationException(String.format("Secret value is %d characters; exceeds maximum of %d characters", value.length(), MAX_TEXT_SIZE_LENGTH));
                }
            } else {
                if (value.length() > MAX_FILE_SIZE_BYTES) {
                    throw new DataValidationException(String.format("Secret file size is %d bytes; exceeds the maximum of %d bytes", value.length(), MAX_FILE_SIZE_BYTES));
                }
            }
        }
    }

    @Override
    protected void validateCreate(TenantId tenantId, Secret secret) {
        if (secret.getValue() == null) {
            throw new DataValidationException("Secret must contain value");
        }
    }

    @Override
    protected Secret validateUpdate(TenantId tenantId, Secret secret) {
        Secret old = secretService.findSecretById(tenantId, secret.getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing secret!");
        }
        if (!old.getName().equals(secret.getName())) {
            throw new DataValidationException("Can't update secret name!");
        }
        if (!old.getType().equals(secret.getType())) {
            throw new DataValidationException("Can't update secret type!");
        }
        return old;
    }

}
