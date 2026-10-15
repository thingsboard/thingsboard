// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RecaptchaErrorCode {

    //reCAPTCHA verification errors
    MISSING_SECRET_KEY("missing-input-secret"),
    INVALID_SECRET_KEY("invalid-input-secret"),
    MISSING_USER_CAPTCHA_RESPONSE("missing-input-response"),
    INVALID_USER_CAPTCHA_RESPONSE("invalid-input-response"),

    //Custom errors
    MISSING_USERNAME_REQUEST_PARAMETER("missing-username-request-parameter"),
    MISSING_CAPTCHA_RESPONSE_PARAMETER("missing-captcha-response-parameter"),
    VALIDATION_HTTP_ERROR("validation-http-error");

    private final String text;

    RecaptchaErrorCode(String text) {
        this.text = text;
    }

    @JsonCreator
    private static RecaptchaErrorCode fromValue(String value) {
        if (value == null) {
            return null;
        }
        switch (value) {
            case "missing-input-secret":
                return MISSING_SECRET_KEY;
            case "invalid-input-secret":
                return INVALID_SECRET_KEY;
            case "missing-input-response":
                return MISSING_USER_CAPTCHA_RESPONSE;
            case "invalid-input-response":
                return INVALID_USER_CAPTCHA_RESPONSE;
            case "missing-username-request-parameter":
                return MISSING_USERNAME_REQUEST_PARAMETER;
            case "missing-captcha-response-parameter":
                return MISSING_CAPTCHA_RESPONSE_PARAMETER;
            default:
                throw new IllegalArgumentException("Invalid error code: " + value);
        }
    }

    @JsonValue
    public String getText() {
        return text;
    }
}