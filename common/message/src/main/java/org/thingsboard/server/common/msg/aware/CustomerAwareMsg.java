// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.aware;

import org.thingsboard.server.common.data.id.CustomerId;

public interface CustomerAwareMsg {

	CustomerId getCustomerId();
	
}
