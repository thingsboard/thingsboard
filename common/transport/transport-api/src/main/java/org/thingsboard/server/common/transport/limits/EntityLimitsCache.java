// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.limits;

public interface EntityLimitsCache {

    boolean get(EntityLimitKey key);

    void put(EntityLimitKey key, boolean value);

}
