// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport;

import org.thingsboard.server.common.data.TenantProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.transport.profile.TenantProfileUpdateResult;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.Set;

public interface TransportTenantProfileCache {

    TenantProfile get(TenantId tenantId);

    TenantProfileUpdateResult put(TransportProtos.TenantProfileProto proto);

    boolean put(TenantId tenantId, TenantProfileId profileId);

    Set<TenantId> remove(TenantProfileId profileId);

}
