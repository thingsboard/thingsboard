// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state.geofencing;

public interface ScheduledRefreshSupported {

    void resetScheduledRefreshTs();

    long getLastScheduledRefreshTs();

    void updateScheduledRefreshTs();

}
