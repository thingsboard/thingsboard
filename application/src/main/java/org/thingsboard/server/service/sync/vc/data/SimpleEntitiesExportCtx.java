package org.thingsboard.server.service.sync.vc.data;

import lombok.Getter;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.vc.request.create.SingleEntityVersionCreateRequest;
import org.thingsboard.server.service.security.model.SecurityUser;

public class SimpleEntitiesExportCtx extends EntitiesExportCtx<SingleEntityVersionCreateRequest> {

    @Getter
    private final EntityExportSettings settings;

    public SimpleEntitiesExportCtx(SecurityUser user, CommitGitRequest commit, SingleEntityVersionCreateRequest request) {
        super(user, commit, request);
        this.settings = request != null ? buildExportSettings(request.getConfig()) : null;
    }
}
