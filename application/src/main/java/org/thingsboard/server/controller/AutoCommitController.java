// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.controller;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.EntityGroupId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.service.sync.vc.EntitiesVersionControlService;

import java.util.UUID;

public class AutoCommitController extends BaseController {

    @Autowired
    private EntitiesVersionControlService vcService;

    protected ListenableFuture<UUID> autoCommit(User user, EntityId entityId) throws Exception {
        if (vcService != null) {
            return vcService.autoCommit(user, entityId);
        } else {
            // We do not support auto-commit for rule engine
            return Futures.immediateFailedFuture(new RuntimeException("Operation not supported!"));
        }
    }

    protected ListenableFuture<UUID> autoCommit(User user, EntityType entityType, EntityGroupId groupId) throws Exception {
        if (vcService != null) {
            return vcService.autoCommit(user, entityType, groupId);
        } else {
            // We do not support auto-commit for rule engine
            return Futures.immediateFailedFuture(new RuntimeException("Operation not supported!"));
        }
    }

}
