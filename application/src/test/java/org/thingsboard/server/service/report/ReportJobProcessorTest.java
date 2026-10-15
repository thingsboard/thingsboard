// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.report;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.ReportJobConfiguration;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.job.task.Task;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.CsvReportTemplateConfig;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.security.model.token.AccessJwtToken;
import org.thingsboard.server.service.security.permission.OwnersCacheService;
import org.thingsboard.server.service.security.system.SystemSecurityService;

import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Covers the other half of the wiring gap: the {@code .nonProduction(subscriptionService.isDevelopment(...))}
 * builder call site in {@link ReportJobProcessor#process}, not just the context providers downstream of it
 * (see {@link LocalTbReportCtxProviderTest}/{@code RemoteTbReportCtxProviderTest}).
 * <p>
 * Only the dependencies this test actually stubs are declared. The rest of {@link ReportJobProcessor}'s
 * constructor arguments are left to {@code @InjectMocks} to fill with nulls: naming them just to satisfy a
 * positional constructor call would tie this test to the field order of a Lombok-generated constructor, where
 * an added or reordered dependency could silently swap two same-typed arguments.
 */
@ExtendWith(MockitoExtension.class)
public class ReportJobProcessorTest {

    @Mock
    private ReportTemplateService reportTemplateService;
    @Mock
    private SystemSecurityService systemSecurityService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private OwnersCacheService ownersCacheService;
    @Mock
    private TbApiUsageStateService apiUsageStateService;
    @Mock
    private UserService userService;

    @InjectMocks
    private ReportJobProcessor processor;

    @Test
    void testProcessTracksNonProductionTrue() throws Exception {
        ReportTask task = process(true);

        assertThat(task.isNonProduction()).isTrue();
    }

    @Test
    void testProcessTracksNonProductionFalse() throws Exception {
        ReportTask task = process(false);

        assertThat(task.isNonProduction()).isFalse();
    }

    private ReportTask process(boolean nonProduction) throws Exception {
        TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
        UserId userId = new UserId(UUID.randomUUID());
        ReportTemplateId reportTemplateId = new ReportTemplateId(UUID.randomUUID());

        // report-creation gate: a freshly built ApiUsageState has reportExecState == null, which
        // isReportCreationEnabled() treats as enabled (null != DISABLED), so process() doesn't bail out early.
        when(apiUsageStateService.getApiUsageState(tenantId)).thenReturn(new ApiUsageState());

        ReportTemplate reportTemplate = mock(ReportTemplate.class);
        when(reportTemplate.getId()).thenReturn(reportTemplateId);
        when(reportTemplate.getConfiguration()).thenReturn(CsvReportTemplateConfig.builder().build());
        when(reportTemplateService.findReportTemplateById(tenantId, reportTemplateId)).thenReturn(reportTemplate);

        User user = mock(User.class);
        when(userService.findUserById(tenantId, userId)).thenReturn(user);

        Claims claims = mock(Claims.class);
        when(claims.getExpiration()).thenReturn(new Date());
        AccessJwtToken accessToken = mock(AccessJwtToken.class);
        when(accessToken.getToken()).thenReturn("token");
        when(accessToken.getClaims()).thenReturn(claims);
        when(systemSecurityService.createUserAccessToken(tenantId, userId)).thenReturn(accessToken);

        when(ownersCacheService.getOwner(tenantId, userId)).thenReturn(tenantId);

        when(subscriptionService.isDevelopment(tenantId)).thenReturn(nonProduction);

        ReportJobConfiguration configuration = new ReportJobConfiguration();
        configuration.setReportTemplateId(reportTemplateId);
        configuration.setUserId(userId);
        configuration.setTasksKey("key");

        Job job = new Job();
        job.setTenantId(tenantId);
        job.setConfiguration(configuration);

        AtomicReference<Task<?>> captured = new AtomicReference<>();
        processor.process(job, captured::set);

        return (ReportTask) captured.get();
    }

}
