// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.queue.processing;

public interface TbRuleEngineProcessingStrategy {

    boolean isSkipTimeoutMsgs();

    TbRuleEngineProcessingDecision analyze(TbRuleEngineProcessingResult result);

}
