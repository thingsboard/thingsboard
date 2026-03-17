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

import io.netty.resolver.AddressResolver;
import io.netty.resolver.AddressResolverGroup;
import io.netty.resolver.DefaultAddressResolverGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import org.thingsboard.common.util.SsrfProtectionValidator;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Custom Netty {@link AddressResolverGroup} that validates every resolved IP address
 * against the SSRF block-list at connection time. This eliminates the DNS rebinding
 * TOCTOU gap where a hostname resolves to a safe IP during validation but to a
 * private/metadata IP when the actual connection is made.
 */
public final class SsrfSafeAddressResolverGroup extends AddressResolverGroup<InetSocketAddress> {

    public static final SsrfSafeAddressResolverGroup INSTANCE = new SsrfSafeAddressResolverGroup();

    private SsrfSafeAddressResolverGroup() {
    }

    @Override
    protected AddressResolver<InetSocketAddress> newResolver(EventExecutor executor) throws Exception {
        AddressResolver<InetSocketAddress> delegate = DefaultAddressResolverGroup.INSTANCE.getResolver(executor);
        return new SsrfValidatingResolver(executor, delegate);
    }

    private static final class SsrfValidatingResolver implements AddressResolver<InetSocketAddress> {

        private final EventExecutor executor;
        private final AddressResolver<InetSocketAddress> delegate;

        SsrfValidatingResolver(EventExecutor executor, AddressResolver<InetSocketAddress> delegate) {
            this.executor = executor;
            this.delegate = delegate;
        }

        @Override
        public boolean isSupported(SocketAddress address) {
            return delegate.isSupported(address);
        }

        @Override
        public boolean isResolved(SocketAddress address) {
            return delegate.isResolved(address);
        }

        @Override
        public Future<InetSocketAddress> resolve(SocketAddress address) {
            return resolve(address, executor.newPromise());
        }

        @Override
        public Future<InetSocketAddress> resolve(SocketAddress address, Promise<InetSocketAddress> promise) {
            delegate.resolve(address).addListener((Future<InetSocketAddress> future) -> {
                if (!future.isSuccess()) {
                    promise.tryFailure(future.cause());
                    return;
                }
                InetSocketAddress resolved = future.getNow();
                if (SsrfProtectionValidator.isEnabled() && isBlocked(resolved) && !isOriginalHostAllowed(address)) {
                    promise.tryFailure(new RuntimeException(
                            "URI is invalid: host '" + resolved.getAddress().getHostAddress() + "' is not allowed"));
                } else {
                    promise.trySuccess(resolved);
                }
            });
            return promise;
        }

        @Override
        public Future<List<InetSocketAddress>> resolveAll(SocketAddress address) {
            return resolveAll(address, executor.newPromise());
        }

        @Override
        public Future<List<InetSocketAddress>> resolveAll(SocketAddress address, Promise<List<InetSocketAddress>> promise) {
            delegate.resolveAll(address).addListener((Future<List<InetSocketAddress>> future) -> {
                if (!future.isSuccess()) {
                    promise.tryFailure(future.cause());
                    return;
                }
                List<InetSocketAddress> resolved = future.getNow();
                if (!SsrfProtectionValidator.isEnabled() || isOriginalHostAllowed(address)) {
                    promise.trySuccess(resolved);
                    return;
                }
                List<InetSocketAddress> safe = resolved.stream()
                        .filter(addr -> !isBlocked(addr))
                        .collect(Collectors.toList());
                if (safe.isEmpty()) {
                    String host = address instanceof InetSocketAddress isa ? isa.getHostString() : address.toString();
                    promise.tryFailure(new RuntimeException(
                            "URI is invalid: host '" + host + "' is not allowed"));
                } else {
                    promise.trySuccess(safe);
                }
            });
            return promise;
        }

        @Override
        public void close() {
            delegate.close();
        }

        private static boolean isBlocked(InetSocketAddress socketAddress) {
            InetAddress addr = socketAddress.getAddress();
            return addr != null && SsrfProtectionValidator.isBlockedAddress(addr);
        }

        private static boolean isOriginalHostAllowed(SocketAddress address) {
            if (address instanceof InetSocketAddress isa) {
                String host = isa.getHostString();
                return host != null && SsrfProtectionValidator.isHostnameAllowed(host);
            }
            return false;
        }
    }
}
