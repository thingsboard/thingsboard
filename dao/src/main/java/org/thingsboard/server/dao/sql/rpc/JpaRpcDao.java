/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.rpc;

import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rpc.Rpc;
import org.thingsboard.server.common.data.rpc.RpcStatus;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.model.sql.RpcEntity;
import org.thingsboard.server.dao.rpc.RpcDao;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.ScheduledLogExecutorComponent;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueParams;
import org.thingsboard.server.dao.sql.TbSqlBlockingQueueWrapper;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Comparator;
import java.util.UUID;
import java.util.function.Function;

@Slf4j
@Component
@SqlDao
public class JpaRpcDao extends JpaAbstractDao<RpcEntity, Rpc> implements RpcDao, TenantEntityDao<Rpc> {

    @Autowired
    private RpcRepository rpcRepository;
    @Autowired
    private RpcInsertRepository rpcInsertRepository;
    @Autowired
    private ScheduledLogExecutorComponent logExecutor;
    @Autowired
    private StatsFactory statsFactory;

    @Value("${sql.rpc.batch_size:1000}")
    private int batchSize;
    @Value("${sql.rpc.batch_max_delay:50}")
    private long maxDelay;
    @Value("${sql.rpc.stats_print_interval_ms:10000}")
    private long statsPrintIntervalMs;
    @Value("${sql.rpc.batch_threads:3}")
    private int batchThreads;
    @Value("${sql.batch_sort:true}")
    private boolean batchSortEnabled;

    // Boolean response per queued write: an INSERT-on-conflict always persists (true), while an
    // UPDATE-by-id reports false when it matched no row (the RPC was deleted in the meantime), so the
    // service layer can skip the rule-engine notification for a row that no longer exists.
    private TbSqlBlockingQueueWrapper<RpcQueueEntry, Boolean> queue;

    @PostConstruct
    private void init() {
        TbSqlBlockingQueueParams params = TbSqlBlockingQueueParams.builder()
                .logName("RPC")
                .batchSize(batchSize)
                .maxDelay(maxDelay)
                .statsPrintIntervalMs(statsPrintIntervalMs)
                .statsNamePrefix("rpc")
                .batchSortEnabled(batchSortEnabled)
                .withResponse(true)
                .build();
        Function<RpcQueueEntry, Integer> hashcodeFunction = entry -> entry.entity().getUuid().hashCode();
        queue = new TbSqlBlockingQueueWrapper<>(params, hashcodeFunction, batchThreads, statsFactory);
        queue.init(logExecutor, entries -> rpcInsertRepository.saveOrUpdate(entries),
                Comparator.comparing((RpcQueueEntry entry) -> entry.entity().getUuid()),
                Function.identity());
    }

    @PreDestroy
    private void destroy() {
        if (queue != null) {
            queue.destroy();
        }
    }

    @Override
    public ListenableFuture<Boolean> createAsync(Rpc rpc) {
        return queue.add(RpcQueueEntry.forInsert(new RpcEntity(rpc)));
    }

    @Override
    public ListenableFuture<Boolean> updateAsync(Rpc rpc) {
        return queue.add(RpcQueueEntry.forUpdate(new RpcEntity(rpc)));
    }

    @Override
    protected Class<RpcEntity> getEntityClass() {
        return RpcEntity.class;
    }

    @Override
    protected JpaRepository<RpcEntity, UUID> getRepository() {
        return rpcRepository;
    }

    @Override
    public PageData<Rpc> findAllByDeviceId(TenantId tenantId, DeviceId deviceId, PageLink pageLink) {
        return DaoUtil.toPageData(rpcRepository.findAllByTenantIdAndDeviceId(tenantId.getId(), deviceId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Rpc> findAllByDeviceIdAndStatus(TenantId tenantId, DeviceId deviceId, RpcStatus rpcStatus, PageLink pageLink) {
        return DaoUtil.toPageData(rpcRepository.findAllByTenantIdAndDeviceIdAndStatus(tenantId.getId(), deviceId.getId(), rpcStatus, DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<Rpc> findAllRpcByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(rpcRepository.findAllByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Transactional
    @Override
    public int deleteOutdatedRpcByTenantIdBatch(TenantId tenantId, Long expirationTime, int batchSize) {
        return rpcRepository.deleteOutdatedRpcByTenantIdBatch(tenantId.getId(), expirationTime, batchSize);
    }

    @Override
    public PageData<Rpc> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return findAllRpcByTenantId(tenantId, pageLink);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.RPC;
    }

}
