// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.session;

import org.thingsboard.server.gen.transport.TransportProtos;

public interface LwM2MSessionManager {

    void register(TransportProtos.SessionInfoProto sessionInfo);

    void deregister(TransportProtos.SessionInfoProto sessionInfo);


}
