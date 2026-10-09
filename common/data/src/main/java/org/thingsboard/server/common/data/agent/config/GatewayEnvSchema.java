// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.config;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Names of the connectivity environment variables understood by a tb-gateway image.
 * <p>
 * Gateways 3.6.0+ use the {@code TB_GW_*} names ({@link #MODERN}); older images only read the
 * unprefixed names ({@link #LEGACY}) and have no security-type variable at all — the auth type is
 * inferred from which credential variables are set. The schema of a particular compose is
 * {@link #detect(JsonNode, Pattern) detected} from the env keys already present in it, since the
 * per-version compose templates (agent-app-templates repo) carry the style matching their gateway line.
 */
@Getter
@RequiredArgsConstructor
public enum GatewayEnvSchema {

    MODERN("TB_GW_HOST", "TB_GW_PORT", "TB_GW_SECURITY_TYPE", "TB_GW_ACCESS_TOKEN", "TB_GW_CLIENT_ID", "TB_GW_USERNAME", "TB_GW_PASSWORD"),
    LEGACY("host", "port", null, "accessToken", "clientId", "username", "password");

    private final String hostKey;
    private final String portKey;
    @Nullable
    private final String securityTypeKey;
    private final String accessTokenKey;
    private final String clientIdKey;
    private final String usernameKey;
    private final String passwordKey;

    public List<String> credentialKeys() {
        return List.of(accessTokenKey, clientIdKey, usernameKey, passwordKey);
    }

    /**
     * Detects the schema of the gateway service env in the given compose. MODERN wins when markers
     * of both schemas are present; composes without recognizable markers default to MODERN.
     */
    public static GatewayEnvSchema detect(JsonNode compose, Pattern imagePattern) {
        return detectFromEnv(DockerComposeUtils.findServiceEnvironment(compose, imagePattern));
    }

    /**
     * Same as {@link #detect(JsonNode, Pattern)} but for an already-resolved environment node.
     */
    public static GatewayEnvSchema detectFromEnv(@Nullable JsonNode env) {
        if (env == null) {
            return MODERN;
        }
        for (GatewayEnvSchema schema : values()) {
            if (DockerComposeUtils.envHasKey(env, schema.hostKey)
                    || DockerComposeUtils.envHasKey(env, schema.accessTokenKey)
                    || DockerComposeUtils.envHasKey(env, schema.usernameKey)) {
                return schema;
            }
        }
        return MODERN;
    }
}
