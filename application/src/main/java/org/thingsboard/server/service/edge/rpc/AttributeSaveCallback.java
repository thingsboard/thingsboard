package org.thingsboard.server.service.edge.rpc;

import com.google.common.util.concurrent.FutureCallback;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;

@Slf4j
@AllArgsConstructor
public class AttributeSaveCallback implements FutureCallback<Void> {

    private final TenantId tenantId;
    private final EdgeId edgeId;
    private final String key;
    private final Object value;

    @Override
    public void onSuccess(@Nullable Void result) {
        log.trace("[{}][{}] Successfully updated attribute [{}] with value [{}]", tenantId, edgeId, key, value);
    }

    @Override
    public void onFailure(Throwable t) {
        log.warn("[{}][{}] Failed to update attribute [{}] with value [{}]", tenantId, edgeId, key, value, t);
    }
}
