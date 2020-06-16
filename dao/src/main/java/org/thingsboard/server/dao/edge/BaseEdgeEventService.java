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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;

@Service
@Slf4j
public class BaseEdgeEventService implements EdgeEventService {

    @Autowired
    public EdgeEventDao edgeEventDao;

    @Override
    public EdgeEventType getEdgeEventTypeByEntityType(EntityType entityType) {
        switch (entityType) {
            case DEVICE:
                return EdgeEventType.DEVICE;
            case ASSET:
                return EdgeEventType.ASSET;
            case ENTITY_VIEW:
                return EdgeEventType.ENTITY_VIEW;
            case DASHBOARD:
                return EdgeEventType.DASHBOARD;
            case USER:
                return EdgeEventType.USER;
            default:
                log.warn("Failed to push notification to edge service. Unsupported entity type [{}]", entityType);
                return null;
        }
    }

    @Override
    public ListenableFuture<EdgeEvent> saveAsync(EdgeEvent edgeEvent) {
        edgeEventValidator.validate(edgeEvent, EdgeEvent::getTenantId);
        return edgeEventDao.saveAsync(edgeEvent);
    }

    @Override
    public TimePageData<EdgeEvent> findEdgeEvents(TenantId tenantId, EdgeId edgeId, TimePageLink pageLink) {
        List<EdgeEvent> events = edgeEventDao.findEdgeEvents(tenantId.getId(), edgeId, pageLink);
        return new TimePageData<>(events, pageLink);
    }

    private DataValidator<EdgeEvent> edgeEventValidator =
            new DataValidator<EdgeEvent>() {
                @Override
                protected void validateDataImpl(TenantId tenantId, EdgeEvent edgeEvent) {
                    if (edgeEvent.getEdgeId() == null) {
                        throw new DataValidationException("Edge id should be specified!");
                    }
                    if (StringUtils.isEmpty(edgeEvent.getEdgeEventAction())) {
                        throw new DataValidationException("Edge Event action should be specified!");
                    }
                }
            };
}
