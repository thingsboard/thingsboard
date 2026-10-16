// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.ws;

import com.fasterxml.jackson.annotation.JsonIgnore;


public interface WsCmd {

    int getCmdId();

    @JsonIgnore
    WsCmdType getType();

}
