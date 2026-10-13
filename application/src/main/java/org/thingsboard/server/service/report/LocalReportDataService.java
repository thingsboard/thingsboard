// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.report;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.permission.Operation;
import org.thingsboard.server.common.data.permission.Resource;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.timewindow.Interval;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.report.ReportService;
import org.thingsboard.server.dao.report.ReportTemplateService;
import org.thingsboard.server.dao.resource.ImageService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.datasource.ReportDataService;
import org.thingsboard.server.service.query.EntityQueryService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.telemetry.TbTelemetryService;

import java.util.Collection;
import java.util.List;

import static org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator.getIntervalTs;
import static org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator.getIntervalType;

@RequiredArgsConstructor
@TbCoreComponent
@Service
public class LocalReportDataService implements ReportDataService {

    private final EntityQueryService entityQueryService;
    private final AlarmService alarmService;
    private final TbTelemetryService tbTelemetryService;
    private final AccessControlService accessControlService;
    private final ReportTemplateService reportTemplateService;
    private final ImageService imageService;
    private final ReportService reportService;

    @Override
    public ReportTemplate findReportTemplate(ReportTemplateId templateId, TbReportCtx ctx) throws ThingsboardException {
        SecurityUser securityUser = getSecurityUser(ctx);
        ReportTemplate reportTemplate = checkNotNull(reportTemplateService.findReportTemplateById(securityUser.getTenantId(), templateId));
        accessControlService.checkPermission(securityUser, Resource.REPORT_TEMPLATE, Operation.READ, templateId, reportTemplate);
        return reportTemplate;
    }

    @Override
    public byte[] downloadImage(String type, String key, TbReportCtx ctx) throws ThingsboardException {
        SecurityUser securityUser = getSecurityUser(ctx);
        TenantId tenantId = "system".equals(type) ? TenantId.SYS_TENANT_ID : securityUser.getTenantId();
        TbResourceInfo imageInfo = checkNotNull(imageService.getImageInfoByTenantIdAndKey(tenantId, key));
        return checkNotNull(imageService.getImageData(tenantId, imageInfo.getId()));
    }

    @Override
    public byte[] downloadPublicImage(String publicKey, TbReportCtx ctx) throws ThingsboardException {
        SecurityUser securityUser = getSecurityUser(ctx);
        TbResourceInfo imageInfo = checkNotNull(imageService.getPublicImageInfoByKey(publicKey));
        return checkNotNull(imageService.getImageData(securityUser.getTenantId(), imageInfo.getId()));
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(EntityDataQuery query, TbReportCtx ctx) {
        return entityQueryService.findEntityDataByQuery(getSecurityUser(ctx), query);
    }

    @Override
    public Long countEntitiesByQuery(EntityCountQuery query, TbReportCtx ctx) {
        return entityQueryService.countEntitiesByQuery(getSecurityUser(ctx), query);
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQuery(AlarmDataQuery query, TbReportCtx ctx) {
        return entityQueryService.findAlarmDataByQuery(getSecurityUser(ctx), query);
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(AlarmDataQuery query, Collection<EntityId> entityIds, TbReportCtx ctx) {
        SecurityUser securityUser = getSecurityUser(ctx);
        return alarmService.findAlarmDataByQueryForEntities(securityUser.getTenantId(), securityUser.getUserPermissions(), query, entityIds);
    }

    @Override
    public Long countAlarmsByQuery(AlarmCountQuery query, TbReportCtx ctx) {
        return entityQueryService.countAlarmsByQuery(getSecurityUser(ctx), query);
    }

    @SneakyThrows
    @Override
    public List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long startTs, Long endTs, Interval interval, String timeZone, Aggregation agg, SortOrder.Direction sortOrder,
                                         Integer limit, boolean useStrictDataTypes, TbReportCtx ctx) {
        return tbTelemetryService.getTimeseries(entityId, keys, startTs, endTs, getIntervalType(interval), getIntervalTs(interval), timeZone, limit, agg, sortOrder.name(), useStrictDataTypes, getSecurityUser(ctx)).get(); // .get() will be interrupted on task processing timeout
    }

    @SneakyThrows
    @Override
    public List<ReadTsKvQueryResult> findTimeseriesByQueries(EntityId entityId, List<BaseReadTsKvQuery> queries, TbReportCtx ctx) {
        return tbTelemetryService.getTimeseriesByReadQueries(entityId, queries, getSecurityUser(ctx)).get();
    }

    @Override
    public Report createReport(Report report, byte[] data, TbReportCtx ctx) throws ThingsboardException {
        accessControlService.checkPermission(getSecurityUser(ctx), Resource.REPORT, Operation.CREATE);
        return reportService.createReport(report, data);
    }

    private SecurityUser getSecurityUser(TbReportCtx ctx) {
        return ((LocalTbReportCtxProvider.LocalTbReportCtx) ctx).getSecurityUser();
    }

    private <T> T checkNotNull(T reference) throws ThingsboardException {
        if (reference == null) {
            throw new ThingsboardException("Requested item wasn't found!", ThingsboardErrorCode.ITEM_NOT_FOUND);
        }
        return reference;
    }
}
