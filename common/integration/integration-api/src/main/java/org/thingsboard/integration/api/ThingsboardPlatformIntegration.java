// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.integration.api.data.IntegrationDownlinkMsg;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.integration.Integration;

public interface ThingsboardPlatformIntegration<T> {

    Integration getConfiguration();

    void validateConfiguration(Integration configuration, boolean allowLocalNetworkHosts) throws ThingsboardException;

    void checkConnection(Integration integration, IntegrationContext ctx) throws ThingsboardException;

    void init(TbIntegrationInitParams params) throws Exception;

    void update(TbIntegrationInitParams params) throws Exception;

    void destroy();

    void process(T msg);

    ListenableFuture<Void> processAsync(T msg);

    void onDownlinkMsg(IntegrationDownlinkMsg msg);

    IntegrationStatistics popStatistics();

}
