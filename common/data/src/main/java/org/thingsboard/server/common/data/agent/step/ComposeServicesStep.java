// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent.step;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.thingsboard.server.common.data.agent.AgentApplication;
import org.thingsboard.server.common.data.agent.config.AgentAppConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeConfig;
import org.thingsboard.server.common.data.agent.config.DockerComposeUtils;
import org.thingsboard.server.common.data.agent.step.state.AgentAppStepState;
import org.thingsboard.server.common.data.agent.step.state.ComposeServicesStepState;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ComposeServicesStep<T extends ComposeServicesStepState> extends StatefulStep<T> {

    public static final String COMPOSE = "compose";
    public static final String SERVICES = "services";

    @Override
    @JsonIgnore
    public Map<String, String> getCommandMetadata(AgentApplication application, @Nullable AgentAppStepState resolvedState) {
        HashMap<String, String> res = new HashMap<>();
        AgentAppConfig config = application.getConfig();
        if (config instanceof DockerComposeConfig d && d.getCompose() != null) {
            if (includeCompose()) {
                res.put(COMPOSE, d.getCompose().toString());
            }
            T base = getState();
            if (base != null) {
                List<String> servicesRegex =
                        base.effectiveValue(ComposeServicesStepState.SERVICES_IMAGE_REGEX_PATTERNS, resolvedState);
                String services = DockerComposeUtils.resolveServiceNames(d.getCompose(), servicesRegex);
                if (!services.isEmpty()) {
                    res.put(SERVICES, services);
                }
            }
        }
        res.putAll(super.getCommandMetadata(application, resolvedState));

        return res;
    }

    /**
     * Whether the raw compose document should be emitted as command metadata. Service-name resolution always reads the
     * compose from {@link DockerComposeConfig}; this only controls the {@link #COMPOSE} output key.
     */
    protected boolean includeCompose() {
        return false;
    }
}
