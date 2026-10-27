// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.context;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;

import java.io.IOException;

import static org.thingsboard.server.report.util.ReportUtils.formatTimestamp;

@RequiredArgsConstructor
@ConditionalOnExpression("'${service.type:null}' == 'tb-report'")
@Service
public class RemoteTbReportCtxProvider implements TbReportCtxProvider {

    @Value("${service.tb_core.base_url:http://localhost:${server.port}}")
    private String tbCoreBaseUrl;

    private final TbelInvokeService tbelInvokeService;

    @Override
    public TbReportCtx newContext(ReportTask task) {
        return RemoteTbReportCtx.builder()
                .tenantId(task.getTenantId())
                .tbelInvokeService(tbelInvokeService)
                .configuration(task.getReportTemplateConfig())
                .userId(task.getUserId())
                .userOwnerId(task.getUserOwnerId())
                .timeZone(task.getTimezone())
                .accessToken(task.getAccessToken())
                .accessTokenExpTs(task.getAccessTokenExpirationTs())
                .restClient(new RestClient(new RestTemplate(), tbCoreBaseUrl, RestClient.AuthType.JWT, task.getAccessToken()))
                .reportCreatedTime(formatTimestamp(System.currentTimeMillis(), task.getReportTemplateConfig().getTimeDataPattern(), task.getTimezone()))
                .nonProduction(task.isNonProduction())
                .build();
    }

    @Data
    @SuperBuilder(toBuilder = true)
    public static class RemoteTbReportCtx extends TbReportCtx {

        private final RestClient restClient;

        @Override
        public void close() throws IOException {
            super.close();
            restClient.close();
        }

        @Override
        public TbReportCtx createSubReportCxt(ReportTemplateConfig reportTemplateConfig) {
            RemoteTbReportCtx copy = this.toBuilder().configuration(reportTemplateConfig).build();

            copy.getParams().putAll(this.getParams());

            return copy;
        }

    }

}
