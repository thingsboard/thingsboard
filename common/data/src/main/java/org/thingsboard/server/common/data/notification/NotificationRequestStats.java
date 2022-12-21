/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.common.data.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.thingsboard.server.common.data.User;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class NotificationRequestStats {

    private final Map<NotificationDeliveryMethod, AtomicInteger> sent;
    private final Map<NotificationDeliveryMethod, Map<String, String>> errors;

    public NotificationRequestStats() {
        this.sent = new ConcurrentHashMap<>();
        this.errors = new ConcurrentHashMap<>();
    }

    @JsonCreator
    public NotificationRequestStats(@JsonProperty("sent") Map<NotificationDeliveryMethod, AtomicInteger> sent,
                                    @JsonProperty("errors") Map<NotificationDeliveryMethod, Map<String, String>> errors) {
        this.sent = sent;
        this.errors = errors;
    }

    public void reportSent(NotificationDeliveryMethod deliveryMethod) {
        sent.computeIfAbsent(deliveryMethod, k -> new AtomicInteger()).incrementAndGet();
    }

    public void reportError(NotificationDeliveryMethod deliveryMethod, User recipient, Throwable error) {
        if (error instanceof AlreadySentException) {
            return;
        }
        String errorMessage = error.getMessage();
        errors.computeIfAbsent(deliveryMethod, k -> new ConcurrentHashMap<>()).put(recipient.getEmail(), errorMessage);
    }

    public boolean contains(NotificationDeliveryMethod deliveryMethod) {
        return sent.containsKey(deliveryMethod) || errors.containsKey(deliveryMethod);
    }

}
