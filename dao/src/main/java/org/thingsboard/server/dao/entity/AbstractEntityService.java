/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.thingsboard.common.util.DebugModeUtil;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.HasDebugSettings;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.housekeeper.CleanUpService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class AbstractEntityService {

    public static final String INCORRECT_EDGE_ID = "Incorrect edgeId ";
    public static final String INCORRECT_PAGE_LINK = "Incorrect page link ";

    @Autowired
    protected ApplicationEventPublisher eventPublisher;

    @Lazy
    @Autowired
    protected RelationService relationService;

    @Lazy
    @Autowired
    protected AlarmService alarmService;

    @Lazy
    @Autowired
    protected EntityViewService entityViewService;

    @Lazy
    @Autowired
    protected CalculatedFieldService calculatedFieldService;

    @Lazy
    @Autowired(required = false)
    protected EdgeService edgeService;

    @Autowired
    @Lazy
    protected CleanUpService cleanUpService;

    @Autowired
    @Lazy
    private TbTenantProfileCache tbTenantProfileCache;

    @Value("${debug.settings.default_duration:15}")
    private int defaultDebugDurationMinutes;

    protected void createRelation(TenantId tenantId, EntityRelation relation) {
        log.debug("Creating relation: {}", relation);
        relationService.saveRelation(tenantId, relation);
    }

    protected void deleteRelation(TenantId tenantId, EntityRelation relation) {
        log.debug("Deleting relation: {}", relation);
        relationService.deleteRelation(tenantId, relation);
    }

    protected static Optional<ConstraintViolationException> extractConstraintViolationException(Exception t) {
        if (t instanceof ConstraintViolationException) {
            return Optional.of((ConstraintViolationException) t);
        } else if (t.getCause() instanceof ConstraintViolationException) {
            return Optional.of((ConstraintViolationException) (t.getCause()));
        } else {
            return Optional.empty();
        }
    }

    public static final void checkConstraintViolation(Exception t, String constraintName, String constraintMessage) {
        checkConstraintViolation(t, Collections.singletonMap(constraintName, constraintMessage));
    }

    public static final void checkConstraintViolation(Exception t, String constraintName1, String constraintMessage1, String constraintName2, String constraintMessage2) {
        checkConstraintViolation(t, Map.of(constraintName1, constraintMessage1, constraintName2, constraintMessage2));
    }

    public static final void checkConstraintViolation(Exception t, Map<String, String> constraints) {
        var exOpt = extractConstraintViolationException(t);
        if (exOpt.isPresent()) {
            var ex = exOpt.get();
            if (StringUtils.isNotEmpty(ex.getConstraintName())) {
                var constraintName = ex.getConstraintName();
                for (var constraintMessage : constraints.entrySet()) {
                    if (constraintName.equals(constraintMessage.getKey())) {
                        throw new DataValidationException(constraintMessage.getValue());
                    }
                }
            }
        }
    }

    protected void checkAssignedEntityViewsToEdge(TenantId tenantId, EntityId entityId, EdgeId edgeId) {
        List<EntityView> entityViews = entityViewService.findEntityViewsByTenantIdAndEntityId(tenantId, entityId);
        if (entityViews != null && !entityViews.isEmpty()) {
            EntityView entityView = entityViews.get(0);
            boolean relationExists = relationService.checkRelation(
                    tenantId, edgeId, entityView.getId(),
                    EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE
            );
            if (relationExists) {
                throw new DataValidationException("Can't unassign device/asset from edge that is related to entity view and entity view is assigned to edge!");
            }
        }
    }

    protected void updateDebugSettings(TenantId tenantId, HasDebugSettings entity, long now) {
        if (entity.getDebugSettings() != null) {
            entity.setDebugSettings(entity.getDebugSettings().copy(getMaxDebugAllUntil(tenantId, now)));
        } else if (entity.isDebugMode()) {
            entity.setDebugSettings(DebugSettings.failuresOrUntil(getMaxDebugAllUntil(tenantId, now)));
            entity.setDebugMode(false);
        }
    }

    private long getMaxDebugAllUntil(TenantId tenantId, long now) {
        return now + TimeUnit.MINUTES.toMillis(DebugModeUtil.getMaxDebugAllDuration(tbTenantProfileCache.get(tenantId).getDefaultProfileConfiguration().getMaxDebugModeDurationMinutes(), defaultDebugDurationMinutes));
    }
}
