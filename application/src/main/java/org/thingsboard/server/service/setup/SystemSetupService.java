// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.setup;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.setup.LicenseChangeResult;
import org.thingsboard.server.common.data.setup.LicenseClaimInfo;
import org.thingsboard.server.common.data.setup.LicenseClaimResult;
import org.thingsboard.server.common.data.setup.LicenseClaimStatus;
import org.thingsboard.server.common.data.setup.SetupInfo;
import org.thingsboard.server.common.data.setup.SystemSetupState;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;
import org.thingsboard.server.dao.subscription.LicenseStateReconciliationListener;

public interface SystemSetupService extends LicenseStateReconciliationListener {

    SystemSetupState getState();

    SetupInfo getSetupInfo();

    /**
     * @param claimToken the token the caller's own claim was minted with, or null when it holds none. A token
     *                   that a later claim request has superseded is answered
     *                   {@link LicenseClaimStatus#EXPIRED}, so a session left on a dead link learns it rather
     *                   than polling the live claim forever.
     */
    LicenseClaimInfo pollClaim(String claimToken) throws ThingsboardException;

    void applyLicenseKey(String secret);

    /** What {@code secret} would install, without installing it. */
    SubscriptionInfo previewLicenseKey(String secret);

    /** Applies {@code secret} and reports the setup state and the licence it left in force. */
    LicenseChangeResult applyLicenseKeyAndReport(String secret);

    LicenseClaimResult requestClaim() throws ThingsboardException;

    void completeSetup(String email, String password, boolean loadDemo);

    /**
     * Clears every trace of a licence from this node and from the cluster row, so the activation flow can be
     * run again without editing the database and restarting.
     */
    void clearLicense();

}
