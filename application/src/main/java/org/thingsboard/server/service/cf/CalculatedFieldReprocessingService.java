// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.cf;

import org.thingsboard.server.common.data.job.task.CfReprocessingTask;

public interface CalculatedFieldReprocessingService {

    void reprocess(CfReprocessingTask task) throws Exception;

}
