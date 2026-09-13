// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthCmd implements WsCmd {
    private int cmdId;
    private String token;

    @Override
    public WsCmdType getType() {
        return WsCmdType.AUTH;
    }
}
