// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf;

import com.google.common.util.concurrent.FutureCallback;
import org.thingsboard.rule.engine.api.AttributesDeleteRequest;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.RuleEngineCalculatedFieldQueueService;
import org.thingsboard.rule.engine.api.TimeseriesDeleteRequest;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.common.data.kv.AttributesSaveResult;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;

import java.util.List;

public interface CalculatedFieldQueueService extends RuleEngineCalculatedFieldQueueService {

    /**
     * Filter CFs based on the request entity. Push to the queue if any matching CF exist;
     *
     * @param request - telemetry save request;
     * @param callback
     */
    void pushRequestToQueue(TimeseriesSaveRequest request, TimeseriesSaveResult result, FutureCallback<Void> callback);

    void pushRequestToQueue(AttributesSaveRequest request, AttributesSaveResult result, FutureCallback<Void> callback);

    void pushRequestToQueue(AttributesDeleteRequest request, List<String> result, FutureCallback<Void> callback);

    void pushRequestToQueue(TimeseriesDeleteRequest request, List<String> result, FutureCallback<Void> callback);

}
