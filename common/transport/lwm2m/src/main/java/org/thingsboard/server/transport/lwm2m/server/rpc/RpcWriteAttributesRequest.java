// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.rpc;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.device.profile.lwm2m.ObjectAttributes;

@Data
@EqualsAndHashCode(callSuper = true)
public class RpcWriteAttributesRequest extends LwM2MRpcRequestHeader {

    private ObjectAttributes attributes;

}
