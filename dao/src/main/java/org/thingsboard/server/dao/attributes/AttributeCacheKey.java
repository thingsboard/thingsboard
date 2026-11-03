// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.attributes;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.thingsboard.server.cache.VersionedCacheKey;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.id.EntityId;

import java.io.Serial;

@EqualsAndHashCode
@Getter
@AllArgsConstructor
public class AttributeCacheKey implements VersionedCacheKey {

    @Serial
    private static final long serialVersionUID = 2013369077925351881L;

    private final AttributeScope scope;
    private final EntityId entityId;
    private final String key;

    @Override
    public String toString() {
        return "{" + entityId + "}" + scope + "_" + key;
    }

    @Override
    public boolean isVersioned() {
        return true;
    }

}
