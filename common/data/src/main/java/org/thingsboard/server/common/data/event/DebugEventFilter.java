// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.event;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

@Data
@Schema
public abstract class DebugEventFilter implements EventFilter {

    @Schema(description = "String value representing the server name, identifier or ip address where the platform is running", example = "ip-172-31-24-152")
    protected String server;
    @Schema(description = "Boolean value to filter the errors", allowableValues = {"false", "true"})
    protected boolean isError;
    @Schema(description = "The case insensitive 'contains' filter based on error message", example = "not present in the DB")
    protected String errorStr;

    public void setIsError(boolean isError) {
        this.isError = isError;
    }

    @Override
    public boolean isNotEmpty() {
        return !StringUtils.isEmpty(server) || isError || !StringUtils.isEmpty(errorStr);
    }

}
