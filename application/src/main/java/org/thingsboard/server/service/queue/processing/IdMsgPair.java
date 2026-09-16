// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.queue.processing;

import com.google.protobuf.GeneratedMessageV3;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.UUID;

public record IdMsgPair<T extends GeneratedMessageV3>(UUID uuid, TbProtoQueueMsg<T> msg) {}
