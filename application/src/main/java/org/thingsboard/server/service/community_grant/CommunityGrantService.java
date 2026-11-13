// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import org.thingsboard.server.common.data.community_grant.CommunityGrantStateInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;

import java.util.function.Consumer;

public interface CommunityGrantService {

    CommunityGrantStateInfo getStateInfo();

    /** Starts or restarts the flow; a restart supersedes the previous claim token. */
    CommunityGrantStateInfo start() throws ThingsboardException;

    /**
     * Starts an uploaded instance checker in the background on this node. The signature must verify before
     * this returns; a refusal throws here, and the run's own outcome reaches {@link #getStateInfo()}.
     *
     * @param completionListener told how the run ended, on the run's own thread: {@code null} on success
     */
    CommunityGrantStateInfo runOfflineChecker(byte[] checkerData, byte[] signatureData, String fileName,
                                              CommunityGrantCheckerInput checkerInput,
                                              Consumer<Exception> completionListener) throws ThingsboardException;

    String getOfflineReport() throws ThingsboardException;

    /** Records that the operator handed the report to the portal by hand; the flow keeps waiting. */
    CommunityGrantStateInfo confirmOfflineHandoff() throws ThingsboardException;

    /** Asks the portal to notify this cluster's registered owner. Throttled by a cooldown. */
    void requestAccess() throws ThingsboardException;

}
