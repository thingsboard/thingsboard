// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.context;

import org.junit.jupiter.api.Test;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Covers the wiring between {@link ReportTask#isNonProduction()} and {@link TbReportCtx#isNonProduction()}:
 * the realistic regression is a future {@code newContext}/{@code createSubReportCxt} builder call site that
 * forgets to copy the flag and silently defaults to {@code false}.
 */
public class RemoteTbReportCtxProviderTest {

    @Test
    void testNewContextAndSubReportCtxTrackNonProductionTrue() {
        TbReportCtx ctx = newContext(true);

        assertThat(ctx.isNonProduction()).isTrue();
        assertThat(ctx.createSubReportCxt(ctx.getConfiguration()).isNonProduction()).isTrue();
    }

    @Test
    void testNewContextTracksNonProductionFalse() {
        TbReportCtx ctx = newContext(false);

        assertThat(ctx.isNonProduction()).isFalse();
        assertThat(ctx.createSubReportCxt(ctx.getConfiguration()).isNonProduction()).isFalse();
    }

    private TbReportCtx newContext(boolean nonProduction) {
        RemoteTbReportCtxProvider provider = new RemoteTbReportCtxProvider(mock(TbelInvokeService.class));

        ReportTask task = ReportTask.builder()
                .tenantId(TenantId.fromUUID(UUID.randomUUID()))
                .reportTemplateConfig(CsvReportTemplateConfig.builder().timeDataPattern("yyyy-MM-dd").build())
                .accessToken("token")
                .nonProduction(nonProduction)
                .build();

        return provider.newContext(task);
    }

}
