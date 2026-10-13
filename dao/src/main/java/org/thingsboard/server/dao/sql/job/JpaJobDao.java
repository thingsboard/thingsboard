// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.sql.job;

import com.google.common.base.Strings;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.JobId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.job.Job;
import org.thingsboard.server.common.data.job.JobFilter;
import org.thingsboard.server.common.data.job.JobStatus;
import org.thingsboard.server.common.data.job.JobType;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.data.util.TbTriple;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.job.JobDao;
import org.thingsboard.server.dao.model.sql.JobEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@SqlDao
@RequiredArgsConstructor
public class JpaJobDao extends JpaAbstractDao<JobEntity, Job> implements JobDao {

    private final JobRepository jobRepository;

    @Override
    public PageData<Job> findByTenantIdAndFilter(TenantId tenantId, JobFilter filter, PageLink pageLink) {
        CustomerId customerId = filter.getCustomerId();
        boolean includeCustomers = filter.isIncludeCustomers();
        List<JobType> types = CollectionsUtil.isEmpty(filter.getTypes()) ? null : filter.getTypes();
        List<JobStatus> statuses = CollectionsUtil.isEmpty(filter.getStatuses()) ? null : filter.getStatuses();
        List<UUID> entities = CollectionsUtil.isEmpty(filter.getEntities()) ? null : filter.getEntities();
        long startTime = filter.getStartTime() != null ? filter.getStartTime() : 0;
        long endTime = filter.getEndTime() != null ? filter.getEndTime() : 0;
        String searchText = Strings.emptyToNull(pageLink.getTextSearch());
        Pageable pageable = DaoUtil.toPageable(pageLink);

        if (customerId == null || customerId.isNullUid()) {
            if (includeCustomers) {
                return DaoUtil.toPageData(jobRepository.findAllByTenantIdAndTypesAndStatusesAndEntitiesAndTimeAndSearchText(tenantId.getId(),
                        types, statuses, entities, startTime, endTime, searchText, pageable));
            } else {
                return DaoUtil.toPageData(jobRepository.findTenantJobsByTypesAndStatusesAndEntitiesAndTimeAndSearchText(tenantId.getId(),
                        types, statuses, entities, startTime, endTime, searchText, pageable));
            }
        } else {
            if (includeCustomers) {
                return DaoUtil.toPageData(jobRepository.findByTenantIdAndSubCustomersAndTypesAndStatusesAndEntitiesAndTimeAndSearchText(tenantId.getId(),
                        customerId.getId(), toNames(types), toNames(statuses), entities, startTime, endTime, searchText, pageable));
            } else {
                return DaoUtil.toPageData(jobRepository.findByTenantIdAndCustomerIdAndTypesAndStatusesAndEntitiesAndTimeAndSearchText(tenantId.getId(),
                        customerId.getId(), types, statuses, entities, startTime, endTime, searchText, pageable));
            }
        }
    }

    @Override
    public Job findByIdForUpdate(TenantId tenantId, JobId jobId) {
        return DaoUtil.getData(jobRepository.findByIdForUpdate(jobId.getId()));
    }

    @Override
    public Job findLatestByTenantIdAndKey(TenantId tenantId, String key) {
        return DaoUtil.getData(jobRepository.findLatestByTenantIdAndKey(tenantId.getId(), key, Limit.of(1)));
    }

    @Override
    public boolean existsByTenantAndKeyAndStatusOneOf(TenantId tenantId, String key, JobStatus... statuses) {
        return jobRepository.existsByTenantIdAndKeyAndStatusIn(tenantId.getId(), key, Arrays.stream(statuses).toList());
    }

    @Override
    public boolean existsByTenantIdAndTypeAndStatusOneOf(TenantId tenantId, JobType type, JobStatus... statuses) {
        return jobRepository.existsByTenantIdAndTypeAndStatusIn(tenantId.getId(), type, Arrays.stream(statuses).toList());
    }

    @Override
    public boolean existsByTenantIdAndEntityIdAndStatusOneOf(TenantId tenantId, EntityId entityId, JobStatus... statuses) {
        return jobRepository.existsByTenantIdAndEntityIdAndStatusIn(tenantId.getId(), entityId.getId(), Arrays.stream(statuses).toList());
    }

    @Override
    public Job findOldestByTenantIdAndTypeAndStatusForUpdate(TenantId tenantId, JobType type, JobStatus status) {
        return DaoUtil.getData(jobRepository.findOldestByTenantIdAndTypeAndStatusForUpdate(tenantId.getId(), type.name(), status.name()));
    }

    @Override
    public Map<String, Map<String, Long>> countJobsByTypeAndStatusLastMonth() {
        long sinceMillis = LocalDate
                .now(ZoneOffset.UTC)
                .minusMonths(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli();

        return jobRepository.findCountsGroupedByTypeAndStatusSince(sinceMillis)
                        .stream()
                        .collect(Collectors.groupingBy(e -> e.getFirst().name(), Collectors.toMap(e -> e.getSecond().name(), TbTriple::getThird)));
    }

    @Override
    public void removeByTenantId(TenantId tenantId) {
        jobRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public int removeByEntityId(TenantId tenantId, EntityId entityId) {
        return jobRepository.deleteByEntityId(entityId.getId());
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.JOB;
    }

    @Override
    protected Class<JobEntity> getEntityClass() {
        return JobEntity.class;
    }

    @Override
    protected JpaRepository<JobEntity, UUID> getRepository() {
        return jobRepository;
    }

    private static List<String> toNames(List<? extends Enum<?>> values) {
        return values == null ? null : values.stream().map(Enum::name).toList();
    }

}
