// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.agent.AgentAppProfile;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.agent.AgentAppArgumentReferenceValidator;
import org.thingsboard.server.dao.agent.AgentAppProfileDao;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

@Component
@AllArgsConstructor
public class AgentAppProfileDataValidator extends DataValidator<AgentAppProfile> {

    private final AgentAppProfileDao profileDao;
    private final TenantService tenantService;
    private final AgentAppArgumentReferenceValidator argumentReferenceValidator;

    @Override
    protected AgentAppProfile validateUpdate(TenantId tenantId, AgentAppProfile profile) {
        AgentAppProfile old = profileDao.findById(profile.getTenantId(), profile.getId().getId());
        if (old == null) {
            throw new DataValidationException("Can't update non existing agent application profile!");
        }
        if (profile.getAppType() != null && profile.getAppType() != old.getAppType()) {
            throw new DataValidationException("Agent application profile type cannot be changed!");
        }
        return old;
    }

    @Override
    protected void validateDataImpl(TenantId tenantId, AgentAppProfile profile) {
        validateString("Agent application profile name", profile.getName());
        if (profile.getAppType() == null) {
            throw new DataValidationException("Agent application profile app type must not be null!");
        }
        if (profile.getTemplateVersion() == null) {
            throw new DataValidationException("Agent application profile template version must not be null!");
        }
        if (profile.getTenantId() == null) {
            throw new DataValidationException("Agent application profile should be assigned to tenant!");
        }
        if (profile.getConfig() == null) {
            throw new DataValidationException("Agent application config must not be null!");
        }
        profile.getConfig().validate();
        profile.getConfig().validateForProfile(profile.getAppType());
        argumentReferenceValidator.validate(profile.getTenantId(), profile.getConfig(), null);
        if (!tenantService.tenantExists(profile.getTenantId())) {
            throw new DataValidationException("Agent application profile is referencing to non-existent tenant!");
        }
    }
}
