// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.report;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.context.TbReportCtxProvider;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.model.token.JwtTokenFactory;

import static org.thingsboard.server.report.util.ReportUtils.formatTimestamp;

@RequiredArgsConstructor
@Primary
@Service
public class LocalTbReportCtxProvider implements TbReportCtxProvider {

    private final JwtTokenFactory tokenFactory;
    private final TbelInvokeService tbelInvokeService;

    @Override
    public LocalTbReportCtx newContext(ReportTask task) {
        SecurityUser securityUser = tokenFactory.parseAccessJwtToken(task.getAccessToken());
        return LocalTbReportCtx.builder()
                .tenantId(securityUser.getTenantId())
                .tbelInvokeService(tbelInvokeService)
                .configuration(task.getReportTemplateConfig())
                .userId(task.getUserId())
                .userOwnerId(task.getUserOwnerId())
                .timeZone(task.getTimezone())
                .accessToken(task.getAccessToken())
                .accessTokenExpTs(task.getAccessTokenExpirationTs())
                .securityUser(securityUser)
                .reportCreatedTime(formatTimestamp(System.currentTimeMillis(), task.getReportTemplateConfig().getTimeDataPattern(), task.getTimezone()))
                .nonProduction(task.isNonProduction())
                .build();
    }

    @Data
    @SuperBuilder(toBuilder = true)
    public static class LocalTbReportCtx extends TbReportCtx {

        private final SecurityUser securityUser;

        @Override
        public TbReportCtx createSubReportCxt(ReportTemplateConfig reportTemplateConfig) {
            LocalTbReportCtx copy = this.toBuilder().configuration(reportTemplateConfig).build();

            copy.getParams().putAll(this.getParams());

            return copy;
        }
    }

}
