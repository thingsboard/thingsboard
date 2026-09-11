// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.actors;

import lombok.Data;

import java.util.concurrent.ExecutorService;

@Data
class Dispatcher {

    private final String dispatcherId;
    private final ExecutorService executor;

}
