// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

@Data
public abstract class DebugEventFilter implements EventFilter {

    @Schema(description = "String value representing the server name, identifier or ip address where the platform is running", example = "ip-172-31-24-152")
    private String server;
    @Schema(description = "Boolean value to filter the errors", allowableValues = {"false", "true"})
    @JsonProperty("isError")
    private boolean isError;
    @Schema(description = "The case insensitive 'contains' filter based on error message", example = "not present in the DB")
    private String errorStr;

    @JsonProperty("isError")
    public boolean isError() {
        return isError;
    }

    @JsonProperty("isError")
    public void setIsError(boolean isError) {
        this.isError = isError;
    }

    @Override
    public boolean isNotEmpty() {
        return !StringUtils.isEmpty(server) || !StringUtils.isEmpty(errorStr) || isError;
    }

}
