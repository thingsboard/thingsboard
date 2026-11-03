// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api;

public interface RuleEngineTelemetryService {

    void saveTimeseries(TimeseriesSaveRequest request);

    void saveAttributes(AttributesSaveRequest request);

    void deleteTimeseries(TimeseriesDeleteRequest request);

    void deleteAttributes(AttributesDeleteRequest request);

}
