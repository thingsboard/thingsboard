// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.report;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.report.ReportTemplateInfo;
import org.thingsboard.server.common.data.report.ReportTemplateQuery;
import org.thingsboard.server.common.data.report.ReportTemplateType;
import org.thingsboard.server.common.data.report.TbReportFormat;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.ReportTemplateInfoEntity;
import org.thingsboard.server.dao.report.ReportTemplateInfoDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@AllArgsConstructor
@Slf4j
@SqlDao
public class JpaReportTemplateInfoDao extends JpaAbstractDao<ReportTemplateInfoEntity, ReportTemplateInfo> implements ReportTemplateInfoDao {

    private final ReportTemplateInfoRepository reportTemplateInfoRepository;

    @Override
    public PageData<ReportTemplateInfo> findReportTemplates(UUID tenantId, ReportTemplateQuery query) {
        List<ReportTemplateType> typeList = query.getTypeList() != null && !query.getTypeList().isEmpty() ? query.getTypeList() : null;
        List<TbReportFormat> formatList = query.getFormatList() != null && !query.getFormatList().isEmpty() ? query.getFormatList() : null;
        if (query.isIncludeCustomers()) {
            return DaoUtil.toPageData(reportTemplateInfoRepository
                    .findTenantReportTemplatesIncludingCustomers(
                            tenantId,
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            typeList,
                            formatList,
                            DaoUtil.toPageable(query.getPageLink())));
        } else {
            return DaoUtil.toPageData(reportTemplateInfoRepository
                    .findTenantReportTemplates(
                            tenantId,
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            typeList,
                            formatList,
                            DaoUtil.toPageable(query.getPageLink())));
        }
    }

    @Override
    public PageData<ReportTemplateInfo> findCustomerReportTemplates(UUID tenantId, UUID customerId, ReportTemplateQuery query) {
        List<ReportTemplateType> typeList = query.getTypeList() != null && !query.getTypeList().isEmpty() ? query.getTypeList() : null;
        List<TbReportFormat> formatList = query.getFormatList() != null && !query.getFormatList().isEmpty() ? query.getFormatList() : null;
        if (query.isIncludeCustomers()) {
            return DaoUtil.toPageData(reportTemplateInfoRepository
                    .findCustomerReportTemplatesIncludingSubCustomers(
                            tenantId,
                            customerId,
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            typeList != null ? typeList.stream().map(Enum::name).toList() : null,
                            formatList != null ? formatList.stream().map(Enum::name).toList() : null,
                            DaoUtil.toPageable(query.getPageLink())));
        } else {
            return DaoUtil.toPageData(reportTemplateInfoRepository
                    .findCustomerReportTemplates(
                            tenantId,
                            customerId,
                            Objects.toString(query.getPageLink().getTextSearch(), ""),
                            typeList,
                            formatList,
                            DaoUtil.toPageable(query.getPageLink())));
        }
    }

    @Override
    public List<ReportTemplateInfo> findReportTemplatesByIds(UUID tenantId, List<UUID> reportTemplateIds) {
        return DaoUtil.convertDataList(reportTemplateInfoRepository.findByIdIn(reportTemplateIds));
    }

    @Override
    protected Class<ReportTemplateInfoEntity> getEntityClass() {
        return ReportTemplateInfoEntity.class;
    }

    @Override
    protected JpaRepository<ReportTemplateInfoEntity, UUID> getRepository() {
        return reportTemplateInfoRepository;
    }
}
