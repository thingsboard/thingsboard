// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import org.thingsboard.server.common.data.id.RuleChainId;

public interface HasRuleEngineProfile {

    RuleChainId getDefaultRuleChainId();

    String getDefaultQueueName();

}
