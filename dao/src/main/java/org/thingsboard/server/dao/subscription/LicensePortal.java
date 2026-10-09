// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

/**
 * The licence portal every operator-facing message points at. Its own holder rather than a member of any one
 * of them, because the three messages that name it - the exhausted non-production allowance, the
 * production-scale warning and {@link LicenseCapacity}'s cap refusal - have nothing else in common.
 */
public final class LicensePortal {

    public static final String URL = "https://license.thingsboard.io";

    private LicensePortal() {
    }

}
