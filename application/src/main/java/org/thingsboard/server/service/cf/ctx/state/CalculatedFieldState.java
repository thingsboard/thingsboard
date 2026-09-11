// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx.state;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.service.cf.CalculatedFieldResult;
import org.thingsboard.server.service.cf.ctx.CalculatedFieldEntityCtxId;

import java.util.List;
import java.util.Map;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SimpleCalculatedFieldState.class, name = "SIMPLE"),
        @JsonSubTypes.Type(value = ScriptCalculatedFieldState.class, name = "SCRIPT"),
})
public interface CalculatedFieldState {

    @JsonIgnore
    CalculatedFieldType getType();

    Map<String, ArgumentEntry> getArguments();

    long getLatestTimestamp();

    void setRequiredArguments(List<String> requiredArguments);

    boolean updateState(CalculatedFieldCtx ctx, Map<String, ArgumentEntry> argumentValues);

    ListenableFuture<CalculatedFieldResult> performCalculation(CalculatedFieldCtx ctx);

    @JsonIgnore
    boolean isReady();

    boolean isSizeExceedsLimit();

    @JsonIgnore
    default boolean isSizeOk() {
        return !isSizeExceedsLimit();
    }

    void checkStateSize(CalculatedFieldEntityCtxId ctxId, long maxStateSize);

    void checkArgumentSize(String name, ArgumentEntry entry, CalculatedFieldCtx ctx);

}
