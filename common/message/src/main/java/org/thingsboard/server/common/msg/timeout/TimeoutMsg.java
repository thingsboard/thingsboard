// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.timeout;

import lombok.Data;
import org.thingsboard.server.common.msg.TbActorMsg;

/**
 * @author Andrew Shvayka
 */
@Data
public abstract class TimeoutMsg<T> implements TbActorMsg {
    private final T id;
    private final long timeout;
}
