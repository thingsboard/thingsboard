// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state;

public interface HasEntityLimit {

    default void checkEntityLimit(int currentEntitiesCount, CalculatedFieldCtx ctx) {
        if (currentEntitiesCount >= ctx.getMaxRelatedEntitiesPerCfArgument()) {
            throw new IllegalArgumentException(
                    "Exceeded the maximum allowed related entities per argument '"
                    + ctx.getMaxRelatedEntitiesPerCfArgument() + "'. Increase the limit in the tenant profile configuration."
            );
        }
    }

}
