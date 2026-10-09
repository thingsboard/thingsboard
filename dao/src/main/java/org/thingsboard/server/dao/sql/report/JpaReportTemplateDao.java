// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.report;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.edqs.fields.ReportTemplateFields;
import org.thingsboard.server.common.data.id.ReportTemplateId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.report.ReportTemplate;
import org.thingsboard.server.common.data.util.TbTriple;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.model.sql.ReportTemplateEntity;
import org.thingsboard.server.dao.report.ReportTemplateDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@AllArgsConstructor
@SqlDao
public class JpaReportTemplateDao extends JpaAbstractDao<ReportTemplateEntity, ReportTemplate> implements ReportTemplateDao {

    private final ReportTemplateRepository reportTemplateRepository;

    @Override
    public PageData<ReportTemplateId> findIdsByTenantIdAndCustomerId(UUID tenantId, UUID customerId, PageLink pageLink) {
        Page<UUID> page;
        if (customerId == null) {
            page = reportTemplateRepository.findIdsByTenantIdAndNullCustomerId(tenantId, DaoUtil.toPageable(pageLink));
        } else {
            page = reportTemplateRepository.findIdsByTenantIdAndCustomerId(tenantId, customerId, DaoUtil.toPageable(pageLink));
        }
        return DaoUtil.pageToPageData(page, ReportTemplateId::new);
    }

    @Override
    public ReportTemplate findByTenantIdAndExternalId(UUID tenantId, UUID externalId) {
        return DaoUtil.getData(reportTemplateRepository.findByTenantIdAndExternalId(tenantId, externalId));
    }

    @Override
    public PageData<ReportTemplate> findByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(reportTemplateRepository.findByTenantId(tenantId, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<ReportTemplateId> findIdsByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(reportTemplateRepository.findIdsByTenantId(tenantId, DaoUtil.toPageable(pageLink))
                .map(ReportTemplateId::new));
    }

    @Override
    public ReportTemplateId getExternalIdByInternal(ReportTemplateId internalId) {
        return Optional.ofNullable(reportTemplateRepository.getExternalIdById(internalId.getId()))
                .map(ReportTemplateId::new).orElse(null);
    }

    @Override
    public List<ReportTemplateFields> findNextBatch(UUID id, int batchSize) {
        return reportTemplateRepository.findNextBatch(id, Limit.of(batchSize));
    }

    @Override
    protected Class<ReportTemplateEntity> getEntityClass() {
        return ReportTemplateEntity.class;
    }

    @Override
    protected JpaRepository<ReportTemplateEntity, UUID> getRepository() {
        return reportTemplateRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.REPORT_TEMPLATE;
    }

    @Override
    public Map<String, Map<String, Long>> countTemplateByFormatAndType() {
        return reportTemplateRepository.countTemplatesByFormatAndType()
                .stream()
                .collect(Collectors.groupingBy(e -> e.getFirst().name(), Collectors.toMap(e -> e.getSecond().name(), TbTriple::getThird)));
    }

}
