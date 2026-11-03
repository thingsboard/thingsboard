// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.rpc;

import org.thingsboard.rule.engine.api.RuleEngineRpcService;
import org.thingsboard.server.common.msg.rpc.FromDeviceRpcResponse;

/**
 * Created by ashvayka on 16.04.18.
 */
public interface TbRuleEngineDeviceRpcService extends RuleEngineRpcService {

    /**
     * Handles the RPC response from the Device Actor (Transport).
     *
     * @param response the RPC response
     */
    void processRpcResponseFromDevice(FromDeviceRpcResponse response);
}
