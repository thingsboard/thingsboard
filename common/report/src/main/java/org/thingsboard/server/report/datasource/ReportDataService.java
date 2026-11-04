// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.datasource;

import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.kv.Aggregation;
import org.thingsboard.server.common.data.kv.BaseReadTsKvQuery;
import org.thingsboard.server.common.data.kv.ReadTsKvQueryResult;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.configuration.timewindow.Interval;
import org.thingsboard.server.report.context.TbReportCtx;

import java.util.Collection;
import java.util.List;

public interface ReportDataService {

    ReportTemplate findReportTemplate(ReportTemplateId templateId, TbReportCtx ctx) throws ThingsboardException;

    byte[] downloadImage(String type, String key, TbReportCtx ctx) throws ThingsboardException;

    byte[] downloadPublicImage(String publicKey, TbReportCtx ctx) throws ThingsboardException;

    PageData<EntityData> findEntityDataByQuery(EntityDataQuery query, TbReportCtx ctx);

    Long countEntitiesByQuery(EntityCountQuery query, TbReportCtx ctx);

    PageData<AlarmData> findAlarmDataByQuery(AlarmDataQuery query, TbReportCtx ctx);

    PageData<AlarmData> findAlarmDataByQueryForEntities(AlarmDataQuery query, Collection<EntityId> entityIds, TbReportCtx ctx);

    Long countAlarmsByQuery(AlarmCountQuery query, TbReportCtx ctx);

    List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long startTs, Long endTs,
                                  Interval interval, String timeZone, Aggregation agg, SortOrder.Direction sortOrder,
                                  Integer limit, boolean useStrictDataTypes, TbReportCtx ctx);

    List<ReadTsKvQueryResult> findTimeseriesByQueries(EntityId entityId, List<BaseReadTsKvQuery> queries, TbReportCtx ctx);

    Report createReport(Report report, byte[] data, TbReportCtx ctx) throws ThingsboardException;

}
