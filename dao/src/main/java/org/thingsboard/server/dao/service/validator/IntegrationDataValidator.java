// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.converter.Converter;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.Integration;
import org.thingsboard.server.dao.converter.ConverterDao;
import org.thingsboard.server.dao.integration.IntegrationDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

@Component
public class IntegrationDataValidator extends DataValidator<Integration> {

    @Autowired
    private IntegrationDao integrationDao;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private ConverterDao converterDao;

    @Override
    protected void validateCreate(TenantId tenantId, Integration integration) {
        if (!integration.isEdgeTemplate()) {
            validateNumberOfEntitiesPerTenant(tenantId, EntityType.INTEGRATION);
        }
        integrationDao.findByRoutingKey(tenantId.getId(), integration.getRoutingKey()).ifPresent(
                d -> {
                    throw new DataValidationException("Integration with such routing key already exists!");
                }
        );
    }

    @Override
    protected Integration validateUpdate(TenantId tenantId, Integration integration) {
        var old = integrationDao.findByRoutingKey(tenantId.getId(), integration.getRoutingKey());
        old.ifPresent(
                d -> {
                    if (!d.getId().equals(integration.getId())) {
                        throw new DataValidationException("Integration with such routing key already exists!");
                    }
                    if (!d.getType().equals(integration.getType())) {
                        throw new DataValidationException("Integration type can not be changed!");
                    }
                }
        );
        return old.orElse(null);
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, Integration integration) {
        if (StringUtils.isEmpty(integration.getName())) {
            throw new DataValidationException("Integration name should be specified!");
        }
        if (integration.getType() == null) {
            throw new DataValidationException("Integration type should be specified!");
        }
        if (StringUtils.isEmpty(integration.getRoutingKey())) {
            throw new DataValidationException("Integration routing key should be specified!");
        }
        if (integration.getTenantId() == null || integration.getTenantId().isNullUid()) {
            throw new DataValidationException("Integration should be assigned to tenant!");
        } else {
            if (!tenantService.tenantExists(integration.getTenantId())) {
                throw new DataValidationException("Integration is referencing to non-existent tenant!");
            }
        }
        if (integration.getDefaultConverterId() == null || integration.getDefaultConverterId().isNullUid()) {
            throw new DataValidationException("Integration default converter should be specified!");
        } else {
            Converter converter = converterDao.findById(tenantId, integration.getDefaultConverterId().getId());
            if (converter == null) {
                throw new DataValidationException("Integration is referencing to non-existent converter!");
            }
            if (!converter.getTenantId().equals(integration.getTenantId())) {
                throw new DataValidationException("Integration can't have converter from different tenant!");
            }
            if (converter.isEdgeTemplate() != integration.isEdgeTemplate()) {
                throw new DataValidationException("Edge integration can't have non-edge converter and vise versa!");
            }
        }
    }
}
