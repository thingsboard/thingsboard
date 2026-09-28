// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.install.update;

public interface DataUpdateService {

    void updateData(boolean fromCe) throws Exception;

    void postUpdateData() throws Exception;

    void upgradeRuleNodes();
}
