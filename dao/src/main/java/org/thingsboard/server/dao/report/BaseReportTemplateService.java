// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.report;

import com.google.common.util.concurrent.FluentFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.BaseReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.report.ReportTemplateQuery;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.scheduler.SchedulerEventService;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.service.PaginatedRemover;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;
import static org.thingsboard.server.dao.service.Validator.validateIds;
import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Slf4j
@RequiredArgsConstructor
@Service("ReportTemplateDaoService")
public class BaseReportTemplateService extends AbstractEntityService implements ReportTemplateService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_CUSTOMER_ID = "Incorrect customerId ";
    public static final String INCORRECT_REPORT_TEMPLATE_ID = "Incorrect reportTemplateId ";

    private final ReportTemplateDao reportTemplateDao;
    private final ReportTemplateInfoDao reportTemplateInfoDao;
    private final DataValidator<ReportTemplate> reportTemplateDataValidator;
    private final EntityCountService countService;
    private final SchedulerEventService schedulerEventService;

    @Override
    public ReportTemplate findReportTemplateById(TenantId tenantId, ReportTemplateId reportTemplateId) {
        log.trace("Executing findReportTemplateById [{}]", reportTemplateId);
        validateId(reportTemplateId, id -> INCORRECT_REPORT_TEMPLATE_ID + id);
        return reportTemplateDao.findById(tenantId, reportTemplateId.getId());
    }

    @Override
    public ReportTemplateInfo findReportTemplateInfoById(TenantId tenantId, ReportTemplateId reportTemplateId) {
        log.trace("Executing findReportTemplateInfoById [{}]", reportTemplateId);
        validateId(reportTemplateId, id -> INCORRECT_REPORT_TEMPLATE_ID + id);
        return reportTemplateInfoDao.findById(tenantId, reportTemplateId.getId());
    }

    @Override
    public ReportTemplate saveReportTemplate(ReportTemplate reportTemplate) {
        return saveReportTemplate(reportTemplate, true);
    }

    @Override
    public ReportTemplate saveReportTemplate(ReportTemplate reportTemplate, boolean doValidate) {
        ReportTemplate oldReportTemplate = null;
        if (doValidate) {
            oldReportTemplate = reportTemplateDataValidator.validate(reportTemplate, BaseReportTemplate::getTenantId);
        } else if (reportTemplate.getId() != null) {
            oldReportTemplate = findReportTemplateById(reportTemplate.getTenantId(), reportTemplate.getId());
        }
        try {
            TenantId tenantId = reportTemplate.getTenantId();
            log.trace("Executing saveReportTemplate [{}]", reportTemplate);
            ReportTemplate savedReportTemplate = reportTemplateDao.save(tenantId, reportTemplate);
            eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(savedReportTemplate.getTenantId()).entityId(savedReportTemplate.getId())
                    .entity(savedReportTemplate).oldEntity(oldReportTemplate).created(reportTemplate.getId() == null).build());
            if (reportTemplate.getId() == null) {
                countService.publishCountEntityEvictEvent(savedReportTemplate.getTenantId(), EntityType.REPORT_TEMPLATE);
            }
            return savedReportTemplate;
        } catch (Exception e) {
            checkConstraintViolation(e,
                    "report_template_external_id_unq_key", "Report template with such external id already exists!");
            throw e;
        }
    }

    @Override
    public void deleteReportTemplate(TenantId tenantId, ReportTemplateId reportTemplateId) {
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(reportTemplateId, id -> INCORRECT_REPORT_TEMPLATE_ID + id);
        int eventsByTemplateId = schedulerEventService.countScheduledReportEventsByTemplateId(tenantId, reportTemplateId);
        if (eventsByTemplateId > 0) {
            throw new DataValidationException("Cannot delete report template with id [" + reportTemplateId + "], because it is used in " +
                    eventsByTemplateId + " scheduled reports. Please delete the scheduled reports first.");
        }
        deleteEntity(tenantId, reportTemplateId, false);
    }

    @Override
    public List<ReportTemplateInfo> findReportTemplateInfoByIds(TenantId tenantId, List<ReportTemplateId> reportTemplateIds) {
        log.trace("Executing findReportTemplateInfoByIds, reportTemplateIds [{}]", reportTemplateIds);
        validateIds(reportTemplateIds, ids -> "Incorrect reportTemplateIds " + ids);
        return reportTemplateInfoDao.findReportTemplatesByIds(tenantId.getId(), toUUIDs(reportTemplateIds));
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        ReportTemplate reportTemplate = reportTemplateDao.findById(tenantId, id.getId());
        if (reportTemplate == null) {
            if (force) {
                return;
            } else {
                throw new IncorrectParameterException("Unable to delete non-existent report template.");
            }
        }
        deleteReportTemplate(tenantId, reportTemplate);
    }

    private void deleteReportTemplate(TenantId tenantId, BaseReportTemplate reportTemplate) {
        log.trace("Executing deleteReportTemplate, tenantId [{}], reportTemplateId [{}]", tenantId, reportTemplate.getId());
        reportTemplateDao.removeById(tenantId, reportTemplate.getUuidId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(reportTemplate.getId()).entity(reportTemplate).build());
    }

    @Override
    public PageData<ReportTemplateInfo> findReportTemplates(TenantId tenantId, ReportTemplateQuery query) {
        log.trace("Executing findReportTemplates, tenantId [{}], query [{}]", tenantId, query);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validatePageLink(query.getPageLink());
        return reportTemplateInfoDao.findReportTemplates(tenantId.getId(), query);
    }

    @Override
    public PageData<ReportTemplateInfo> findCustomerReportTemplates(TenantId tenantId, CustomerId customerId, ReportTemplateQuery query) {
        log.trace("Executing findCustomerReportTemplates, tenantId [{}], customerId [{}], query [{}]", tenantId, customerId, query);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        validatePageLink(query.getPageLink());
        return reportTemplateInfoDao.findCustomerReportTemplates(tenantId.getId(), customerId.getId(), query);
    }

    @Override
    public void deleteReportTemplatesByTenantId(TenantId tenantId) {
        log.trace("Executing deleteReportTemplatesByTenantId, tenantId [{}]", tenantId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        tenantReportTemplatesRemover.removeEntities(tenantId, tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteReportTemplatesByTenantId(tenantId);
    }

    @Override
    public void deleteReportTemplatesByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteReportTemplatesByTenantIdAndCustomerId, tenantId [{}], customerId [{}]", tenantId, customerId);
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(customerId, id -> INCORRECT_CUSTOMER_ID + id);
        customerReportTemplatesRemover.removeEntities(tenantId, customerId);
    }

    private final PaginatedRemover<TenantId, ReportTemplateInfo> tenantReportTemplatesRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<ReportTemplateInfo> findEntities(TenantId tenantId, TenantId id, PageLink pageLink) {
            return reportTemplateInfoDao.findReportTemplates(id.getId(),
                    ReportTemplateQuery.builder()
                            .includeCustomers(true)
                            .pageLink(pageLink)
                            .build());
        }

        @Override
        protected void removeEntity(TenantId tenantId, ReportTemplateInfo reportInfo) {
            deleteReportTemplate(tenantId, reportInfo);
        }
    };

    private final PaginatedRemover<CustomerId, ReportTemplateInfo> customerReportTemplatesRemover = new PaginatedRemover<>() {

        @Override
        protected PageData<ReportTemplateInfo> findEntities(TenantId tenantId, CustomerId id, PageLink pageLink) {
            return reportTemplateInfoDao.findCustomerReportTemplates(tenantId.getId(), id.getId(),
                    ReportTemplateQuery.builder()
                            .includeCustomers(false)
                            .pageLink(pageLink)
                            .build());
        }

        @Override
        protected void removeEntity(TenantId tenantId, ReportTemplateInfo reportInfo) {
            deleteReportTemplate(tenantId, reportInfo);
        }
    };

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findReportTemplateById(tenantId, new ReportTemplateId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(reportTemplateDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return reportTemplateDao.countByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.REPORT_TEMPLATE;
    }

}
