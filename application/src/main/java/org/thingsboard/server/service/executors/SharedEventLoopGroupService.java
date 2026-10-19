// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.executors;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class SharedEventLoopGroupService {

    @Getter
    private EventLoopGroup sharedEventLoopGroup;

    @PostConstruct
    public void init() {
        this.sharedEventLoopGroup = new NioEventLoopGroup();
    }

    @PreDestroy
    public void destroy() {
        if (this.sharedEventLoopGroup != null) {
            this.sharedEventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        }
    }

}
