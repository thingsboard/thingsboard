// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.setup;

/**
 * How this installation expects to receive its licence, as its own reachability probe reads it. A hint
 * carried in the sign-up URL, not an instruction: the portal pre-selects it and the operator confirms it
 * there, since the URL is theirs to edit.
 */
public enum LicenseClaimMode {

    ONLINE,
    OFFLINE

}
