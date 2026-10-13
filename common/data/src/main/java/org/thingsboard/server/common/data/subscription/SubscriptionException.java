// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.subscription;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Map;

public class SubscriptionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private static final ObjectMapper mapper = new ObjectMapper();

    private SubscriptionErrorCode errorCode;
    private SubscriptionEntry entry;
    private JsonNode value;

    public SubscriptionException() {
        super();
    }

    public SubscriptionException(String message,
                                 SubscriptionErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public SubscriptionException(String message,
                                 SubscriptionErrorCode errorCode,
                                 Map<SubscriptionEntry, Long> entitiesCountsInfo) {
        super(message);
        this.errorCode = errorCode;
        this.value = mapper.valueToTree(entitiesCountsInfo);
    }

    public SubscriptionException(String message,
                                 SubscriptionErrorCode errorCode,
                                 JsonNode value) {
        super(message);
        this.errorCode = errorCode;
        this.value = value;
    }

    public SubscriptionException(String message, SubscriptionErrorCode errorCode, SubscriptionEntry entry, long value) {
        super(message);
        setValues(errorCode, entry, value);
    }

    public SubscriptionException(String message, Throwable cause, SubscriptionErrorCode errorCode, SubscriptionEntry entry, long value) {
        super(message, cause);
        setValues(errorCode, entry, value);
    }

    public SubscriptionException(Throwable cause, SubscriptionErrorCode errorCode, SubscriptionEntry entry, long value) {
        super(cause);
        setValues(errorCode, entry, value);
    }

    public SubscriptionException(SubscriptionErrorCode errorCode, SubscriptionEntry entry, long value) {
        super();
        setValues(errorCode, entry, value);
    }

    private void setValues(SubscriptionErrorCode errorCode, SubscriptionEntry entry, long value) {
        this.errorCode = errorCode;
        this.entry = entry;
        this.value = mapper.createObjectNode();
        ((ObjectNode) this.value).put("value", value);
    }

    public SubscriptionErrorCode getErrorCode() {
        return errorCode;
    }

    public SubscriptionEntry getEntry() {
        return entry;
    }

    public JsonNode getValue() {
        return value;
    }

}
