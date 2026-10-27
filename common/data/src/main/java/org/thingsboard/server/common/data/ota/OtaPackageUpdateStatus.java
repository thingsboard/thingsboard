// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.ota;

public enum OtaPackageUpdateStatus {
    QUEUED, INITIATED, DOWNLOADING, DOWNLOADED, VERIFIED, UPDATING, UPDATED, FAILED
}
