// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.processor.user;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.gen.edge.v1.UserCredentialsUpdateMsg;
import org.thingsboard.server.gen.edge.v1.UserUpdateMsg;
import org.thingsboard.server.service.edge.rpc.processor.EdgeProcessor;

public interface UserProcessor extends EdgeProcessor {

    ListenableFuture<Void> processUserMsgFromEdge(TenantId tenantId, Edge edge, UserUpdateMsg userUpdateMsg);

    ListenableFuture<Void> processUserCredentialsMsgFromEdge(TenantId tenantId, Edge edge, UserCredentialsUpdateMsg userCredentialsUpdateMsg);

}
