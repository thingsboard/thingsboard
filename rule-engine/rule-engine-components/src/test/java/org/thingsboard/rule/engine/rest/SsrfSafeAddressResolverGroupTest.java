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
package org.thingsboard.rule.engine.rest;

import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.resolver.AddressResolver;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Promise;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.thingsboard.common.util.SsrfProtectionValidator;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ResourceLock("SsrfProtectionValidator") // to avoid race conditions when modifying SsrfProtectionValidator's static configuration
class SsrfSafeAddressResolverGroupTest {

    private static NioEventLoopGroup eventLoopGroup;

    @BeforeAll
    static void setUp() {
        eventLoopGroup = new NioEventLoopGroup(1);
    }

    @AfterAll
    static void tearDown() {
        eventLoopGroup.shutdownGracefully(0, 5, TimeUnit.SECONDS);
        SsrfProtectionValidator.setEnabled(false);
        SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
    }

    @BeforeEach
    void enableSsrf() {
        SsrfProtectionValidator.setEnabled(true);
        SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
    }

    @AfterEach
    void resetState() {
        SsrfProtectionValidator.setAllowedHosts(Collections.emptyList());
        SsrfProtectionValidator.setEnabled(false);
    }

    @Test
    void isBlockedAddressWorksForLoopback() throws Exception {
        assertThat(SsrfProtectionValidator.isBlockedAddress(InetAddress.getByName("127.0.0.1"))).isTrue();
        assertThat(SsrfProtectionValidator.isBlockedAddress(InetAddress.getByName("192.168.1.1"))).isTrue();
        assertThat(SsrfProtectionValidator.isBlockedAddress(InetAddress.getByName("8.8.8.8"))).isFalse();
    }

    @Test
    void resolvePublicIpSucceeds() throws Exception {
        EventExecutor executor = eventLoopGroup.next();
        AddressResolver<InetSocketAddress> resolver = SsrfSafeAddressResolverGroup.INSTANCE.getResolver(executor);
        Promise<InetSocketAddress> promise = executor.newPromise();

        executor.submit(() -> resolver.resolve(InetSocketAddress.createUnresolved("8.8.8.8", 80), promise));
        InetSocketAddress result = promise.get(10, TimeUnit.SECONDS);

        assertThat(result.getAddress()).isNotNull();
        assertThat(result.getAddress().getHostAddress()).isEqualTo("8.8.8.8");
    }

    @Test
    void resolveLoopbackFailsWhenSsrfEnabled() throws Exception {
        assertThat(SsrfProtectionValidator.isEnabled()).isTrue();

        EventExecutor executor = eventLoopGroup.next();
        AddressResolver<InetSocketAddress> resolver = SsrfSafeAddressResolverGroup.INSTANCE.getResolver(executor);
        Promise<InetSocketAddress> promise = executor.newPromise();

        executor.submit(() -> resolver.resolve(InetSocketAddress.createUnresolved("127.0.0.1", 80), promise));

        assertThatThrownBy(() -> promise.get(10, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasRootCauseInstanceOf(RuntimeException.class)
                .rootCause().hasMessageContaining("is not allowed");
    }

    @Test
    void resolvePrivateIpFailsWhenSsrfEnabled() throws Exception {
        assertThat(SsrfProtectionValidator.isEnabled()).isTrue();

        EventExecutor executor = eventLoopGroup.next();
        AddressResolver<InetSocketAddress> resolver = SsrfSafeAddressResolverGroup.INSTANCE.getResolver(executor);
        Promise<InetSocketAddress> promise = executor.newPromise();

        executor.submit(() -> resolver.resolve(InetSocketAddress.createUnresolved("192.168.1.1", 80), promise));

        assertThatThrownBy(() -> promise.get(10, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasRootCauseInstanceOf(RuntimeException.class)
                .rootCause().hasMessageContaining("is not allowed");
    }

    @Test
    void resolveAllowedPrivateIpSucceeds() throws Exception {
        SsrfProtectionValidator.setAllowedHosts(List.of("192.168.1.0/24"));

        EventExecutor executor = eventLoopGroup.next();
        AddressResolver<InetSocketAddress> resolver = SsrfSafeAddressResolverGroup.INSTANCE.getResolver(executor);
        Promise<InetSocketAddress> promise = executor.newPromise();

        executor.submit(() -> resolver.resolve(InetSocketAddress.createUnresolved("192.168.1.1", 80), promise));
        InetSocketAddress result = promise.get(10, TimeUnit.SECONDS);

        assertThat(result.getAddress().getHostAddress()).isEqualTo("192.168.1.1");
    }

    @Test
    void resolveAllPublicIpSucceeds() throws Exception {
        EventExecutor executor = eventLoopGroup.next();
        AddressResolver<InetSocketAddress> resolver = SsrfSafeAddressResolverGroup.INSTANCE.getResolver(executor);
        Promise<List<InetSocketAddress>> promise = executor.newPromise();

        executor.submit(() -> resolver.resolveAll(InetSocketAddress.createUnresolved("8.8.8.8", 80), promise));
        List<InetSocketAddress> results = promise.get(10, TimeUnit.SECONDS);

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getAddress().getHostAddress()).isEqualTo("8.8.8.8");
    }

    @Test
    void resolveAllPrivateIpFailsWhenSsrfEnabled() {
        assertThatThrownBy(() -> {
            EventExecutor executor = eventLoopGroup.next();
            AddressResolver<InetSocketAddress> resolver = SsrfSafeAddressResolverGroup.INSTANCE.getResolver(executor);
            Promise<List<InetSocketAddress>> promise = executor.newPromise();
            executor.submit(() -> resolver.resolveAll(InetSocketAddress.createUnresolved("127.0.0.1", 80), promise));
            promise.get(10, TimeUnit.SECONDS);
        }).isInstanceOf(ExecutionException.class)
                .hasRootCauseInstanceOf(RuntimeException.class)
                .rootCause().hasMessageContaining("is not allowed");
    }

}
