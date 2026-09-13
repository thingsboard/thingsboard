// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc;

import com.google.common.util.concurrent.SettableFuture;
import lombok.Data;
import org.thingsboard.server.gen.edge.v1.DownlinkMsg;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

@Data
public class EdgeSessionState {

    private final Map<Integer, DownlinkMsg> pendingMsgsMap = Collections.synchronizedMap(new LinkedHashMap<>());
    private SettableFuture<Boolean> sendDownlinkMsgsFuture;
    private ScheduledFuture<?> scheduledSendDownlinkTask;

}
