// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.mqtt;

import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@SuppressWarnings({"WeakerAccess", "unused"})
public final class MqttConnectResult {

    @Getter
    private final boolean success;
    @Getter
    private final MqttConnectReturnCode returnCode;
    @Getter
    private final ChannelFuture closeFuture;

    MqttConnectResult(boolean success, MqttConnectReturnCode returnCode, ChannelFuture closeFuture) {
        this.success = success;
        this.returnCode = returnCode;
        this.closeFuture = closeFuture;
    }

}
