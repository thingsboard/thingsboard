// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api;

import com.google.common.util.concurrent.FutureCallback;

public interface RuleEngineCalculatedFieldQueueService {

    void pushRequestToQueue(TimeseriesSaveRequest request, FutureCallback<Void> callback);

    void pushRequestToQueue(AttributesSaveRequest request, FutureCallback<Void> callback);

}
