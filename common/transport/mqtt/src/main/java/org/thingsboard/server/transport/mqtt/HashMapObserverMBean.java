// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.mqtt;

public interface HashMapObserverMBean {
    int getSize();

    long getGatewayCount(String unused);

    long getNonGatewayCount(String unused);

    String getSessionByUUID(String key);

    String getAllSessions(String key);

    String getSubscribedSessions(String unused);

    String getNonActiveSessions(String unused);

    String getActiveSessions(String unused);

    String getGatewayDeviceSessionContextConnectedSessions(String unused);

    String getDeviceAwareSessionContextNotConnectedSessions(String unused);
}
