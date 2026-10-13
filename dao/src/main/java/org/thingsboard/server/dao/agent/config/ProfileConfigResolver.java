// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.agent.config;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppProfileService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ProfileConfigResolver {

    private final AgentAppProfileService profileService;

    public AgentAppProfile resolve(TenantId tenantId, AgentApplication application) {
        if (application.getApplicationProfileId() == null) {
            return null;
        }
        AgentAppProfile profile = profileService.findProfileById(tenantId, application.getApplicationProfileId());
        if (profile == null || profile.getConfig() == null || !tenantId.equals(profile.getTenantId())) {
            throw new DataValidationException("Couldn't find profile with id: " + application.getApplicationProfileId());
        }
        Map<String, String> incomingCreds = extractCredEnvVars(application);
        AgentAppConfig resolved = profile.getConfig().copy();
        application.setConfig(resolved);
        application.setProfileConfigVersion(profile.getVersion());
        applyCredEnvVars(application, incomingCreds);

        return profile;
    }

    private Map<String, String> extractCredEnvVars(AgentApplication app) {
        List<String> keys = credKeysFor(app);
        if (keys.isEmpty() || !(app.getConfig() instanceof DockerComposeConfig compose) || compose.getCompose() == null) {
            return Map.of();
        }
        var imagePattern = app.getAppType().getMainImagePattern();
        Map<String, String> values = new LinkedHashMap<>();
        for (String key : keys) {
            String value = DockerComposeUtils.getEnvVariable(compose.getCompose(), imagePattern, key);
            if (value != null) {
                values.put(key, value);
            }
        }
        return values;
    }

    private void applyCredEnvVars(AgentApplication app, Map<String, String> creds) {
        if (creds.isEmpty() || !(app.getConfig() instanceof DockerComposeConfig compose) || compose.getCompose() == null) {
            return;
        }
        DockerComposeUtils.setEnvVariables(compose.getCompose(), app.getAppType().getMainImagePattern(), creds);
    }

    private List<String> credKeysFor(AgentApplication app) {
        return app.getAppType() != null ? app.getAppType().getCredentialEnvKeys() : Collections.emptyList();
    }
}
