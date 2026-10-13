// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.report.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.task.ReportTask;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.configuration.DataSource;
import org.thingsboard.server.common.data.report.configuration.ReportTemplateConfig;
import org.thingsboard.server.common.data.report.configuration.components.AlarmTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.EntityTableComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.ReportComponentType;
import org.thingsboard.server.common.data.report.configuration.components.SubReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TableReportComponent;
import org.thingsboard.server.common.data.report.configuration.components.TimeseriesTableComponent;
import org.thingsboard.server.report.context.ComponentData;
import org.thingsboard.server.report.context.TbReportCtx;
import org.thingsboard.server.report.renderer.CsvReportComponentRenderer;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.thingsboard.server.common.data.util.DataSourceUtils.entityDataFromEntityId;
import static org.thingsboard.server.report.util.CsvUtils.generateCsv;
import static org.thingsboard.server.report.util.ReportUtils.getSingleDataSource;
import static org.thingsboard.server.report.util.ReportUtils.prepareReportName;

@Service
@Slf4j
public class CsvReportService extends AbstractReportService {

    private final Map<ReportComponentType, CsvReportComponentRenderer<TableReportComponent>> componentsRenderers = new HashMap<>();

    // Package-private so the unit test can construct the service directly; Spring injects through it either way.
    CsvReportService(List<CsvReportComponentRenderer> renderers) {
        renderers.forEach(renderer -> {
            ReportComponentType type = renderer.getType();
            if (type != null) {
                this.componentsRenderers.put(type, renderer);
            }
        });
    }

    @Override
    public ReportData generateReport(ReportTask task, TbReportCtx ctx) {
        TenantId tenantId = task.getTenantId();

        log.trace("[{}] Executing generateReport, reportRequest [{}]", tenantId, task);
        ReportTemplateConfig configuration = task.getReportTemplateConfig();

        EntityData stateEntity = task.getOriginator() != null ? entityDataFromEntityId(task.getOriginator()) : null;

        List<List<String>> content = renderContent(ctx, stateEntity);
        if (ctx.isNonProduction()) {
            content.add(0, List.of(DataConstants.NON_PRODUCTION_NOTICE));
        }
        byte[] csvBytes = generateCsv(content);
        String reportName = prepareReportName(configuration.getNamePattern(), new Date(), task.getTimezone(), TbReportFormat.CSV.getExtension());

        return ReportData.builder()
                .data(csvBytes)
                .contentType(configuration.getFormat().getContentType())
                .name(reportName)
                .build();
    }

    private List<List<String>> renderContent(TbReportCtx ctx, EntityData stateEntity) {
        List<List<String>> content = new LinkedList<>();
        List<ReportComponent> components = ctx.getConfiguration().getComponents();
        EntityId stateEntityId = stateEntity != null ? stateEntity.getEntityId() : null;
        for (ReportComponent component : components) {
            switch (component.getType()) {
                case SUB_REPORT -> content.addAll(renderSubReport(ctx, (SubReportComponent) component, stateEntityId));
                case TIME_SERIES_TABLE -> content.addAll(renderTimeseriesTables(ctx, (TableReportComponent) component, stateEntityId));
                case ALARM_TABLE, ENTITY_TABLE -> content.addAll(renderTableComponent(ctx, (TableReportComponent) component, stateEntity));
                default -> throw new IllegalArgumentException("Unsupported component type: " + component.getType());
            }
        }
        return content;
    }

    private List<List<String>> renderTableComponent(TbReportCtx ctx, TableReportComponent component, EntityData stateEntity) {
        try {
            ComponentData componentData = getComponentData(ctx, component, stateEntity);
            return componentsRenderers.get(component.getType()).render(component, componentData);
        } catch (Exception e) {
            log.error("[{}] Failed to render component of type [{}]", ctx.getTenantId(), component.getType(), e);
            return renderError("Failed to render component of type: " + component.getType(), e);
        }
    }

    private List<List<String>> renderSubReport(TbReportCtx ctx, SubReportComponent subReportComponent, EntityId stateEntityId) {
        ReportTemplateId templateId = subReportComponent.getTemplateId();
        if (templateId == null) {
            return renderError("Report template id is not configured for Subreport component");
        }
        List<List<String>> content = new LinkedList<>();
        try {
            ReportTemplate reportTemplate = dataService.findReportTemplate(templateId, ctx);
            if (reportTemplate == null) {
                return renderError("Template with id " + templateId + " not found. Please check the configuration.");
            }
            TbReportCtx subReportCtx = ctx.createSubReportCxt(reportTemplate.getConfiguration());
            List<EntityData> entities = getSubReportEntities(ctx, subReportComponent, stateEntityId);
            for (EntityData entity : entities) {
                content.addAll(renderContent(subReportCtx, entity));
            }
            return content;
        } catch (Exception e) {
            log.error("[{}] Failed to render Subreport, template id: {}", ctx.getTenantId(), templateId, e);
            return renderError("Failed to render sub-report " + templateId, e);
        }
    }

    private List<List<String>> renderTimeseriesTables(TbReportCtx ctx, TableReportComponent component, EntityId stateEntityId) {
        List<List<String>> content = new LinkedList<>();
        Optional<DataSource> dataSource = getSingleDataSource(component);
        if (dataSource.isEmpty()) {
            return renderError("Data source is not configured for time series table");
        }
        DataSource ds = dataSource.get();
        if (ds.getDataKeys().isEmpty()) {
            return renderError("At least one time series column should be specified for time series table");
        }
        DataSource latestDataSource = DataSource.builder()
                .type(ds.getType())
                .deviceId(ds.getDeviceId())
                .entityAliasId(ds.getEntityAliasId())
                .filterId(ds.getFilterId())
                .dataKeys(ds.getLatestDataKeys()).build();
        List<EntityData> entityDatas = fetchEntities(ctx, latestDataSource, stateEntityId);
        for (EntityData entity : entityDatas) {
            content.addAll(renderTableComponent(ctx, component, entity));
        }
        return content;
    }

    private ComponentData getComponentData(TbReportCtx ctx, ReportComponent component, EntityData stateEntity) {
        ComponentData componentData = switch (component.getType()) {
            case TIME_SERIES_TABLE -> buildTsComponentData(0, ctx, (TimeseriesTableComponent) component, stateEntity);
            case ALARM_TABLE -> buildAlarmComponentData(0, ctx, (AlarmTableComponent) component, stateEntity);
            case ENTITY_TABLE -> buildEntityComponentData(ctx, (EntityTableComponent) component, stateEntity);
            default -> throw new IllegalArgumentException("Unsupported component type: " + component.getType());
        };
        populateReportVars(componentData, ctx);
        return componentData;
    }

    private ComponentData buildEntityComponentData(TbReportCtx ctx, EntityTableComponent component, EntityData stateEntity) {
        Optional<DataSource> singleDataSource = getSingleDataSource(component);
        if (singleDataSource.isEmpty()) {
            return new ComponentData(0);
        }
        List<Map<String, String>> entityDatas = collectEntityDatas(ctx, singleDataSource.get(), stateEntity != null ? stateEntity.getEntityId() : null);
        Map<String, Object> variables = new HashMap<>(toStringMap(stateEntity, singleDataSource.get().getDataKeys(), ctx, null));
        return new ComponentData(0, null, entityDatas, variables);
    }

    private List<List<String>> renderError(String errorDescription) {
        return renderError(errorDescription, null);
    }

    private List<List<String>> renderError(String errorDescription, Exception e) {
        if (e instanceof InterruptedException || ExceptionUtils.getRootCause(e) instanceof InterruptedException) {
            throw new RuntimeException(e);
        }
        if (e != null) {
            errorDescription = errorDescription + " Error: " + e.getMessage();
        }
        return List.of(List.of(errorDescription));
    }

    @Override
    public TbReportFormat getFormat() {
        return TbReportFormat.CSV;
    }

}
