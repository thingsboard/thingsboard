// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api.tbel;

import com.google.common.util.concurrent.ListenableFuture;
import org.mvel2.ExecutionContext;
import org.thingsboard.script.api.TbScriptExecutionTask;


public class TbelScriptExecutionTask extends TbScriptExecutionTask {

    private final ExecutionContext context;

    public TbelScriptExecutionTask(ExecutionContext context, ListenableFuture<Object> resultFuture) {
        super(resultFuture);
        this.context = context;
    }

    @Override
    public void stop(){
        context.stop();
    }
}
