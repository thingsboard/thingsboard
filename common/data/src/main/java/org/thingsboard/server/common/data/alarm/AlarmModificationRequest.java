// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;

public interface AlarmModificationRequest {

    TenantId getTenantId();

    AlarmSeverity getSeverity();

    long getStartTs();

    long getEndTs();

    void setStartTs(long startTs);

    void setEndTs(long endTs);

    UserId getUserId();
}
