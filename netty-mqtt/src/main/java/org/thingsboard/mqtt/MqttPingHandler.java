/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.mqtt;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
final class MqttPingHandler extends ChannelInboundHandlerAdapter {

    private final int keepaliveSeconds;

    private ScheduledFuture<?> pingRespTimeout;

    MqttPingHandler(int keepaliveSeconds) {
        this.keepaliveSeconds = keepaliveSeconds;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (!(msg instanceof MqttMessage message)) {
            ctx.fireChannelRead(msg);
            return;
        }
        if (message.fixedHeader().messageType() == MqttMessageType.PINGREQ) {
            this.handlePingReq(ctx.channel());
        } else if (message.fixedHeader().messageType() == MqttMessageType.PINGRESP) {
            this.handlePingResp(ctx.channel());
        } else {
            ctx.fireChannelRead(ReferenceCountUtil.retain(msg));
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        super.userEventTriggered(ctx, evt);

        if (evt instanceof IdleStateEvent event) {
            switch (event.state()) {
                case READER_IDLE:
                    log.debug("[{}] No reads were performed for specified period for channel {}", event.state(), ctx.channel().id());
                    this.sendPingReq(ctx.channel(), event);
                    break;
                case WRITER_IDLE:
                    log.debug("[{}] No writes were performed for specified period for channel {}", event.state(), ctx.channel().id());
                    this.sendPingReq(ctx.channel(), event);
                    break;
            }
        }
    }

    private void sendPingReq(Channel channel, IdleStateEvent idleEvent) {
        log.trace("[{}] Sending ping request", channel.id());
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGREQ, false, MqttQoS.AT_MOST_ONCE, false, 0);
        channel.writeAndFlush(new MqttMessage(fixedHeader));

        if (this.pingRespTimeout == null) {
            log.trace("[{}] Scheduling disconnect due to {}", channel.id(), idleEvent);
            this.pingRespTimeout = channel.eventLoop().schedule(() -> {
                log.trace("[{}] Sending disconnect due to {}", channel.id(), idleEvent);
                MqttFixedHeader fixedHeader2 = new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0);
                channel.writeAndFlush(new MqttMessage(fixedHeader2)).addListener(ChannelFutureListener.CLOSE);
                //TODO: what do when the connection is closed ?
            }, this.keepaliveSeconds, TimeUnit.SECONDS);
        }
    }

    private void handlePingReq(Channel channel) {
        log.trace("[{}] Handling ping request", channel.id());
        MqttFixedHeader fixedHeader = new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0);
        channel.writeAndFlush(new MqttMessage(fixedHeader));
    }

    private void handlePingResp(Channel channel) {
        log.trace("[{}] Handling ping response", channel.id());
        if (this.pingRespTimeout != null && !this.pingRespTimeout.isCancelled() && !this.pingRespTimeout.isDone()) {
            log.trace("[{}] Cancelling disconnect due to idle event because ping response was received", channel.id());
            this.pingRespTimeout.cancel(true);
            this.pingRespTimeout = null;
        }
    }
}
