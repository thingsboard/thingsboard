// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration.downlink;

import lombok.Data;

import java.io.Serializable;
import java.util.UUID;

/**
 * Created by ashvayka on 22.02.18.
 */
@Data
public class RPCCall implements Serializable {

    private UUID id;
    private long expirationTime;
    private String method;
    private String params;

}
