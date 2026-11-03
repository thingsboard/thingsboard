// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api;

import com.google.common.util.concurrent.ListenableFuture;
import lombok.Getter;
import lombok.RequiredArgsConstructor;


@RequiredArgsConstructor
public abstract class TbScriptExecutionTask {

    @Getter
    private final ListenableFuture<Object> resultFuture;

    public abstract void stop();
}
