// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions;

import jakarta.servlet.http.HttpServletRequest;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.iot_hub.SolutionTemplateInstalledItemDescriptor;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.solutions.data.solution.SolutionInstallResponse;

import java.util.List;

public interface SolutionService {

    SolutionInstallResponse installSolution(SecurityUser user, TenantId tenantId, byte[] zipData, HttpServletRequest request) throws Exception;

    void deleteSolution(TenantId tenantId, SolutionTemplateInstalledItemDescriptor descriptor, SecurityUser user) throws ThingsboardException;

}
