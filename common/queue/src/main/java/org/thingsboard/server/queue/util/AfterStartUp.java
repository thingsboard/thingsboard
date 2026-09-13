// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.queue.util;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.Order;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@EventListener(ApplicationReadyEvent.class)
@Order
public @interface AfterStartUp {

    int QUEUE_INFO_INITIALIZATION = 1;
    int DISCOVERY_SERVICE = 2;

    int STARTUP_SERVICE = 8;
    int ACTOR_SYSTEM = 9;

    int CF_READ_CF_SERVICE = 10;

    int REGULAR_SERVICE = 11;

    int BEFORE_TRANSPORT_SERVICE = Integer.MAX_VALUE - 1001;
    int TRANSPORT_SERVICE = Integer.MAX_VALUE - 1000;
    int AFTER_TRANSPORT_SERVICE = Integer.MAX_VALUE - 999;

    @AliasFor(annotation = Order.class, attribute = "value")
    int order();
}
