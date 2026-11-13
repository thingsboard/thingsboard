// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.agent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.EntityId;

@Schema
@Data
@EqualsAndHashCode(callSuper = true)
public class AgentApplicationInfo extends AgentApplication {

    @Schema(description = "Current version of the template this application is based on.", accessMode = Schema.AccessMode.READ_ONLY)
    private String currentVersion;

    @Schema(description = "Next version available for upgrade.", accessMode = Schema.AccessMode.READ_ONLY)
    private String nextVersion;

    @Schema(description = "True if the app's config is outdated relative to its profile.", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean profileConfigOutdated;

    @Schema(description = "Name of the application profile this app is based on.", accessMode = Schema.AccessMode.READ_ONLY)
    private String profileName;

    @Schema(description = "Template version of the application profile this app is based on.", accessMode = Schema.AccessMode.READ_ONLY)
    private String profileTemplateVersion;

    @Schema(description = "Name of the owning agent.", accessMode = Schema.AccessMode.READ_ONLY)
    private String agentName;

    @Schema(description = "Related entity id (Edge or Gateway device) currently assigned to this application.", accessMode = Schema.AccessMode.READ_ONLY)
    private EntityId relatedEntityId;

    public AgentApplicationInfo() {
        super();
    }

    public AgentApplicationInfo(AgentApplication application, String currentVersion, String nextVersion) {
        super(application);
        this.currentVersion = currentVersion;
        this.nextVersion = nextVersion;
    }

    public AgentApplicationInfo(AgentApplication application, String currentVersion, String nextVersion, boolean profileConfigOutdated) {
        super(application);
        this.currentVersion = currentVersion;
        this.nextVersion = nextVersion;
        this.profileConfigOutdated = profileConfigOutdated;
    }

}
