// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.rpc;

import org.thingsboard.server.gen.transport.TransportProtos;

public interface LwM2MRpcRequestHandler {

    void onToDeviceRpcRequest(TransportProtos.ToDeviceRpcRequestMsg toDeviceRequest, TransportProtos.SessionInfoProto sessionInfo);

    void onToDeviceRpcResponse(TransportProtos.ToDeviceRpcResponseMsg toDeviceRpcResponse, TransportProtos.SessionInfoProto sessionInfo);

    void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg toServerResponse);


}
