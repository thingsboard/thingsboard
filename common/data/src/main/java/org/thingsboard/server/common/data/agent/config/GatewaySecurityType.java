// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Security types of the IoT Gateway connection to the platform that agent applications support,
 * as written to the gateway's security-type env variable.
 * <p>
 * {@link #TLS_ACCESS_TOKEN} authenticates with the same access token as {@link #ACCESS_TOKEN};
 * the gateway turns TLS on from the port or the CA certificate, not from the security type.
 */
@Getter
@RequiredArgsConstructor
public enum GatewaySecurityType {

    ACCESS_TOKEN("accessToken", true),
    TLS_ACCESS_TOKEN("tlsAccessToken", true),
    USERNAME_PASSWORD("usernamePassword", false);

    private final String value;
    private final boolean tokenBased;

    public List<String> requiredCredentialKeys(GatewayEnvSchema schema) {
        return tokenBased
                ? List.of(schema.getAccessTokenKey())
                : List.of(schema.getClientIdKey(), schema.getUsernameKey(), schema.getPasswordKey());
    }

    @Nullable
    public static GatewaySecurityType fromValue(@Nullable String value) {
        for (GatewaySecurityType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return null;
    }

    public static String supportedValues() {
        return Arrays.stream(values()).map(GatewaySecurityType::getValue).collect(Collectors.joining(", "));
    }
}
