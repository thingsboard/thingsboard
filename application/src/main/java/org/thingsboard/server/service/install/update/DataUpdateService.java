// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install.update;

public interface DataUpdateService {

    void updateData() throws Exception;

    void upgradeRuleNodes();
}
