// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.report;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.ReportFields;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.ReportId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.Report;
import org.thingsboard.server.common.data.report.ReportData;
import org.thingsboard.server.common.data.report.ReportInfo;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.common.data.report.ReportInfoQuery;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.ReportDataEntity;
import org.thingsboard.server.dao.model.sql.ReportEntity;
import org.thingsboard.server.dao.report.ReportDao;
import org.thingsboard.server.dao.sql.JpaPartitionedAbstractDao;
import org.thingsboard.server.dao.sqlts.insert.sql.SqlPartitioningRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@SqlDao
public class JpaReportDao extends JpaPartitionedAbstractDao<ReportEntity, Report> implements ReportDao {

    private final ReportRepository reportRepository;
    private final ReportInfoRepository reportInfoRepository;
    private final SqlPartitioningRepository partitioningRepository;

    @Value("${sql.reports.partition_size:168}")
    private int partitionSizeInHours;

    private static final String TABLE_NAME = ModelConstants.REPORT_TABLE_NAME;

    public JpaReportDao(ReportRepository reportRepository, ReportInfoRepository reportInfoRepository, SqlPartitioningRepository partitioningRepository) {
        this.reportRepository = reportRepository;
        this.reportInfoRepository = reportInfoRepository;
        this.partitioningRepository = partitioningRepository;
    }

    @Override
    public void saveData(TenantId tenantId, ReportId reportId, byte[] data) {
        reportRepository.saveData(reportId.getId(), data);
    }

    @Override
    public ReportData getReportDataById(TenantId tenantId, ReportId reportId) {
        return toReportData(reportRepository.getReportDataById(reportId.getId()));
    }

    @Override
    public PageData<Report> findReports(TenantId tenantId, CustomerId customerId, boolean includeCustomers, PageLink pageLink) {
        if (customerId == null || customerId.isNullUid()) {
            if (includeCustomers) {
                return DaoUtil.toPageData(reportRepository.findByTenantIdAndSearchText(tenantId.getId(),
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
            } else {
                return DaoUtil.toPageData(reportRepository.findTenantReports(tenantId.getId(),
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
            }
        } else {
            if (includeCustomers) {
                return DaoUtil.toPageData(reportRepository.findCustomerReportsIncludingSubCustomers(tenantId.getId(),
                        customerId.getId(),
                        Objects.toString(pageLink.getTextSearch(), null),
                        DaoUtil.toPageable(pageLink)));
            } else {
                return DaoUtil.toPageData(reportRepository.findByTenantIdAndCustomerIdAndSearchText(tenantId.getId(),
                        customerId.getId(),
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
            }
        }
    }

    @Override
    public PageData<ReportInfo> findReportInfos(TenantId tenantId, CustomerId customerId, ReportInfoQuery query) {
        if (customerId == null || customerId.isNullUid()) {
            if (query.isIncludeCustomers()) {
                return DaoUtil.toPageData(reportInfoRepository
                        .findTenantReportInfosIncludingCustomers(
                                tenantId.getId(),
                                query.getReportTemplateId(),
                                query.getUserId(),
                                Objects.toString(query.getPageLink().getTextSearch(), ""),
                                DaoUtil.toPageable(query.getPageLink())));
            } else {
                return DaoUtil.toPageData(reportInfoRepository
                        .findTenantReportInfos(
                                tenantId.getId(),
                                query.getReportTemplateId(),
                                query.getUserId(),
                                Objects.toString(query.getPageLink().getTextSearch(), ""),
                                DaoUtil.toPageable(query.getPageLink())));
            }
        } else {
            if (query.isIncludeCustomers()) {
                return DaoUtil.toPageData(reportInfoRepository
                        .findCustomerReportInfosIncludingSubCustomers(
                                tenantId.getId(),
                                customerId.getId(),
                                query.getReportTemplateId(),
                                query.getUserId(),
                                Objects.toString(query.getPageLink().getTextSearch(), ""),
                                DaoUtil.toPageable(query.getPageLink())));
            } else {
                return DaoUtil.toPageData(reportInfoRepository
                        .findCustomerReportInfos(
                                tenantId.getId(),
                                customerId.getId(),
                                query.getReportTemplateId(),
                                query.getUserId(),
                                Objects.toString(query.getPageLink().getTextSearch(), ""),
                                DaoUtil.toPageable(query.getPageLink())));
            }
        }
    }

    @Override
    public ReportData getReportDataByPublicKey(String publicKey) {
        return toReportData(reportRepository.getReportDataByPublicKey(publicKey));
    }

    private static ReportData toReportData(ReportDataEntity reportDataEntity) {
        if (reportDataEntity == null) {
            return null;
        }
        return ReportData.builder()
                .data(reportDataEntity.getData())
                .name(reportDataEntity.getName())
                .contentType(TbReportFormat.fromOrdinal(reportDataEntity.getFormat()).getContentType())
                .build();
    }

    @Override
    public List<ReportInfo> findReportByIds(TenantId tenantId, List<UUID> toUUIDs) {
        return DaoUtil.convertDataList(reportInfoRepository.findByIdIn(toUUIDs));
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        reportRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public void deleteByTenantIdAndCustomerId(TenantId tenantId, CustomerId customerId) {
        reportRepository.deleteByTenantIdAndCustomerId(tenantId.getId(), customerId.getId());
    }

    @Override
    public Map<String, Long> countReportsByType() {
        return reportRepository.countReportsByFormatType()
                .stream()
                .collect(Collectors.toMap(e->e.getFirst().name(), TbPair::getSecond));
    }

    @Override
    public void createPartition(ReportEntity entity) {
        partitioningRepository.createPartitionIfNotExists(TABLE_NAME, entity.getCreatedTime(), TimeUnit.HOURS.toMillis(partitionSizeInHours));
    }

    @Override
    public List<ReportFields> findNextBatch(UUID id, int batchSize) {
        return reportRepository.findNextBatch(id, Limit.of(batchSize));
    }

    @Override
    protected Class<ReportEntity> getEntityClass() {
        return ReportEntity.class;
    }

    @Override
    protected JpaRepository<ReportEntity, UUID> getRepository() {
        return reportRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.REPORT;
    }

}
