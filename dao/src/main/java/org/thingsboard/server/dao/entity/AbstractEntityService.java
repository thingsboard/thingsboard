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
package org.thingsboard.server.dao.entity;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.List;
import java.util.Optional;

@Slf4j
public abstract class AbstractEntityService {

    public static final String INCORRECT_EDGE_ID = "Incorrect edgeId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";

    @Autowired
    protected RelationService relationService;

    @Autowired
    protected EntityViewService entityViewService;

    @Autowired(required = false)
    protected EdgeService edgeService;

    protected void createRelation(TenantId tenantId, EntityRelation relation) {
        log.debug("Creating relation: {}", relation);
        relationService.saveRelation(tenantId, relation);
    }

    protected void deleteRelation(TenantId tenantId, EntityRelation relation) {
        log.debug("Deleting relation: {}", relation);
        relationService.deleteRelation(tenantId, relation);
    }

    protected void deleteEntityRelations(TenantId tenantId, EntityId entityId) {
        log.trace("Executing deleteEntityRelations [{}]", entityId);
        relationService.deleteEntityRelations(tenantId, entityId);
    }

    protected Optional<ConstraintViolationException> extractConstraintViolationException(Exception t) {
        if (t instanceof ConstraintViolationException) {
            return Optional.of ((ConstraintViolationException) t);
        } else if (t.getCause() instanceof ConstraintViolationException) {
            return Optional.of ((ConstraintViolationException) (t.getCause()));
        } else {
            return Optional.empty();
        }
    }

    protected void checkAssignedEntityViewsToEdge(TenantId tenantId, EntityId entityId, EdgeId edgeId) {
        try {
            List<EntityView> entityViews = entityViewService.findEntityViewsByTenantIdAndEntityIdAsync(tenantId, entityId).get();
            if (entityViews != null && !entityViews.isEmpty()) {
                EntityView entityView = entityViews.get(0);
                Boolean relationExists = relationService.checkRelation(tenantId,edgeId, entityView.getId(),
                        EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE).get();
                if (relationExists) {
                    throw new DataValidationException("Can't unassign device/asset from edge that is related to entity view and entity view is assigned to edge!");
                }
            }
        } catch (Exception e) {
            log.error("Exception while finding entity views for entityId [{}]", entityId, e);
            throw new RuntimeException("Exception while finding entity views for entityId [" + entityId + "]", e);
        }
    }

}
