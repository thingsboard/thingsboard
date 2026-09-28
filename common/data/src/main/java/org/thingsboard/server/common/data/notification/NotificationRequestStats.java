// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.notification;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.notification.targets.NotificationRecipient;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

@Schema(description = "Notification request processing statistics")
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationRequestStats {

    private final ConcurrentMap<NotificationDeliveryMethod, AtomicInteger> sent;
    private final ConcurrentMap<NotificationDeliveryMethod, Map<String, String>> errors;
    private final AtomicInteger totalErrors;
    private String error;
    @JsonIgnore
    private final ConcurrentMap<NotificationDeliveryMethod, Set<Object>> processedRecipients;

    public NotificationRequestStats() {
        this.sent = new ConcurrentHashMap<>();
        this.errors = new ConcurrentHashMap<>();
        this.totalErrors = new AtomicInteger();
        this.processedRecipients = new ConcurrentHashMap<>();
    }

    @JsonCreator
    public NotificationRequestStats(@JsonProperty("sent") Map<NotificationDeliveryMethod, AtomicInteger> sent,
                                    @JsonProperty("errors") Map<NotificationDeliveryMethod, Map<String, String>> errors,
                                    @JsonProperty("totalErrors") Integer totalErrors,
                                    @JsonProperty("error") String error) {
        this.sent = new ConcurrentHashMap<>(sent);
        this.errors = new ConcurrentHashMap<>(errors);
        if (totalErrors == null) {
            if (errors != null) {
                totalErrors = errors.values().stream().mapToInt(Map::size).sum();
            } else {
                totalErrors = 0;
            }
        }
        this.totalErrors = new AtomicInteger(totalErrors);
        this.error = error;
        this.processedRecipients = new ConcurrentHashMap<>();
    }

    public void reportSent(NotificationDeliveryMethod deliveryMethod, NotificationRecipient recipient) {
        sent.computeIfAbsent(deliveryMethod, k -> new AtomicInteger()).incrementAndGet();
    }

    public void reportError(NotificationDeliveryMethod deliveryMethod, Throwable error, NotificationRecipient recipient) {
        reportError(deliveryMethod, error, recipient.getTitle());
    }

    public void reportError(NotificationDeliveryMethod deliveryMethod, Throwable error, String recipientTitle) {
        if (error instanceof AlreadySentException) {
            return;
        }
        String errorMessage = error.getMessage();
        if (errorMessage == null) {
            errorMessage = error.getClass().getSimpleName();
        }
        Map<String, String> errors = this.errors.computeIfAbsent(deliveryMethod, k -> new ConcurrentHashMap<>());
        if (errors.size() < 100) {
            errors.put(recipientTitle, errorMessage);
        }
        totalErrors.incrementAndGet();
    }

    public void reportGeneralError(Throwable error) {
        sent.keySet().forEach(deliveryMethod -> {
            reportError(deliveryMethod, error, "General");
        });
        this.error = error.getMessage();
    }

    public void reportProcessed(NotificationDeliveryMethod deliveryMethod, Object recipientId) {
        processedRecipients.computeIfAbsent(deliveryMethod, k -> ConcurrentHashMap.newKeySet()).add(recipientId);
    }

    public boolean contains(NotificationDeliveryMethod deliveryMethod, Object recipientId) {
        Set<Object> processedRecipients = this.processedRecipients.get(deliveryMethod);
        return processedRecipients != null && processedRecipients.contains(recipientId);
    }

    public int getTotalSent() {
        return sent.values().stream().mapToInt(AtomicInteger::get).sum();
    }

}
