// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.rpc;

import lombok.Data;

import java.io.Serializable;

@Data
public class IntegrationSession implements Serializable {

    private final String serviceId;

}
