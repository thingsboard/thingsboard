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
package org.thingsboard.server.dao.edge;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.service.DataValidator;

@Service
@Slf4j
@AllArgsConstructor
public class BaseEdgeEventService implements EdgeEventService {

    private final EdgeEventDao edgeEventDao;

    private final DataValidator<EdgeEvent> edgeEventValidator;

    @Override
    public ListenableFuture<Void> saveAsync(EdgeEvent edgeEvent) {
        edgeEventValidator.validate(edgeEvent, EdgeEvent::getTenantId);
        return edgeEventDao.saveAsync(edgeEvent);
    }

    @Override
    public PageData<EdgeEvent> findEdgeEvents(TenantId tenantId, EdgeId edgeId, TimePageLink pageLink, boolean withTsUpdate) {
        return findEdgeEvents(tenantId, edgeId, 0L, pageLink, withTsUpdate);
    }

    @Override
    public PageData<EdgeEvent> findEdgeEvents(TenantId tenantId, EdgeId edgeId, Long startSeqId, TimePageLink pageLink, boolean withTsUpdate) {
        return edgeEventDao.findEdgeEvents(tenantId.getId(), edgeId, startSeqId, pageLink, withTsUpdate);
    }

    @Override
    public void cleanupEvents(long ttl) {
        edgeEventDao.cleanupEvents(ttl);
    }
}
