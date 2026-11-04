// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.update;

import org.thingsboard.server.common.data.UpdateMessage;

public interface UpdateService {

    UpdateMessage checkUpdates();

}
