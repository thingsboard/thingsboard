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
package org.thingsboard.server.common.data.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.thingsboard.server.common.data.notification.targets.NotificationRecipient;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Data
public class NotificationRequestStats {

    private final Map<NotificationDeliveryMethod, AtomicInteger> sent;
    @JsonIgnore
    private final AtomicInteger totalSent;
    private final Map<NotificationDeliveryMethod, Map<String, String>> errors;
    private final AtomicInteger totalErrors;
    private String error;
    @JsonIgnore
    private final Map<NotificationDeliveryMethod, Set<Object>> processedRecipients;

    public NotificationRequestStats() {
        this.sent = new ConcurrentHashMap<>();
        this.totalSent = new AtomicInteger();
        this.errors = new ConcurrentHashMap<>();
        this.totalErrors = new AtomicInteger();
        this.processedRecipients = new ConcurrentHashMap<>();
    }

    @JsonCreator
    public NotificationRequestStats(@JsonProperty("sent") Map<NotificationDeliveryMethod, AtomicInteger> sent,
                                    @JsonProperty("errors") Map<NotificationDeliveryMethod, Map<String, String>> errors,
                                    @JsonProperty("totalErrors") Integer totalErrors,
                                    @JsonProperty("error") String error) {
        this.sent = sent;
        this.totalSent = null;
        this.errors = errors;
        if (totalErrors == null) {
            if (errors != null) {
                totalErrors = errors.values().stream().mapToInt(Map::size).sum();
            } else {
                totalErrors = 0;
            }
        }
        this.totalErrors = new AtomicInteger(totalErrors);
        this.error = error;
        this.processedRecipients = Collections.emptyMap();
    }

    public void reportSent(NotificationDeliveryMethod deliveryMethod, NotificationRecipient recipient) {
        sent.computeIfAbsent(deliveryMethod, k -> new AtomicInteger()).incrementAndGet();
        totalSent.incrementAndGet();
    }

    public void reportError(NotificationDeliveryMethod deliveryMethod, Throwable error, NotificationRecipient recipient) {
        if (error instanceof AlreadySentException) {
            return;
        }
        String errorMessage = error.getMessage();
        if (errorMessage == null) {
            errorMessage = error.getClass().getSimpleName();
        }
        Map<String, String> errors = this.errors.computeIfAbsent(deliveryMethod, k -> new ConcurrentHashMap<>());
        if (errors.size() < 100) {
            errors.put(recipient.getTitle(), errorMessage);
        }
        totalErrors.incrementAndGet();
    }

    public void reportProcessed(NotificationDeliveryMethod deliveryMethod, Object recipientId) {
        processedRecipients.computeIfAbsent(deliveryMethod, k -> ConcurrentHashMap.newKeySet()).add(recipientId);
    }

    public boolean contains(NotificationDeliveryMethod deliveryMethod, Object recipientId) {
        Set<Object> processedRecipients = this.processedRecipients.get(deliveryMethod);
        return processedRecipients != null && processedRecipients.contains(recipientId);
    }

}
