// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

/**
 * Thrown when a keyless deployment's cumulative non-production uptime has reached
 * {@link TbClusterStore#NON_PRODUCTION_UPTIME_LIMIT_MS}. Its own type so boot can single it out and fail
 * startup rather than serve the data plane on a spent allowance.
 */
public class NonProductionAllowanceExhaustedException extends RuntimeException {

    public NonProductionAllowanceExhaustedException(String message) {
        super(message);
    }

}
