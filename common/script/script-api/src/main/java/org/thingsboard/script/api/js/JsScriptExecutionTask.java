// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api.js;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.script.api.TbScriptExecutionTask;

public class JsScriptExecutionTask extends TbScriptExecutionTask {

    public JsScriptExecutionTask(ListenableFuture<Object> resultFuture) {
        super(resultFuture);
    }

    @Override
    public void stop() {
        // do nothing
    }
}
