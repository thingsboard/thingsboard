// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.community_grant;

/**
 * There is no success value: a run that succeeds is cleared from the flow state.
 */
public enum CommunityGrantOfflineRunStatus {

    RUNNING,
    FAILED

}
