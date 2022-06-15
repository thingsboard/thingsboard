package org.thingsboard.server.service.sync.vc.data;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.sync.ie.EntityExportSettings;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateConfig;
import org.thingsboard.server.common.data.sync.vc.request.create.VersionCreateRequest;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.ArrayList;
import java.util.List;

@Data
public class EntitiesExportCtx<R extends VersionCreateRequest> {

    protected final SecurityUser user;
    protected final CommitGitRequest commit;
    protected final R request;
    private final List<ListenableFuture<Void>> futures = new ArrayList<>();

    public void add(ListenableFuture<Void> future) {
        futures.add(future);
    }

    public TenantId getTenantId() {
        return user.getTenantId();
    }

    protected static EntityExportSettings buildExportSettings(VersionCreateConfig config) {
        return EntityExportSettings.builder()
                .exportRelations(config.isSaveRelations())
                .exportAttributes(config.isSaveAttributes())
                .exportCredentials(config.isSaveCredentials())
                .build();
    }
}
