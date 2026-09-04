// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.edge;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;

import java.io.Serializable;

public interface EdgeSessionMsg extends Serializable {

    TenantId getTenantId();

    MsgType getMsgType();

}
