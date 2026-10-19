// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.aware;

import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;

public interface RuleChainAwareMsg extends TbActorMsg {

	RuleChainId getRuleChainId();

	TbMsg getMsg();
	
}
