// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.housekeeper;

import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;

public interface HousekeeperClient {

    void submitTask(HousekeeperTask task);

}
