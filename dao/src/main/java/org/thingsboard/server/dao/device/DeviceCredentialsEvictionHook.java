// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device;

/**
 * Invoked whenever cached validation results for the credentials must be invalidated: the credentials were actually
 * created, rotated or deleted, or their cached validation data became stale without a credentials change (e.g. the
 * owning device was reassigned to another tenant — such events carry the current credentials id, no old id and
 * {@code deleted = false}, the same shape as a creation). {@code deleted = true} means the credentials ceased to
 * exist. Invoked after commit in transactional flows; never invoked for defensive evictions on failed saves.
 */
public interface DeviceCredentialsEvictionHook {

    void onDeviceCredentialsEvict(DeviceCredentialsEvictEvent event);

}
