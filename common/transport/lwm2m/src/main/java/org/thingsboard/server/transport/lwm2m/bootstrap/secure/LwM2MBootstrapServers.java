// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.bootstrap.secure;

import lombok.Data;

@Data
public class LwM2MBootstrapServers {
    private Integer shortId = 123;
    private Integer lifetime = 300;
    private Integer defaultMinPeriod = 1;
    private boolean notifIfDisabled = true;
    private String binding = "UQ";
}
