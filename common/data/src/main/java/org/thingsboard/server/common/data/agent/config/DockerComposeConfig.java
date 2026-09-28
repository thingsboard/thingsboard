// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.agent.AgentApplicationType;
import org.thingsboard.server.common.data.validation.NoXss;
import org.thingsboard.server.exception.DataValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DockerComposeConfig extends AgentAppConfig {

    private JsonNode compose;

    @NoXss
    private String composeType;

    @Override
    public AgentAppConfigType getType() {
        return AgentAppConfigType.DOCKER_COMPOSE;
    }

    @Override
    public AgentAppConfig copy() {
        DockerComposeConfig copy = new DockerComposeConfig();
        copy.setCompose(this.compose != null ? this.compose.deepCopy() : null);
        copy.setComposeType(this.composeType);
        copy.setArguments(copyArguments());
        return copy;
    }

    @Override
    public void validate() {
        if (compose == null || compose.isNull()) {
            throw new DataValidationException("Docker compose config compose content must be specified!");
        }
        List<String> relativeVolumes = DockerComposeUtils.findRelativeVolumeSources(compose);
        if (!relativeVolumes.isEmpty()) {
            throw new DataValidationException("Docker compose config must not reference relative host paths: "
                    + relativeVolumes + ". Use an absolute host path or a named volume.");
        }
        validateArguments();
    }

    @Override
    public void validateForProfile(AgentApplicationType appType) {
        Pattern imagePattern = appType.getMainImagePattern();
        if (imagePattern == null) {
            return;
        }
        JsonNode env = DockerComposeUtils.findServiceEnvironment(compose, imagePattern);
        if (env == null) {
            throw new DataValidationException("Compose config must contain a service matching image pattern: " + imagePattern);
        }
        switch (appType) {
            case EDGE -> requireEnvKeys(env, "CLOUD_ROUTING_KEY", "CLOUD_ROUTING_SECRET", "CLOUD_RPC_HOST");
            case GATEWAY -> validateGatewayCredentialKeys(env);
        }
    }

    private void validateGatewayCredentialKeys(JsonNode env) {
        GatewayEnvSchema schema = GatewayEnvSchema.detectFromEnv(env);
        if (schema == GatewayEnvSchema.LEGACY) {
            validateLegacyGatewayCredentialKeys(env, schema);
            return;
        }
        requireEnvKeys(env, "TB_GW_SECURITY_TYPE");
        String securityType = DockerComposeUtils.envGet(env, "TB_GW_SECURITY_TYPE");
        if (securityType == null || securityType.isBlank()) {
            throw new DataValidationException("Gateway security type (TB_GW_SECURITY_TYPE) must be specified");
        }
        switch (securityType) {
            case "accessToken" -> requireEnvKeys(env, "TB_GW_ACCESS_TOKEN");
            case "usernamePassword" -> requireEnvKeys(env, "TB_GW_CLIENT_ID", "TB_GW_USERNAME", "TB_GW_PASSWORD");
            default -> throw new DataValidationException("Unsupported gateway security type: " + securityType
                    + ". Supported types: accessToken, usernamePassword");
        }
    }

    // legacy gateway images have no security-type variable; the auth type is implied by which credential keys are set
    private void validateLegacyGatewayCredentialKeys(JsonNode env, GatewayEnvSchema schema) {
        boolean accessToken = DockerComposeUtils.envHasKey(env, schema.getAccessTokenKey());
        boolean usernamePassword = DockerComposeUtils.envHasKey(env, schema.getUsernameKey())
                && DockerComposeUtils.envHasKey(env, schema.getPasswordKey());
        if (!accessToken && !usernamePassword) {
            throw new DataValidationException("Compose config is missing required credential environment variables: "
                    + "'accessToken' or 'username'/'password' (legacy gateway env schema)");
        }
    }

    private void requireEnvKeys(JsonNode env, String... keys) {
        List<String> missing = new ArrayList<>();
        for (String key : keys) {
            if (!DockerComposeUtils.envHasKey(env, key)) {
                missing.add(key);
            }
        }
        if (!missing.isEmpty()) {
            throw new DataValidationException("Compose config is missing required credential environment variables: " + missing);
        }
    }

    @Override
    @JsonIgnore
    public String getEdgeRoutingKey() {
        return DockerComposeUtils.getEnvVariable(
                compose, AgentApplicationType.EDGE.getMainImagePattern(), "CLOUD_ROUTING_KEY");
    }
}

