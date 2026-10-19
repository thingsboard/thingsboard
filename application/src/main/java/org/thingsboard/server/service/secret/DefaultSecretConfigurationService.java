// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.secret;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.secret.Secret;
import org.thingsboard.server.dao.encryptionkey.EncryptionService;
import org.thingsboard.server.dao.secret.SecretConfigurationService;
import org.thingsboard.server.dao.secret.SecretService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultSecretConfigurationService implements SecretConfigurationService {

    // To match a placeholder like: ${secret:name;type:type}
    private static final Pattern SECRET_PATTERN = Pattern.compile("\\$\\{secret:([^;{}]+);type:([^;{}]+)}");

    private final SecretService secretService;
    private final EncryptionService encryptionService;

    @Override
    public void replaceSecretUsages(TenantId tenantId, JsonNode config) {
        replaceAllSecretUsages(tenantId, config);
    }

    @Override
    public <T> T replaceSecretUsages(TenantId tenantId, T entity, Class<T> clazz) {
        JsonNode config = JacksonUtil.valueToTree(entity);
        JsonNode replaced = replaceAllSecretUsages(tenantId, config);
        return JacksonUtil.treeToValue(replaced, clazz);
    }

    @Override
    public String replaceSecretUsage(TenantId tenantId, String value) {
        Matcher matcher = SECRET_PATTERN.matcher(value);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String name = matcher.group(1);
            Secret secret = secretService.findSecretByName(tenantId, name);
            String replacement = "";
            if (secret != null) {
                replacement = encryptionService.decryptToString(tenantId, secret.getType(), secret.getEncryptedValue());
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private JsonNode replaceAllSecretUsages(TenantId tenantId, JsonNode config) {
        JacksonUtil.replaceAll(config, "", (path, value) -> replaceSecretUsage(tenantId, value));
        return config;
    }

}
