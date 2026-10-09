// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.integration;

import lombok.Data;
import org.thingsboard.server.cache.VersionedCacheKey;
import org.thingsboard.server.common.data.id.IntegrationId;

import java.io.Serial;

@Data(staticConstructor = "forId")
public class IntegrationCacheKey implements VersionedCacheKey {

    @Serial
    private static final long serialVersionUID = 3472395528434231465L;

    private final IntegrationId integrationId;

    @Override
    public String toString() {
        return integrationId.toString();
    }

    @Override
    public boolean isVersioned() {
        return true;
    }

}
