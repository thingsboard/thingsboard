/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.monitoring.service.transport.impl;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.monitoring.config.transport.Lwm2mTransportMonitoringConfig;
import org.thingsboard.monitoring.config.transport.TransportMonitoringTarget;
import org.thingsboard.monitoring.metrics.ProbeMetricsRecorder;
import org.thingsboard.monitoring.service.MonitoringReporter;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

public class Lwm2mTransportHealthCheckerTest {

    @Test
    public void checkAccepted_isNoOp_neverRecordsAcceptedProbeOrReports() {
        // checkAccepted() is a no-op for LwM2M (see override) - must never touch the metric or alerting
        ProbeMetricsRecorder probeMetricsRecorder = mock(ProbeMetricsRecorder.class);
        MonitoringReporter reporter = mock(MonitoringReporter.class);
        Lwm2mTransportHealthChecker checker = new Lwm2mTransportHealthChecker(
                new Lwm2mTransportMonitoringConfig(), new TransportMonitoringTarget());
        ReflectionTestUtils.setField(checker, "probeMetricsRecorder", probeMetricsRecorder);
        ReflectionTestUtils.setField(checker, "reporter", reporter);

        checker.checkAccepted();

        verifyNoInteractions(probeMetricsRecorder);
        verifyNoInteractions(reporter);
    }

}
