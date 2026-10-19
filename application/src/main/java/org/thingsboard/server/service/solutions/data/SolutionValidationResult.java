// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The outcome of validating a solution template before anything is created: either the install can go ahead, or the
 * entities of the tenant the template clashes with, reported as the markdown the install dialog renders.
 */
@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class SolutionValidationResult {

    private static final SolutionValidationResult PASSED = new SolutionValidationResult(true, "");

    private final boolean passed;
    private final String conflictReport;

    public static SolutionValidationResult passed() {
        return PASSED;
    }

    public static SolutionValidationResult conflictsFound(String conflictReport) {
        return new SolutionValidationResult(false, conflictReport);
    }

}
