/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
     * Save or update edge event object async
     *
     * @param edgeEvent the event object
     * @return saved edge event object future
     */
    ListenableFuture<EdgeEvent> saveAsync(EdgeEvent edgeEvent);


    /**
     * Find edge events by tenantId, edgeId and pageLink.
     *
     * @param tenantId the tenantId
     * @param edgeId   the edgeId
     * @param pageLink the pageLink
     * @return the event list
     */
    PageData<EdgeEvent> findEdgeEvents(UUID tenantId, EdgeId edgeId, TimePageLink pageLink, boolean withTsUpdate);

}
