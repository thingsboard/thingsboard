// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.unmodifiableList;

public class RecaptchaValidationResult {

    private boolean success;
    private List<RecaptchaErrorCode> errorCodes = new ArrayList<>();

    @JsonCreator
    public RecaptchaValidationResult(
            @JsonProperty("success") boolean success,
            @JsonProperty("error-codes") List<RecaptchaErrorCode> errorCodes
    ) {
        this.success = success;
        this.errorCodes = errorCodes == null ? new ArrayList<RecaptchaErrorCode>() : errorCodes;
    }

    public boolean isSuccess() {
        return success;
    }

    @JsonIgnore
    public boolean isFailure() {
        return !success;
    }

    public List<RecaptchaErrorCode> getErrorCodes() {
        return unmodifiableList(errorCodes);
    }

    public boolean hasError(RecaptchaErrorCode error) {
        return errorCodes.contains(error);
    }

    @Override
    public String toString() {
        return "RecaptchaValidationResult{" +
                "success=" + success +
                ", errorCodes=" + errorCodes +
                '}';
    }

}
