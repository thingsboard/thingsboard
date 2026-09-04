// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.rpc;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@Data
@EqualsAndHashCode(callSuper = true)
public class RpcCreateRequest extends LwM2MRpcRequestHeader {

    private Object value;
    private String contentFormat;
    private Map<String, Object> nodes;

}
