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
package org.thingsboard.server.transport.coap.session;

import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.network.ExchangeObserver;

public class CoapExchangeObserverProxy implements ExchangeObserver {

    private final ExchangeObserver proxy;
    private final CoapSessionCtx ctx;

    public CoapExchangeObserverProxy(ExchangeObserver proxy, CoapSessionCtx ctx) {
        super();
        this.proxy = proxy;
        this.ctx = ctx;
    }

    @Override
    public void completed(Exchange exchange) {
        proxy.completed(exchange);
        ctx.close();
    }

}
