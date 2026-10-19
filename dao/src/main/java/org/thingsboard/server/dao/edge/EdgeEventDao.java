// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.edge;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.Dao;

import java.util.UUID;

/**
 * The Interface EdgeEventDao.
 */
public interface EdgeEventDao extends Dao<EdgeEvent> {

    /**
     * Save or update edge event object
     *
     * @param edgeEvent the event object
     * @return saved edge event object future
     */
    ListenableFuture<Void> saveAsync(EdgeEvent edgeEvent);


    /**
     * Find edge events by tenantId, edgeId and pageLink.
     *
     * @param tenantId the tenantId
     * @param edgeId   the edgeId
     * @param seqIdStart  the seq id start
     * @param seqIdEnd  the seq id end
     * @param pageLink the pageLink
     * @return the event list
     */
    PageData<EdgeEvent> findEdgeEvents(UUID tenantId, EdgeId edgeId, Long seqIdStart, Long seqIdEnd, TimePageLink pageLink);

    /**
     * Executes stored procedure to cleanup old edge events.
     * @param ttl the ttl for edge events in seconds
     */
    void cleanupEvents(long ttl);

}
