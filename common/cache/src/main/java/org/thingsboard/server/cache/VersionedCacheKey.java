// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache;

import java.io.Serializable;

public interface VersionedCacheKey extends Serializable {

    default boolean isVersioned() {
        return false;
    }

}
