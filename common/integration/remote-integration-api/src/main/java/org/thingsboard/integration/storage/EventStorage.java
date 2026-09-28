// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.storage;

import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.server.gen.integration.UplinkMsg;

import java.util.List;

public interface EventStorage {

    void write(UplinkMsg msg, IntegrationCallback<Void> callback);

    List<UplinkMsg> readCurrentBatch();

    void discardCurrentBatch();

    void sleep();
}
