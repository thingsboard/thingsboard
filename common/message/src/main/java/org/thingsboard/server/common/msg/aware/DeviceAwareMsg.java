// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.aware;

import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.msg.TbActorMsg;

public interface DeviceAwareMsg extends TbActorMsg {

    DeviceId getDeviceId();
}
