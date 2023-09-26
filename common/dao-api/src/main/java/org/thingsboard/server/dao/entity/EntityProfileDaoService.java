/**
 * Copyright © 2016-2023 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.entity;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Comparator;
import java.util.List;

public interface EntityProfileDaoService extends EntityDaoService {

    /**
     * Gets a functional supplier for retrieving entity profile names asynchronously.
     *
     * @return An implementation of {@link EntityProfileDaoSupplier} that allows fetching entity profile
     *         names with tenant ID and activeOnly flag as parameters.
     */
    EntityProfileDaoSupplier getEntityProfileDaoSupplier();

    /**
     * Asynchronously retrieves a list of entity subtypes representing the names of entity profiles
     * that belongs to the specified tenant.
     *
     * @param tenantId   The UUID of the tenant for which to retrieve device profile names.
     * @param activeOnly Flag indicating whether to retrieve exclusively the names of entity profiles that are associated with entities.
     *
     * @return A ListenableFuture containing a list of EntitySubtype objects representing the names of
     *         entity profiles that belongs to the specified tenant.
     */
    ListenableFuture<List<EntitySubtype>> findEntityProfileNamesByTenantId(TenantId tenantId, boolean activeOnly);

    default ListenableFuture<List<EntitySubtype>> doProcessFindEntityProfileNamesByTenantId(TenantId tenantId, boolean activeOnly) {
        ListenableFuture<List<EntitySubtype>> profileNames = getEntityProfileDaoSupplier()
                .getEntityProfilesNames(tenantId.getId(), activeOnly);
        return Futures.transform(profileNames,
                profileNamesList -> {
                    profileNamesList.sort(Comparator.comparing(EntitySubtype::getType));
                    return profileNamesList;
                }, MoreExecutors.directExecutor());
    }

}
