/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.cf;

import com.google.common.util.concurrent.FluentFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CalculatedFieldLinkId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.service.DataValidator;

import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Service("CalculatedFieldDaoService")
@Slf4j
@RequiredArgsConstructor
public class BaseCalculatedFieldService extends AbstractEntityService implements CalculatedFieldService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CALCULATED_FIELD_ID = "Incorrect calculatedFieldId ";
    public static final String INCORRECT_ENTITY_ID = "Incorrect entityId ";

    private final CalculatedFieldDao calculatedFieldDao;
    private final CalculatedFieldLinkDao calculatedFieldLinkDao;
    private final DataValidator<CalculatedField> calculatedFieldDataValidator;
    private final DataValidator<CalculatedFieldLink> calculatedFieldLinkDataValidator;

    @Override
    public CalculatedField save(CalculatedField calculatedField) {
        CalculatedField oldCalculatedField = calculatedFieldDataValidator.validate(calculatedField, CalculatedField::getTenantId);
        return doSave(calculatedField, oldCalculatedField);
    }

    @Override
    public CalculatedField save(CalculatedField calculatedField, boolean doValidate) {
        CalculatedField oldCalculatedField = null;
        if (doValidate) {
            oldCalculatedField = calculatedFieldDataValidator.validate(calculatedField, CalculatedField::getTenantId);
        } else if (calculatedField.getId() != null) {
            oldCalculatedField = findById(calculatedField.getTenantId(), calculatedField.getId());
        }
        return doSave(calculatedField, oldCalculatedField);
    }


    private CalculatedField doSave(CalculatedField calculatedField, CalculatedField oldCalculatedField) {
        try {
            TenantId tenantId = calculatedField.getTenantId();
            log.trace("Executing save calculated field, [{}]", calculatedField);
            updateDebugSettings(tenantId, calculatedField, System.currentTimeMillis());
            CalculatedField savedCalculatedField = calculatedFieldDao.save(tenantId, calculatedField);
            createOrUpdateCalculatedFieldLink(tenantId, savedCalculatedField);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedCalculatedField.getTenantId()).entityId(savedCalculatedField.getId())
                    .entity(savedCalculatedField).oldEntity(oldCalculatedField).created(calculatedField.getId() == null).build());
            return savedCalculatedField;
        } catch (Exception e) {
            checkConstraintViolation(e,
                    "calculated_field_unq_key", "Calculated Field with such name is already in exists!",
                    "calculated_field_external_id_unq_key", "Calculated Field with such external id already exists!");
            throw e;
        }
    }

    @Override
    public CalculatedField findById(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        log.trace("Executing findById, tenantId [{}], calculatedFieldId [{}]", tenantId, calculatedFieldId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(calculatedFieldId, id -> INCORRECT_CALCULATED_FIELD_ID + id);
        return calculatedFieldDao.findById(tenantId, calculatedFieldId.getId());
    }

    @Override
    public CalculatedField findByEntityIdAndName(EntityId entityId, String name) {
        log.trace("Executing findByEntityIdAndName [{}], calculatedFieldName[{}]", entityId, name);
        validateId(entityId.getId(), id -> INCORRECT_ENTITY_ID + id);
        return calculatedFieldDao.findByEntityIdAndName(entityId, name);
    }

    @Override
    public List<CalculatedFieldId> findCalculatedFieldIdsByEntityId(TenantId tenantId, EntityId entityId) {
        log.trace("Executing findCalculatedFieldIdsByEntityId [{}]", entityId);
        validateId(entityId.getId(), id -> INCORRECT_ENTITY_ID + id);
        return calculatedFieldDao.findCalculatedFieldIdsByEntityId(tenantId, entityId);
    }

    @Override
    public List<CalculatedField> findCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId) {
        log.trace("Executing findCalculatedFieldsByEntityId [{}]", entityId);
        validateId(entityId.getId(), id -> INCORRECT_ENTITY_ID + id);
        return calculatedFieldDao.findCalculatedFieldsByEntityId(tenantId, entityId);
    }

    @Override
    public PageData<CalculatedField> findAllCalculatedFields(PageLink pageLink) {
        log.trace("Executing findAll, pageLink [{}]", pageLink);
        validatePageLink(pageLink);
        return calculatedFieldDao.findAll(pageLink);
    }

    @Override
    public PageData<CalculatedField> findCalculatedFieldsByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAllByTenantId, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return calculatedFieldDao.findAllByTenantId(tenantId, pageLink);
    }

    @Override
    public PageData<CalculatedField> findAllCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId, PageLink pageLink) {
        log.trace("Executing findAllByEntityId, entityId [{}], pageLink [{}]", entityId, pageLink);
        validateId(entityId.getId(), id -> INCORRECT_ENTITY_ID + id);
        validatePageLink(pageLink);
        return calculatedFieldDao.findAllByEntityId(tenantId, entityId, pageLink);
    }

    @Override
    public void deleteCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(calculatedFieldId, id -> INCORRECT_CALCULATED_FIELD_ID + id);
        deleteEntity(tenantId, calculatedFieldId, false);
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        CalculatedField calculatedField = calculatedFieldDao.findById(tenantId, id.getId());
        if (calculatedField == null) {
            if (force) {
                return;
            } else {
                throw new IncorrectParameterException("Unable to delete non-existent calculated field.");
            }
        }
        deleteCalculatedField(tenantId, calculatedField);
    }

    private void deleteCalculatedField(TenantId tenantId, CalculatedField calculatedField) {
        log.trace("Executing deleteCalculatedField, tenantId [{}], calculatedFieldId [{}]", tenantId, calculatedField.getId());
        calculatedFieldDao.removeById(tenantId, calculatedField.getUuidId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(calculatedField.getId()).entity(calculatedField).build());
    }

    @Override
    public int deleteAllCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId) {
        log.trace("Executing deleteAllCalculatedFieldsByEntityId, tenantId [{}], entityId [{}]", tenantId, entityId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(entityId.getId(), id -> INCORRECT_ENTITY_ID + id);
        List<CalculatedField> calculatedFields = calculatedFieldDao.removeAllByEntityId(tenantId, entityId);
        return calculatedFields.size();
    }

    @Override
    public CalculatedFieldLink saveCalculatedFieldLink(TenantId tenantId, CalculatedFieldLink calculatedFieldLink) {
        calculatedFieldLinkDataValidator.validate(calculatedFieldLink, CalculatedFieldLink::getTenantId);
        log.trace("Executing save calculated field link, [{}]", calculatedFieldLink);
        return calculatedFieldLinkDao.save(tenantId, calculatedFieldLink);
    }

    @Override
    public CalculatedFieldLink findCalculatedFieldLinkById(TenantId tenantId, CalculatedFieldLinkId calculatedFieldLinkId) {
        log.trace("Executing findCalculatedFieldLinkById, tenantId [{}], calculatedFieldLinkId [{}]", tenantId, calculatedFieldLinkId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(calculatedFieldLinkId, id -> "Incorrect calculatedFieldLinkId " + id);
        return calculatedFieldLinkDao.findById(tenantId, calculatedFieldLinkId.getId());
    }

    @Override
    public List<CalculatedFieldLink> findAllCalculatedFieldLinksById(TenantId tenantId, CalculatedFieldId calculatedFieldId) {
        log.trace("Executing findAllCalculatedFieldLinksById, calculatedFieldId [{}]", calculatedFieldId);
        return calculatedFieldLinkDao.findCalculatedFieldLinksByCalculatedFieldId(tenantId, calculatedFieldId);
    }

    @Override
    public List<CalculatedFieldLink> findAllCalculatedFieldLinksByEntityId(TenantId tenantId, EntityId entityId) {
        log.trace("Executing findAllCalculatedFieldLinksByEntityId, entityId [{}]", entityId);
        return calculatedFieldLinkDao.findCalculatedFieldLinksByEntityId(tenantId, entityId);
    }

    @Override
    public PageData<CalculatedFieldLink> findAllCalculatedFieldLinksByTenantId(TenantId tenantId, PageLink pageLink) {
        log.trace("Executing findAllCalculatedFieldLinksByTenantId, tenantId[{}] pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return calculatedFieldLinkDao.findAllByTenantId(tenantId, pageLink);
    }

    @Override
    public PageData<CalculatedFieldLink> findAllCalculatedFieldLinks(PageLink pageLink) {
        log.trace("Executing findAllCalculatedFieldLinks, pageLink [{}]", pageLink);
        validatePageLink(pageLink);
        return calculatedFieldLinkDao.findAll(pageLink);
    }

    @Override
    public boolean referencedInAnyCalculatedField(TenantId tenantId, EntityId referencedEntityId) {
        return calculatedFieldDao.findAllByTenantId(tenantId).stream()
                .filter(calculatedField -> !referencedEntityId.equals(calculatedField.getEntityId()))
                .map(CalculatedField::getConfiguration)
                .map(CalculatedFieldConfiguration::getReferencedEntities)
                .anyMatch(referencedEntities -> referencedEntities.contains(referencedEntityId));
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findById(tenantId, new CalculatedFieldId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(calculatedFieldDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CALCULATED_FIELD;
    }

    private void createOrUpdateCalculatedFieldLink(TenantId tenantId, CalculatedField calculatedField) {
        List<CalculatedFieldLink> links = calculatedField.getConfiguration().buildCalculatedFieldLinks(tenantId, calculatedField.getEntityId(), calculatedField.getId());
        links.forEach(link -> saveCalculatedFieldLink(tenantId, link));
    }

}
