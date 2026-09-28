// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.report;

import com.google.common.util.concurrent.FluentFuture;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportInfo;
import org.thingsboard.server.common.data.report.ReportInfoQuery;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.service.ConstraintValidator;
import org.thingsboard.server.dao.service.validator.ReportDataValidator;

import java.util.List;
import java.util.Optional;

import static com.google.common.util.concurrent.MoreExecutors.directExecutor;
import static org.apache.commons.lang3.RandomStringUtils.secure;
import static org.thingsboard.server.dao.DaoUtil.toUUIDs;
import static org.thingsboard.server.dao.service.Validator.validateId;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultReportService extends AbstractEntityService implements ReportService {

    public static final String INCORRECT_TENANT_ID = "Incorrect tenantId ";
    public static final String INCORRECT_REPORT_ID = "Incorrect reportId ";
    private static final int PUBLIC_KEY_LENGTH = 32;

    private final ReportDao reportDao;
    private final ReportDataValidator reportDataValidator;

    @Transactional
    @Override
    public Report createReport(Report report, byte[] data) {
        if (report.getId() != null) {
            throw new IllegalArgumentException("Report can't be updated");
        }
        reportDataValidator.validateReportSize(report.getTenantId(), data);
        Report saved = saveReport(report);
        reportDao.saveData(saved.getTenantId(), saved.getId(), data);
        return saved;
    }

    @Override
    public Report findReportById(TenantId tenantId, ReportId reportId) {
        return reportDao.findById(tenantId, reportId.getId());
    }

    @Override
    public ReportData getReportDataById(TenantId tenantId, ReportId reportId) {
        return reportDao.getReportDataById(tenantId, reportId);
    }

    @Override
    public ReportData getReportDataByPublicKey(String publicKey) {
        return reportDao.getReportDataByPublicKey(publicKey);
    }

    @Override
    @Transactional
    public Report updateReport(Report report) {
        return saveReport(report);
    }

    private Report saveReport(Report report) {
        boolean created = report.getId() == null;
        ConstraintValidator.validateFields(report);
        if (report.isPublic() && report.getPublicKey() == null) {
            report.setPublicKey(generatePublicKey());
        } else if (!report.isPublic() && report.getPublicKey() != null) {
            report.setPublicKey(null);
        }
        Report saved = reportDao.save(report.getTenantId(), report);
        eventPublisher.publishEvent(SaveEntityEvent.builder().tenantId(saved.getTenantId()).entityId(saved.getId())
                .entity(saved).created(created).build());
        return saved;
    }

    @Override
    public void deleteReport(TenantId tenantId, ReportId reportId) {
        validateId(tenantId, id -> INCORRECT_TENANT_ID + id);
        validateId(reportId, id -> INCORRECT_REPORT_ID + id);
        deleteEntity(tenantId, reportId, false);
    }

    @Override
    public PageData<Report> findReports(TenantId tenantId, CustomerId customerId, boolean includeCustomers, PageLink pageLink) {
        log.trace("Executing findReports, tenantId [{}], customerId [{}], includeCustomers [{}]", tenantId, customerId, includeCustomers);
        return reportDao.findReports(tenantId, customerId, includeCustomers, pageLink);
    }

    @Override
    public PageData<ReportInfo> findReportInfos(TenantId tenantId, CustomerId customerId, ReportInfoQuery query) {
        log.trace("Executing findReportInfos, tenantId [{}], customerId [{}]", tenantId, customerId);
        return reportDao.findReportInfos(tenantId, customerId, query);
    }

    @Override
    public void deleteReportsByTenantId(TenantId tenantId) {
        log.trace("Executing deleteReportsByTenantId, tenantId [{}]", tenantId);
        reportDao.deleteByTenantId(tenantId);
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        deleteReportsByTenantId(tenantId);
    }

    @Override
    public void deleteReportsByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        log.trace("Executing deleteReportsByTenantIdAndCustomerId, tenantId [{}], customerId [{}]", tenantId, customerId);
        reportDao.deleteByTenantIdAndCustomerId(tenantId, customerId);
    }

    @Override
    public List<ReportInfo> findReportInfoByIds(TenantId tenantId, List<ReportId> reportIds) {
        log.trace("Executing findReportInfoByIds, reportIds [{}]", reportIds);
        return reportDao.findReportByIds(tenantId, toUUIDs(reportIds));
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(reportDao.findById(tenantId, entityId.getId()));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(reportDao.findByIdAsync(tenantId, entityId.getId()))
                .transform(Optional::ofNullable, directExecutor());
    }

    @Override
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        reportDao.removeById(tenantId, id.getId());
        eventPublisher.publishEvent(DeleteEntityEvent.builder().tenantId(tenantId).entityId(id).build());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.REPORT;
    }

    private static String generatePublicKey() {
        return secure().nextAlphanumeric(PUBLIC_KEY_LENGTH);
    }

}
