package org.thingsboard.server.service.trace;

import io.micrometer.core.instrument.Timer;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.stats.DefaultCounter;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.model.sql.ruleenginemonitoring.BufferedSpan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@ConditionalOnExpression(TraceConditions.TRACE_ENABLED)
public class BufferedTraceExporter implements SpanExporter {
    private final TraceBuffer buffer;
    private final TraceSpanMapper spanMapper;
    private final TraceExportPressureGate pressureGate;

    private final Timer exportTimer;
    private final DefaultCounter receivedSpansCounter;

    public BufferedTraceExporter(TraceBuffer buffer, TraceSpanMapper spanMapper, TraceExportPressureGate pressureGate,
                                 StatsFactory statsFactory) {
        this.buffer = buffer;
        this.spanMapper = spanMapper;
        this.pressureGate = pressureGate;
        this.exportTimer = statsFactory.createTimer("trace.export.duration");
        this.receivedSpansCounter = statsFactory.createDefaultCounter("trace.export.spans", "result", "received");
    }

    @Override
    public CompletableResultCode export(Collection<SpanData> spans) {
        if (CollectionUtils.isEmpty(spans)) {
            return CompletableResultCode.ofSuccess();
        }
        long exportStart = System.currentTimeMillis();
        receivedSpansCounter.add(spans.size());

        List<BufferedSpan> bufferedSpans = new ArrayList<>(spans.size());
        for (SpanData spanData : spans) {
            BufferedSpan bufferedSpan = spanMapper.toBufferedSpan(spanData, exportStart);
            if (bufferedSpan != null) {
                bufferedSpans.add(bufferedSpan);
            }
        }

        pressureGate.onExported(spans.size());

        try {
            buffer.append(bufferedSpans);
            log.info("Buffered Trace Exporter Stats: batchSpans = [{}] writeTimeMs = [{}]",
                    spans.size(), System.currentTimeMillis() - exportStart);
        } catch (Exception e) {
            log.error("Failed to persist span batch of size {}", bufferedSpans.size(), e);
        } finally {
            exportTimer.record(System.currentTimeMillis() - exportStart, TimeUnit.MILLISECONDS);
        }

        return CompletableResultCode.ofSuccess();
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
