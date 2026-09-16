// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap.client;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;

import java.util.concurrent.atomic.AtomicInteger;

@Data
@RequiredArgsConstructor
public class TbCoapObservationState {

    private final CoapExchange exchange;
    private final String token;
    private final AtomicInteger observeCounter = new AtomicInteger(0);
    private volatile ObserveRelation observeRelation;

}
