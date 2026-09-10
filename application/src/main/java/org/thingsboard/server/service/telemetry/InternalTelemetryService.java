// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.telemetry;

import com.google.common.util.concurrent.ListenableFuture;
import org.thingsboard.rule.engine.api.AttributesDeleteRequest;
import org.thingsboard.rule.engine.api.AttributesSaveRequest;
import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.rule.engine.api.TimeseriesDeleteRequest;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.common.data.kv.AttributesSaveResult;
import org.thingsboard.server.common.data.kv.TimeseriesSaveResult;

/**
 * Created by ashvayka on 27.03.18.
 */
public interface InternalTelemetryService extends RuleEngineTelemetryService {

    ListenableFuture<TimeseriesSaveResult> saveTimeseriesInternal(TimeseriesSaveRequest request);

    ListenableFuture<AttributesSaveResult> saveAttributesInternal(AttributesSaveRequest request);

    void deleteTimeseriesInternal(TimeseriesDeleteRequest request);

    void deleteAttributesInternal(AttributesDeleteRequest request);

}
