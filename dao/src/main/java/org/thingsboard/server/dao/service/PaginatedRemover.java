/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.dao.service;

import com.google.common.util.concurrent.ListenableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.id.IdBased;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.executors.DbCallbackExecutorService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public abstract class PaginatedRemover<I, D extends IdBased<?>> {
    private static final int DEFAULT_LIMIT = 100;

    @Autowired
    protected DbCallbackExecutorService dbCallbackExecutor;

    public void removeEntities(TenantId tenantId, I id) {
        PageLink pageLink = new PageLink(DEFAULT_LIMIT);
        boolean hasNext = true;
        while (hasNext) {
            PageData<D> entities = findEntities(tenantId, id, pageLink);

            if (dbCallbackExecutor != null) {

                List<ListenableFuture<Void>> entityRemovingResults = new ArrayList<>();

                for (D entity : entities.getData()) {
                    entityRemovingResults.add(
                            dbCallbackExecutor.executeAsync(
                                    () -> removeEntity(tenantId, entity)
                            )
                    );
                }

                List<Throwable> exceptions = new ArrayList<>();

                entityRemovingResults.forEach(e -> {
                            try {
                                e.get();
                            } catch (InterruptedException | ExecutionException ex) {
                                exceptions.add(
                                        ex.getCause()
                                );
                            }
                        }
                );

                if (!exceptions.isEmpty()) {
                    throw new RuntimeException(exceptions.get(exceptions.size() - 1));
                }
            } else {
                for (D entity : entities.getData()) {
                    removeEntity(tenantId, entity);
                }
            }

            hasNext = entities.hasNext();
        }
    }

    protected abstract PageData<D> findEntities(TenantId tenantId, I id, PageLink pageLink);

    protected abstract void removeEntity(TenantId tenantId, D entity);

}