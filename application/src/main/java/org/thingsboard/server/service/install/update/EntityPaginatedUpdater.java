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
package org.thingsboard.server.service.install.update;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.id.UUIDBased;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.BaseSqlEntity;

public abstract class EntityPaginatedUpdater<I, D extends BaseSqlEntity<? extends SearchTextBased<? extends UUIDBased>>> {
    private static final int DEFAULT_LIMIT = 100;

    public void updateEntities(I id) {
        PageLink pageLink = new PageLink(DEFAULT_LIMIT);
        boolean hasNext = true;
        while (hasNext) {
            Page<D> entities = findEntities(id, DaoUtil.toPageable(pageLink));
            for (D entity : entities.getContent()) {
                updateEntity(entity);
            }
            hasNext = entities.hasNext();
            if (hasNext) {
                pageLink = pageLink.nextPageLink();
            }
        }
    }

    protected abstract Page<D> findEntities(I id, Pageable pageable);

    protected abstract void updateEntity(D entity);
}
