// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.rpc;

import lombok.Data;
import org.thingsboard.server.common.data.rpc.RpcError;

import java.io.Serializable;
import java.util.Optional;
import java.util.UUID;

/**
 * @author Andrew Shvayka
 */
@Data
public class FromDeviceRpcResponse implements Serializable {

    private static final long serialVersionUID = -3799452502112373491L;

    private final UUID id;
    private final String response;
    private final RpcError error;

    public Optional<String> getResponse() {
        return Optional.ofNullable(response);
    }

    public Optional<RpcError> getError() {
        return Optional.ofNullable(error);
    }

}
