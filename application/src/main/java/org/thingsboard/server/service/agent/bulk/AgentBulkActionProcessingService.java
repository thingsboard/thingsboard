// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.agent.bulk;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.agent.AgentBulkAction;
import org.thingsboard.server.common.data.agent.BulkOperationPreview;
import org.thingsboard.server.common.data.agent.BulkOperationRequest;
import org.thingsboard.server.common.data.id.AgentAppProfileId;
import org.thingsboard.server.common.data.id.AgentProfileId;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.transport.TransportProtos.AgentBulkOperationMsg;

public interface AgentBulkActionProcessingService {

    AgentBulkAction enqueueBulkOperation(TenantId tenantId, AgentProfileId agentProfileId, AgentAppProfileId applicationProfileId, BulkOperationRequest request);

    void processBulkOperation(AgentBulkOperationMsg msg);

    BulkOperationPreview preview(TenantId tenantId, AgentProfileId agentProfileId, AgentAppProfileId applicationProfileId, BulkOperationRequest request);

    /**
     * Runs {@link #preview} on a bounded pool instead of the caller's thread, so a fleet-wide scan does not
     * occupy an HTTP worker for its duration. The action type is validated eagerly, before the hand-off, so an
     * invalid request still fails synchronously; a full queue is rejected with {@code BAD_REQUEST_PARAMS}.
     */
    ListenableFuture<BulkOperationPreview> previewAsync(TenantId tenantId, AgentProfileId agentProfileId,
                                                       AgentAppProfileId applicationProfileId,
                                                       BulkOperationRequest request) throws ThingsboardException;
}
