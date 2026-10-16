// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.coapserver;

import org.eclipse.californium.core.CoapServer;

import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentMap;

public interface CoapServerService {

    CoapServer getCoapServer() throws UnknownHostException;

    ConcurrentMap<TbCoapDtlsSessionKey, TbCoapDtlsSessionInfo> getDtlsSessionsMap();
}
