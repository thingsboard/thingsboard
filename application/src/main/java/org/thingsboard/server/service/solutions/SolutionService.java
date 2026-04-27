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
