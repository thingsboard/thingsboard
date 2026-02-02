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
