// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.integration;

import com.fasterxml.jackson.databind.node.ArrayNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.integration.IntegrationInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.integration.IntegrationInfoDao;
import org.thingsboard.server.dao.model.sql.IntegrationInfoEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.event.StatisticsEventRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Created by Valerii Sosliuk on 5/6/2017.
 */
@Slf4j
@Component
@SqlDao
public class JpaIntegrationInfoDao extends JpaAbstractDao<IntegrationInfoEntity, IntegrationInfo> implements IntegrationInfoDao {

    @Autowired
    private IntegrationInfoRepository integrationInfoRepository;
    @Autowired
    private StatisticsEventRepository statisticsEventRepository;

    @Override
    protected Class<IntegrationInfoEntity> getEntityClass() {
        return IntegrationInfoEntity.class;
    }

    @Override
    protected JpaRepository<IntegrationInfoEntity, UUID> getRepository() {
        return integrationInfoRepository;
    }

    @Override
    public PageData<IntegrationInfo> findByTenantIdAndIsEdgeTemplate(UUID tenantId, PageLink pageLink, boolean isEdgeTemplate) {
        return DaoUtil.toPageData(
                integrationInfoRepository.findByTenantIdAndIsEdgeTemplate(
                        tenantId,
                        pageLink.getTextSearch(),
                        isEdgeTemplate,
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<IntegrationInfo> findIntegrationsByTenantIdAndEdgeId(UUID tenantId, UUID edgeId, PageLink pageLink) {
        log.debug("Try to find integrations by tenantId [{}], edgeId [{}] and pageLink [{}]", tenantId, edgeId, pageLink);
        return DaoUtil.toPageData(integrationInfoRepository
                .findByTenantIdAndEdgeId(
                        tenantId,
                        edgeId,
                        pageLink.getTextSearch(),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<IntegrationInfo> findAllIntegrationInfosWithStats(UUID tenantId, boolean isEdgeTemplate, PageLink pageLink) {
        log.debug("Try to find integrations with stats by tenantId [{}] and pageLink [{}]", tenantId, pageLink);
        PageData<IntegrationInfo> integrationInfos = DaoUtil.toPageData(integrationInfoRepository
                .findAllIntegrationInfos(
                        tenantId,
                        pageLink.getTextSearch(),
                        isEdgeTemplate,
                        DaoUtil.toPageable(pageLink)));
        List<UUID> integrationIds = integrationInfos.getData().stream()
                .map(info -> info.getId().getId())
                .toList();

        Map<UUID, String> statsMap = statisticsEventRepository.findAggregatedDailyStats(tenantId, integrationIds)
                .stream()
                .collect(Collectors.toMap(StatisticsEventRepository.Stats::getEntityId, StatisticsEventRepository.Stats::getStats));

        // add stats to integration info
        integrationInfos.getData().forEach(info -> {
            String stats = statsMap.get(info.getId().getId());
            info.setStats(StringUtils.isEmpty(stats) ? JacksonUtil.newArrayNode() : JacksonUtil.fromString(stats, ArrayNode.class));
        });

        return integrationInfos;
    }

}
