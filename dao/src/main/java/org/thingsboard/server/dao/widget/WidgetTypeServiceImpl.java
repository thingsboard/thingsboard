/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.widget;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.WidgetTypeId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetsBundle;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.TenantEntity;
import org.thingsboard.server.dao.model.WidgetTypeEntity;
import org.thingsboard.server.dao.model.WidgetsBundleEntity;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.Validator;
import org.thingsboard.server.dao.tenant.TenantDao;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.List;

import static org.thingsboard.server.dao.DaoUtil.convertDataList;
import static org.thingsboard.server.dao.DaoUtil.getData;

@Service
@Slf4j
public class WidgetTypeServiceImpl implements WidgetTypeService {

    @Autowired
    private WidgetTypeDao widgetTypeDao;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private WidgetsBundleDao widgetsBundleService;

    @Override
    public WidgetType findWidgetTypeById(WidgetTypeId widgetTypeId) {
        log.trace("Executing findWidgetTypeById [{}]", widgetTypeId);
        Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
        WidgetTypeEntity widgetTypeEntity = widgetTypeDao.findById(widgetTypeId.getId());
        return getData(widgetTypeEntity);
    }

    @Override
    public WidgetType saveWidgetType(WidgetType widgetType) {
        log.trace("Executing saveWidgetType [{}]", widgetType);
        widgetTypeValidator.validate(widgetType);
        WidgetTypeEntity widgetTypeEntity = widgetTypeDao.save(widgetType);
        return getData(widgetTypeEntity);
    }

    @Override
    public void deleteWidgetType(WidgetTypeId widgetTypeId) {
        log.trace("Executing deleteWidgetType [{}]", widgetTypeId);
        Validator.validateId(widgetTypeId, "Incorrect widgetTypeId " + widgetTypeId);
        widgetTypeDao.removeById(widgetTypeId.getId());
    }

    @Override
    public List<WidgetType> findWidgetTypesByTenantIdAndBundleAlias(TenantId tenantId, String bundleAlias) {
        log.trace("Executing findWidgetTypesByTenantIdAndBundleAlias, tenantId [{}], bundleAlias [{}]", tenantId, bundleAlias);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateString(bundleAlias, "Incorrect bundleAlias " + bundleAlias);
        List<WidgetTypeEntity> widgetTypesEntities = widgetTypeDao.findWidgetTypesByTenantIdAndBundleAlias(tenantId.getId(), bundleAlias);
        return convertDataList(widgetTypesEntities);
    }

    @Override
    public WidgetType findWidgetTypeByTenantIdBundleAliasAndAlias(TenantId tenantId, String bundleAlias, String alias) {
        log.trace("Executing findWidgetTypeByTenantIdBundleAliasAndAlias, tenantId [{}], bundleAlias [{}], alias [{}]", tenantId, bundleAlias, alias);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateString(bundleAlias, "Incorrect bundleAlias " + bundleAlias);
        Validator.validateString(alias, "Incorrect alias " + alias);
        WidgetTypeEntity widgetTypeEntity = widgetTypeDao.findByTenantIdBundleAliasAndAlias(tenantId.getId(), bundleAlias, alias);
        return getData(widgetTypeEntity);
    }

    @Override
    public void deleteWidgetTypesByTenantIdAndBundleAlias(TenantId tenantId, String bundleAlias) {
        log.trace("Executing deleteWidgetTypesByTenantIdAndBundleAlias, tenantId [{}], bundleAlias [{}]", tenantId, bundleAlias);
        Validator.validateId(tenantId, "Incorrect tenantId " + tenantId);
        Validator.validateString(bundleAlias, "Incorrect bundleAlias " + bundleAlias);
        List<WidgetTypeEntity> widgetTypesEntities = widgetTypeDao.findWidgetTypesByTenantIdAndBundleAlias(tenantId.getId(), bundleAlias);
        for (WidgetTypeEntity widgetTypeEntity : widgetTypesEntities) {
            deleteWidgetType(new WidgetTypeId(widgetTypeEntity.getId()));
        }
    }

    private DataValidator<WidgetType> widgetTypeValidator =
            new DataValidator<WidgetType>() {
                @Override
                protected void validateDataImpl(WidgetType widgetType) {
                    if (StringUtils.isEmpty(widgetType.getName())) {
                        throw new DataValidationException("Widgets type name should be specified!");
                    }
                    if (StringUtils.isEmpty(widgetType.getBundleAlias())) {
                        throw new DataValidationException("Widgets type bundle alias should be specified!");
                    }
                    if (widgetType.getDescriptor() == null || widgetType.getDescriptor().size() == 0) {
                        throw new DataValidationException("Widgets type descriptor can't be empty!");
                    }
                    if (widgetType.getTenantId() == null) {
                        widgetType.setTenantId(new TenantId(ModelConstants.NULL_UUID));
                    }
                    if (!widgetType.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
                        TenantEntity tenant = tenantDao.findById(widgetType.getTenantId().getId());
                        if (tenant == null) {
                            throw new DataValidationException("Widget type is referencing to non-existent tenant!");
                        }
                    }
                }

                @Override
                protected void validateCreate(WidgetType widgetType) {

                    WidgetsBundleEntity widgetsBundle = widgetsBundleService.findWidgetsBundleByTenantIdAndAlias(widgetType.getTenantId().getId(), widgetType.getBundleAlias());
                    if (widgetsBundle == null) {
                        throw new DataValidationException("Widget type is referencing to non-existent widgets bundle!");
                    }

                    String alias = widgetType.getName().toLowerCase().replaceAll("\\W+", "_");
                    String originalAlias = alias;
                    int c = 1;
                    WidgetTypeEntity withSameAlias;
                    do {
                        withSameAlias = widgetTypeDao.findByTenantIdBundleAliasAndAlias(widgetType.getTenantId().getId(), widgetType.getBundleAlias(), alias);
                        if (withSameAlias != null) {
                            alias = originalAlias + (++c);
                        }
                    } while(withSameAlias != null);
                    widgetType.setAlias(alias);
                }

                @Override
                protected void validateUpdate(WidgetType widgetType) {
                    WidgetTypeEntity storedWidgetType = widgetTypeDao.findById(widgetType.getId().getId());
                    if (!storedWidgetType.getTenantId().equals(widgetType.getTenantId().getId())) {
                        throw new DataValidationException("Can't move existing widget type to different tenant!");
                    }
                    if (!storedWidgetType.getBundleAlias().equals(widgetType.getBundleAlias())) {
                        throw new DataValidationException("Update of widget type bundle alias is prohibited!");
                    }
                    if (!storedWidgetType.getAlias().equals(widgetType.getAlias())) {
                        throw new DataValidationException("Update of widget type alias is prohibited!");
                    }
                }
            };
}
