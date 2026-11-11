// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.role;

import lombok.Data;
import org.thingsboard.server.cache.VersionedCacheKey;
import org.thingsboard.server.common.data.id.RoleId;

import java.io.Serial;

@Data(staticConstructor = "forId")
public class RoleCacheKey implements VersionedCacheKey {

    @Serial
    private static final long serialVersionUID = 3472395528434231465L;

    private final RoleId roleId;

    @Override
    public String toString() {
        return roleId.toString();
    }

    @Override
    public boolean isVersioned() {
        return true;
    }

}
