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
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.widget.WidgetType;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.widget.WidgetTypeDao;
import org.thingsboard.server.dao.widget.WidgetsBundleDao;

@Component
@AllArgsConstructor
public class WidgetTypeDataValidator extends DataValidator<WidgetTypeDetails> {

    private final WidgetTypeDao widgetTypeDao;
    private final WidgetsBundleDao widgetsBundleDao;
    private final TenantService tenantService;

    @Override
    protected void validateDataImpl(TenantId tenantId, WidgetTypeDetails widgetTypeDetails) {
        validateString("Widgets type name", widgetTypeDetails.getName());
        if (widgetTypeDetails.getDescriptor() == null || widgetTypeDetails.getDescriptor().size() == 0) {
            throw new DataValidationException("Widgets type descriptor can't be empty!");
        }
        if (widgetTypeDetails.getTenantId() == null) {
            widgetTypeDetails.setTenantId(TenantId.fromUUID(ModelConstants.NULL_UUID));
        }
        if (!widgetTypeDetails.getTenantId().getId().equals(ModelConstants.NULL_UUID)) {
            if (!tenantService.tenantExists(widgetTypeDetails.getTenantId())) {
                throw new DataValidationException("Widget type is referencing to non-existent tenant!");
            }
        }
    }

    @Override
    protected void validateCreate(TenantId tenantId, WidgetTypeDetails widgetTypeDetails) {
        String fqn = widgetTypeDetails.getFqn();
        if (fqn == null || fqn.trim().isEmpty()) {
            fqn = widgetTypeDetails.getName().toLowerCase().replaceAll("\\W+", "_");
        }
        String originalFqn = fqn;
        int c = 1;
        WidgetType withSameFqn;
        do {
            withSameFqn = widgetTypeDao.findByTenantIdAndFqn(widgetTypeDetails.getTenantId().getId(), fqn);
            if (withSameFqn != null) {
                fqn = originalFqn + (++c);
            }
        } while (withSameFqn != null);
        widgetTypeDetails.setFqn(fqn);
    }

    @Override
    protected WidgetTypeDetails validateUpdate(TenantId tenantId, WidgetTypeDetails widgetTypeDetails) {
        WidgetTypeDetails storedWidgetType = widgetTypeDao.findById(tenantId, widgetTypeDetails.getId().getId());
        if (!storedWidgetType.getTenantId().getId().equals(widgetTypeDetails.getTenantId().getId())) {
            throw new DataValidationException("Can't move existing widget type to different tenant!");
        }
        if (!storedWidgetType.getFqn().equals(widgetTypeDetails.getFqn())) {
            throw new DataValidationException("Update of widget type fqn is prohibited!");
        }
        return new WidgetTypeDetails(storedWidgetType);
    }
}
