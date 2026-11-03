// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf.ctx;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldState;

@Data
@NoArgsConstructor
public class CalculatedFieldEntityCtx {

    private CalculatedFieldEntityCtxId id;
    private CalculatedFieldState state;

    public CalculatedFieldEntityCtx(CalculatedFieldEntityCtxId id, CalculatedFieldState state) {
        this.id = id;
        this.state = state;
    }

}
