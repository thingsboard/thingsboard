// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install.update;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;

@Slf4j
public abstract class PaginatedUpdater<I, D> {

    private static final int DEFAULT_LIMIT = 100;
    private int updated = 0;

    public void updateEntities(I id) {
        updated = 0;
        PageLink pageLink = new PageLink(DEFAULT_LIMIT);
        boolean hasNext = true;
        while (hasNext) {
            PageData<D> entities = findEntities(id, pageLink);
            for (D entity : entities.getData()) {
                updateEntity(entity);
            }
            updated += entities.getData().size();
            hasNext = entities.hasNext();
            if (hasNext) {
                log.info("{}: {} entities updated so far...", getName(), updated);
                pageLink = pageLink.nextPageLink();
            } else {
                if (updated > DEFAULT_LIMIT || forceReportTotal()) {
                    log.info("{}: {} total entities updated.", getName(), updated);
                }
            }
        }
    }

    public void updateEntities() {
        updateEntities(null);
    }

    protected boolean forceReportTotal() {
        return false;
    }

    protected abstract String getName();

    protected abstract PageData<D> findEntities(I id, PageLink pageLink);

    protected abstract void updateEntity(D entity);

}
