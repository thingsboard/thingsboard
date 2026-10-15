// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.aware;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;

public interface TenantAwareMsg extends TbActorMsg {

	TenantId getTenantId();

	default TbCallback getCallback() {
		return TbCallback.EMPTY;
	}

}
