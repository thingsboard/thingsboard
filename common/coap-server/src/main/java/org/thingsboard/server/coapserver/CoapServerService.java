// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.coapserver;

import org.eclipse.californium.core.CoapServer;
import org.eclipse.californium.core.server.resources.Resource;

import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

public interface CoapServerService {

    CoapServer getCoapServer() throws UnknownHostException;

    Resource addResourceHierarchicallyAndReturnLast(List<String> resourceHierarchy) throws UnknownHostException;

    boolean isDtlsEnabled();

    ConcurrentMap<TbCoapDtlsSessionKey, TbCoapDtlsSessionInfo> getDtlsSessionsMap();
}
