// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.telemetry;

import com.google.common.util.concurrent.FutureCallback;
import jakarta.annotation.Nullable;
import lombok.Data;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * Created by ashvayka on 02.04.18.
 */
@Data
class TelemetryNodeCallback implements FutureCallback<Void> {
    private final TbContext ctx;
    private final TbMsg msg;

    @Override
    public void onSuccess(@Nullable Void result) {
        ctx.tellSuccess(msg);
    }

    @Override
    public void onFailure(Throwable t) {
        ctx.tellFailure(msg, t);
    }
}
