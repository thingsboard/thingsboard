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
package org.thingsboard.server.transport.mqtt;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.ssl.SslHandler;
import org.thingsboard.server.transport.mqtt.limits.IpFilter;
import org.thingsboard.server.transport.mqtt.limits.ProxyIpFilter;

public class MqttTransportServerInitializer extends ChannelInitializer<SocketChannel> {

    private final MqttTransportContext context;
    private final boolean sslEnabled;

    public MqttTransportServerInitializer(MqttTransportContext context, boolean sslEnabled) {
        this.context = context;
        this.sslEnabled = sslEnabled;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        SslHandler sslHandler = null;
        if (context.isProxyEnabled()) {
            pipeline.addLast("proxy", new HAProxyMessageDecoder());
            pipeline.addLast("ipFilter", new ProxyIpFilter(context));
        } else {
            pipeline.addLast("ipFilter", new IpFilter(context));
        }
        if (sslEnabled && context.getSslHandlerProvider() != null) {
            sslHandler = context.getSslHandlerProvider().getSslHandler();
            pipeline.addLast(sslHandler);
        }
        pipeline.addLast("decoder", new MqttDecoder(context.getMaxPayloadSize()));
        pipeline.addLast("encoder", MqttEncoder.INSTANCE);

        MqttTransportHandler handler = new MqttTransportHandler(context, sslHandler);

        pipeline.addLast(handler);
        ch.closeFuture().addListener(handler);
    }

}
