/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslHandler;
import io.netty.util.ResourceLeakDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.transport.SessionMsgProcessor;
import org.thingsboard.server.common.transport.auth.DeviceAuthService;
import org.thingsboard.server.transport.mqtt.adaptors.MqttTransportAdaptor;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.SSLEngine;
import java.util.concurrent.Executor;

/**
 * @author Andrew Shvayka
 */
@Service("MqttTransportService")
@Slf4j
public class MqttTransportService {

    private static final String V1 = "v1";
    private static final String DEVICE = "device";

    @Autowired(required = false)
    private ApplicationContext appContext;

    @Autowired(required = false)
    private SessionMsgProcessor processor;

    @Autowired(required = false)
    private DeviceAuthService authService;

    @Autowired(required = false)
    private MqttSslHandlerProvider sslHandlerProvider;

    @Value("${mqtt.bind_address}")
    private String host;
    @Value("${mqtt.bind_port}")
    private Integer port;
    @Value("${mqtt.adaptor}")
    private String adaptorName;

    private MqttTransportAdaptor adaptor;

    private Channel serverChannel;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    @PostConstruct
    public void init() throws Exception {
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
        log.info("Starting MQTT transport...");
        log.info("Lookup MQTT transport adaptor {}", adaptorName);
        this.adaptor = (MqttTransportAdaptor) appContext.getBean(adaptorName);

        log.info("Starting MQTT transport server");
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.TRACE))
                .childHandler(new MqttTransportServerInitializer(processor, authService, adaptor, sslHandlerProvider));

        serverChannel = b.bind(host, port).sync().channel();
        log.info("Mqtt transport started!");
    }

    @PreDestroy
    public void shutdown() throws InterruptedException {
        log.info("Stopping MQTT transport!");
        try {
            serverChannel.close().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
        log.info("MQTT transport stopped!");
    }
}
