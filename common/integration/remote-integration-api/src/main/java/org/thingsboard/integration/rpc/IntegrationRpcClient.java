// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.rpc;

import org.thingsboard.server.gen.integration.ConverterConfigurationProto;
import org.thingsboard.server.gen.integration.DeviceDownlinkDataProto;
import org.thingsboard.server.gen.integration.IntegrationConfigurationProto;

import java.util.function.Consumer;

public interface IntegrationRpcClient {

    void connect(String integrationKey, String integrationSecret, String serviceId, Consumer<IntegrationConfigurationProto> onIntegrationUpdate,
                 Consumer<ConverterConfigurationProto> onConverterUpdate, Consumer<DeviceDownlinkDataProto> onDownlink, Consumer<Exception> onError);

    void disconnect() throws InterruptedException;

    void handleMsgs() throws InterruptedException;

}
