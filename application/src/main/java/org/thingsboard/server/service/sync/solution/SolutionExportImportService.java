// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.sync.solution;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.sync.solution.SolutionData;
import org.thingsboard.server.common.data.sync.solution.SolutionExportRequest;
import org.thingsboard.server.common.data.sync.solution.SolutionExportResponse;
import org.thingsboard.server.common.data.sync.solution.SolutionImportResult;
import org.thingsboard.server.common.data.sync.solution.SolutionValidationResult;
import org.thingsboard.server.service.security.model.SecurityUser;

public interface SolutionExportImportService {

    SolutionExportResponse exportSolution(SecurityUser user, SolutionExportRequest request) throws ThingsboardException;

    SolutionImportResult importSolution(SecurityUser user, SolutionData solutionData) throws ThingsboardException;

    SolutionValidationResult validateSolution(SecurityUser user, SolutionData solutionData) throws ThingsboardException;

}
