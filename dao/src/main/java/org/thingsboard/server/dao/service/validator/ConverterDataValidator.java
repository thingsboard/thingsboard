// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.converter.ConverterType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.converter.ConverterDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Objects;

@Component
public class ConverterDataValidator extends DataValidator<Converter> {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private ConverterDao converterDao;

    @Override
    protected void validateCreate(TenantId tenantId, Converter converter) {
        if (!converter.isEdgeTemplate()) {
            validateNumberOfEntitiesPerTenant(tenantId, EntityType.CONVERTER);
        }
    }

    @Override
    protected Converter validateUpdate(TenantId tenantId, Converter converter) {
        Converter existingConverter = converterDao.findById(converter.getTenantId(), converter.getUuidId());
        if (existingConverter != null) {
            if (!converter.getType().equals(existingConverter.getType())) {
                throw new DataValidationException("Converter type cannot be changed!");
            }
            if (!Objects.equals(converter.getIntegrationType(), existingConverter.getIntegrationType())) {
                throw new DataValidationException("Integration type cannot be changed!");
            }
            if (!Objects.equals(converter.getConverterVersion(), existingConverter.getConverterVersion())) {
                throw new DataValidationException("Converter version cannot be changed!");
            }
        }
        return existingConverter;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Converter converter) {
        if (converter.getType() == null) {
            throw new DataValidationException("Converter type should be specified!");
        }
        if (StringUtils.isEmpty(converter.getName())) {
            throw new DataValidationException("Converter name should be specified!");
        }
        if (converter.getTenantId() == null || converter.getTenantId().isNullUid()) {
            throw new DataValidationException("Converter should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(converter.getTenantId())) {
                throw new DataValidationException("Converter is referencing to non-existent tenant!");
            }
        }
        if (converter.getConfiguration() == null || converter.getConfiguration().isNull()) {
            throw new DataValidationException("Converter configuration should be specified!");
        } else {
            if (converter.getType() == ConverterType.UPLINK) {
                if (!converter.getConfiguration().has("decoder")) {
                    throw new DataValidationException("Converter 'decoder' field should be specified in configuration!");
                }
                if (converter.getConverterVersion() == null) {
                    throw new DataValidationException("Converter 'version' should be specified!");
                }
                if (converter.getConverterVersion() == 2 && converter.getIntegrationType() == null) {
                    throw new DataValidationException("Converter 'integrationType' should be specified!");
                }
            } else {
                if (!converter.getConfiguration().has("encoder")) {
                    throw new DataValidationException("Converter 'encoder' field should be specified in configuration!");
                }
            }
        }
        if (converterDao.existsByTenantIdAndNameAndType(converter.getTenantId().getId(), converter.getName(), converter.getType(), converter.getUuidId())) {
            throw new DataValidationException("Converter with such name and type already exists!");
        }
    }

}
