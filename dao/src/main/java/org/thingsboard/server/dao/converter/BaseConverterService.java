// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.converter;

import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.IntegrationType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;
import static org.thingsboard.server.dao.service.Validator.validateString;

@Slf4j
@Service("ConverterDaoService")
public class BaseConverterService extends AbstractEntityService implements ConverterService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CONVERTER_ID = "Incorrect converterId ";
    public static final String INCORRECT_CONVERTER_NAME = "Incorrect converter name ";

    @Autowired
    private ConverterDao converterDao;

    @Autowired
    private DataValidator<Converter> converterValidator;

    @Autowired
    private EntityCountService entityCountService;

    @Autowired
    private JpaExecutorService executor;

    @Override
    public Converter saveConverter(Converter converter) {
        return saveEntity(converter, () -> doSaveConverter(converter));
    }

    private Converter doSaveConverter(Converter converter) {
        log.trace("Executing saveConverter [{}]", converter);
        if (converter.getConverterVersion() == null) {
            converter.setConverterVersion(1);
        }
        converterValidator.validate(converter, Converter::getTenantId);
        TenantId tenantId = converter.getTenantId();

        try {
            updateDebugSettings(tenantId, converter, System.currentTimeMillis());

            Converter savedConverter = converterDao.save(converter.getTenantId(), converter);
            if (converter.getId() == null) {
                entityCountService.publishCountEntityEvictEvent(converter.getTenantId(), EntityType.CONVERTER);
            }
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedConverter.getTenantId()).entity(savedConverter)
                    .entityId(savedConverter.getId()).created(converter.getId() == null).build());
            return savedConverter;
        } catch (Exception t) {
            checkConstraintViolation(t,
                    "converter_external_id_unq_key", "Converter with such external id already exists!");
            throw t;
        }
    }

    @Override
    public Converter findConverterById(TenantId tenantId, ConverterId converterId) {
        log.trace("Executing findConverterById [{}]", converterId);
        validateId(converterId, id -> INCORRECT_CONVERTER_ID + id);
        return converterDao.findById(tenantId, converterId.getId());
    }

    @Override
    public Optional<Converter> findConverterByName(TenantId tenantId, String converterName) {
        log.trace("Executing findConverterByName, tenantId [{}], name [{}]", tenantId, converterName);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(converterName, n -> INCORRECT_CONVERTER_NAME + n);
        return converterDao.findConverterByTenantIdAndName(tenantId.getId(), converterName);
    }

    @Override
    public ListenableFuture<Optional<Converter>> findConverterByNameAsync(TenantId tenantId, String converterName) {
        log.trace("Executing findConverterByNameAsync, tenantId [{}], name [{}]", tenantId, converterName);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateString(converterName, n -> INCORRECT_CONVERTER_NAME + n);
        return executor.submit(() -> findConverterByName(tenantId, converterName));
    }

    @Override
    public ListenableFuture<Converter> findConverterByIdAsync(TenantId tenantId, ConverterId converterId) {
        log.trace("Executing findConverterByIdAsync [{}]", converterId);
        validateId(converterId, id -> INCORRECT_CONVERTER_ID + id);
        return converterDao.findByIdAsync(tenantId, converterId.getId());
    }

    @Override
    public ListenableFuture<List<Converter>> findConvertersByIdsAsync(TenantId tenantId, List<ConverterId> converterIds) {
        log.trace("Executing findConvertersByIdsAsync, tenantId [{}], converterIds [{}]", tenantId, converterIds);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateIds(converterIds, ids -> "Incorrect converterIds " + ids);
        return converterDao.findConvertersByTenantIdAndIdsAsync(tenantId.getId(), toUUIDs(converterIds));
    }

    @Override
    public PageData<Converter> findTenantConverters(TenantId tenantId, IntegrationType integrationType, PageLink pageLink) {
        log.trace("Executing findTenantConverters, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return converterDao.findCoreConvertersByTenantId(tenantId.getId(), integrationType, pageLink);
    }

    @Override
    public PageData<Converter> findTenantEdgeTemplateConverters(TenantId tenantId, IntegrationType integrationType, PageLink pageLink) {
        log.trace("Executing findTenantEdgeTemplateConverters, tenantId [{}], pageLink [{}]", tenantId, pageLink);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(pageLink);
        return converterDao.findEdgeTemplateConvertersByTenantId(tenantId.getId(), integrationType, pageLink);
    }

    @Override
    @Transactional
    public void deleteConverter(TenantId tenantId, ConverterId converterId) {
        log.trace("Executing deleteConverter [{}]", converterId);
        Converter converter = findConverterById(tenantId, converterId);
        if (converter == null) {
            return;
        }
        try {
            converterDao.removeById(tenantId, converterId.getId());
        } catch (Exception t) {
            ConstraintViolationException e = DaoUtil.extractConstraintViolationException(t).orElse(null);
            if (e != null && DaoUtil.constraintNameMatches(e.getConstraintName(), "fk_integration_converter")) {
                throw new DataValidationException("The converter referenced by the integration cannot be deleted!");
            } else if (e != null && DaoUtil.constraintNameMatches(e.getConstraintName(), "fk_integration_downlink_converter")) {
                throw new DataValidationException("The downlink converter referenced by the integration cannot be deleted!");
            } else {
                throw t;
            }
        }
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entity(converter).entityId(converterId).build());
        entityCountService.publishCountEntityEvictEvent(tenantId, EntityType.CONVERTER);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteConverter(tenantId, (ConverterId) id);
    }

    @Override
    public void deleteConvertersByTenantId(TenantId tenantId) {
        log.trace("Executing deleteConvertersByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantConvertersRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public Map<IntegrationType, Set<ConverterType>> getExistingConverterTypes(TenantId tenantId) {
        log.trace("Executing getExistingConverterTypes, tenantId [{}]", tenantId);
        return converterDao.findExistingConverterTypes(tenantId.getId());
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteConvertersByTenantId(tenantId);
    }

    private final PaginatedRemover<TenantId, Converter> tenantConvertersRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<Converter> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return converterDao.findByTenantId(id.getId(), pageLink);
        }

        @Override
        protected void removeEntity(TenantId tenantId, Converter entity) {
            deleteConverter(tenantId, new ConverterId(entity.getId().getId()));
        }

    };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findConverterById(tenantId, new ConverterId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(converterDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return converterDao.countByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CONVERTER;
    }

}
