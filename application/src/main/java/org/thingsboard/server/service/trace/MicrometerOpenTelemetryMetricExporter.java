/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.service.trace;

import io.micrometer.core.instrument.FunctionCounter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class MicrometerOpenTelemetryMetricExporter implements MetricExporter {
    private final MeterRegistry meterRegistry;
    private final String namePrefix;
    private final ConcurrentMap<String, AtomicLong> meterValues = new ConcurrentHashMap<>();

    public MicrometerOpenTelemetryMetricExporter(MeterRegistry meterRegistry, String namePrefix) {
        this.meterRegistry = meterRegistry;
        this.namePrefix = namePrefix == null ? "" : namePrefix;
    }

    @Override
    public CompletableResultCode export(Collection<MetricData> metrics) {
        try {
            for (MetricData metric : metrics) {
                switch (metric.getType()) {
                    case LONG_SUM -> {
                        boolean monotonic = metric.getLongSumData().isMonotonic();
                        for (LongPointData point : metric.getLongSumData().getPoints()) {
                            record(metric.getName(), point, point.getValue(), monotonic);
                        }
                    }
                    case LONG_GAUGE -> {
                        for (LongPointData point : metric.getLongGaugeData().getPoints()) {
                            record(metric.getName(), point, point.getValue(), false);
                        }
                    }
                    default -> {  }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to export OpenTelemetry metrics to Micrometer", e);
            return CompletableResultCode.ofFailure();
        }
        return CompletableResultCode.ofSuccess();
    }

    private void record(String name, PointData point, long value, boolean monotonic) {
        List<Tag> tags = toTags(point);
        String meterName = namePrefix + name;
        String key = meterName + tags;
        AtomicLong holder = meterValues.computeIfAbsent(key, k -> {
            AtomicLong backing = new AtomicLong();
            if (monotonic) {
                FunctionCounter.builder(meterName, backing, AtomicLong::doubleValue).tags(tags).register(meterRegistry);
            } else {
                Gauge.builder(meterName, backing, AtomicLong::doubleValue).tags(tags).register(meterRegistry);
            }
            return backing;
        });
        holder.set(value);
    }

    private static List<Tag> toTags(PointData point) {
        List<Tag> tags = new ArrayList<>();
        point.getAttributes().forEach((attrKey, attrValue) -> tags.add(Tag.of(attrKey.getKey(), String.valueOf(attrValue))));
        tags.sort(Comparator.comparing(Tag::getKey));
        return tags;
    }

    @Override
    public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
        return AggregationTemporality.CUMULATIVE;
    }

    @Override
    public CompletableResultCode flush() {
        return CompletableResultCode.ofSuccess();
    }

    @Override
    public CompletableResultCode shutdown() {
        return CompletableResultCode.ofSuccess();
    }
}
