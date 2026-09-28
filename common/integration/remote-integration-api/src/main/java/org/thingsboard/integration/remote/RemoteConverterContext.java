// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.remote;

import com.google.protobuf.ByteString;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.integration.api.IntegrationRateLimitService;
import org.thingsboard.integration.api.converter.ConverterContext;
import org.thingsboard.integration.storage.EventStorage;
import org.thingsboard.server.common.data.JavaSerDesUtil;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.gen.integration.TbEventProto;
import org.thingsboard.server.gen.integration.TbEventSource;
import org.thingsboard.server.gen.integration.UplinkMsg;

import java.util.Optional;

@Data
@Slf4j
public class RemoteConverterContext implements ConverterContext {

    private final EventStorage eventStorage;
    private final boolean isUplink;
    private final String clientId;
    private final int port;

    @Override
    public String getServiceId() {
        return "[" + clientId + ":" + port + "]";
    }

    @Override
    public void saveEvent(Event event, IntegrationCallback<Void> callback) {
        TbEventSource source;
        if (isUplink) {
            source = TbEventSource.UPLINK_CONVERTER;
        } else {
            source = TbEventSource.DOWNLINK_CONVERTER;
        }
        eventStorage.write(UplinkMsg.newBuilder()
                .addEventsData(TbEventProto.newBuilder()
                        .setSource(source)
                        .setEvent(ByteString.copyFrom(JavaSerDesUtil.encode(event)))
                        .build()
                ).build(), callback);
    }

    @Override
    public Optional<IntegrationRateLimitService> getRateLimitService() {
        return Optional.empty();
    }
}
