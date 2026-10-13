// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.datasource;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
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
import org.thingsboard.server.report.context.RemoteTbReportCtxProvider;
import org.thingsboard.server.report.context.TbReportCtx;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator.getIntervalTs;
import static org.thingsboard.server.common.data.report.configuration.timewindow.TimeIntervalCalculator.getIntervalType;

@ConditionalOnExpression("'${service.type:null}' == 'tb-report'")
@Service
public class RemoteReportDataService implements ReportDataService {

    @Override
    public ReportTemplate findReportTemplate(ReportTemplateId templateId, TbReportCtx ctx) {
        try {
            return getRestClient(ctx).findReportTemplate(templateId);
        } catch (RestClientResponseException e) {
            throw handleRestClientException(e);
        }
    }

    @Override
    public byte[] downloadImage(String type, String key, TbReportCtx ctx) throws ThingsboardException {
        RestClient restClient = getRestClient(ctx);
        try {
            return restClient.downloadImage(type, key);
        } catch (IOException e) {
            throw new ThingsboardException("Failed to download image", e, ThingsboardErrorCode.GENERAL);
        } catch (RestClientResponseException e) {
            throw handleRestClientException(e);
        }
    }

    @Override
    public byte[] downloadPublicImage(String publicKey, TbReportCtx ctx) throws ThingsboardException {
        RestClient restClient = getRestClient(ctx);
        try {
            return restClient.downloadPublicImage(publicKey);
        } catch (IOException e) {
            throw new ThingsboardException("Failed to download public image", e, ThingsboardErrorCode.GENERAL);
        } catch (RestClientResponseException e) {
            throw handleRestClientException(e);
        }
    }

    @Override
    public PageData<EntityData> findEntityDataByQuery(EntityDataQuery query, TbReportCtx ctx) {
        try {
            return getRestClient(ctx).findEntityDataByQuery(query);
        } catch (RestClientResponseException e) {
            throw handleRestClientException(e);
        }
    }

    @Override
    public Long countEntitiesByQuery(EntityCountQuery query, TbReportCtx ctx) {
        try {
            return getRestClient(ctx).countEntitiesByQuery(query);
        } catch (RestClientResponseException e) {
            throw handleRestClientException(e);
        }
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQuery(AlarmDataQuery query, TbReportCtx ctx) {
        try {
            return getRestClient(ctx).findAlarmDataByQuery(query);
        } catch (RestClientResponseException e) {
            throw handleRestClientException(e);
        }
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(AlarmDataQuery query, Collection<EntityId> entityIds, TbReportCtx ctx) {
        try {
            return getRestClient(ctx).findAlarmDataByQuery(query);
        } catch (RestClientResponseException e) {
            throw handleRestClientException(e);
        }
    }

    @Override
    public Long countAlarmsByQuery(AlarmCountQuery query, TbReportCtx ctx) {
        try {
            return getRestClient(ctx).countAlarmsByQuery(query);
        } catch (RestClientResponseException e) {
            throw handleRestClientException(e);
        }
    }

    @Override
    public List<TsKvEntry> getTimeseries(EntityId entityId, List<String> keys, Long startTs, Long endTs, Interval interval, String timeZone, Aggregation agg, SortOrder.Direction sortOrder,
                                         Integer limit, boolean useStrictDataTypes, TbReportCtx ctx) {
        try {
            return getRestClient(ctx).getTimeseries(entityId, keys, getIntervalTs(interval), getIntervalType(interval), timeZone, agg, sortOrder, startTs, endTs, limit, useStrictDataTypes);
        } catch (RestClientResponseException e) {
            throw handleRestClientException(e);
        }
    }

    @Override
    public List<ReadTsKvQueryResult> findTimeseriesByQueries(EntityId entityId, List<BaseReadTsKvQuery> queries, TbReportCtx ctx) {
        try {
            return getRestClient(ctx).getTimeseriesByQueries(entityId, queries);
        } catch (RestClientResponseException e) {
            throw handleRestClientException(e);
        }
    }

    @Override
    public Report createReport(Report report, byte[] data, TbReportCtx ctx) {
        try {
            return getRestClient(ctx).createReport(report, data);
        } catch (RestClientResponseException e) {
            throw handleRestClientException(e);
        }
    }

    private RuntimeException handleRestClientException(RestClientResponseException e) {
        return new RuntimeException(extractErrorMessage(e.getResponseBodyAsString()), e);
    }

    private String extractErrorMessage(String responseBody) {
        try {
            JsonNode root = JacksonUtil.toJsonNode(responseBody);
            if (root != null && root.hasNonNull("message")) {
                return root.path("message").asText(responseBody);
            }
            return responseBody;
        } catch (Exception e) {
            return responseBody;
        }
    }

    private RestClient getRestClient(TbReportCtx ctx) {
        return ((RemoteTbReportCtxProvider.RemoteTbReportCtx) ctx).getRestClient();
    }

}
