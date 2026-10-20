// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.msg.tools;

import lombok.Getter;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.exception.AbstractRateLimitException;

/**
 * Created by ashvayka on 22.10.18.
 */
public class TbRateLimitsException extends AbstractRateLimitException {
    @Getter
    private final EntityType entityType;

    public TbRateLimitsException(EntityType entityType) {
        super(entityType.name() + " rate limits reached!");
        this.entityType = entityType;
    }

    public TbRateLimitsException(EntityType entityType, String msg) {
        super(msg);
        this.entityType = entityType;
    }

    public TbRateLimitsException(String message) {
        super(message);
        this.entityType = null;
    }

}
