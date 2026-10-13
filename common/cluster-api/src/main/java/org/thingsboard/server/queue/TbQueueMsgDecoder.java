// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue;

import com.google.protobuf.InvalidProtocolBufferException;

public interface TbQueueMsgDecoder<T extends TbQueueMsg> {

    T decode(TbQueueMsg msg) throws InvalidProtocolBufferException;
}
