// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.setup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;

/**
 * What changed when a key was applied: the setup state, so a system administrator recovering a locked instance
 * learns in one round trip whether the key lifted the lock, and the licence now in force, so the page that
 * asked needs no second call. The subscription is null when the key was applied but the licence could not be
 * read back - a critical error or a peer's clear can unactivate this node in between.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LicenseChangeResult {

    private SystemSetupState status;
    private SubscriptionInfo subscription;

}
