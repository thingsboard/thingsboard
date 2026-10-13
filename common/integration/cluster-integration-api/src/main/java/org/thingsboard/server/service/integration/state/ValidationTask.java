// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.state;

import com.google.common.util.concurrent.SettableFuture;
import lombok.Data;
import org.thingsboard.server.common.data.integration.Integration;

import java.util.UUID;

@Data
public class ValidationTask {

    private final UUID uuid = UUID.randomUUID();
    private final long ts = System.currentTimeMillis();
    private final SettableFuture<Void> future = SettableFuture.create();
    private final ValidationTaskType type;
    private final Integration configuration;

}
