/**
 * ThingsBoard, Inc. ("COMPANY") CONFIDENTIAL
 *
 * Copyright © 2016-2026 ThingsBoard, Inc. All Rights Reserved.
 *
 * NOTICE: All information contained herein is, and remains
 * the property of ThingsBoard, Inc. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to ThingsBoard, Inc.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 *
 * Dissemination of this information or reproduction of this material is strictly forbidden
 * unless prior written permission is obtained from COMPANY.
 *
 * Access to the source code contained herein is hereby forbidden to anyone except current COMPANY employees,
 * managers or contractors who have executed Confidentiality and Non-disclosure agreements
 * explicitly covering such access.
 *
 * The copyright notice above does not evidence any actual or intended publication
 * or disclosure  of  this source code, which includes
 * information that is confidential and/or proprietary, and is a trade secret, of  COMPANY.
 * ANY REPRODUCTION, MODIFICATION, DISTRIBUTION, PUBLIC  PERFORMANCE,
 * OR PUBLIC DISPLAY OF OR THROUGH USE  OF THIS  SOURCE CODE  WITHOUT
 * THE EXPRESS WRITTEN CONSENT OF COMPANY IS STRICTLY PROHIBITED,
 * AND IN VIOLATION OF APPLICABLE LAWS AND INTERNATIONAL TREATIES.
 * THE RECEIPT OR POSSESSION OF THIS SOURCE CODE AND/OR RELATED INFORMATION
 * DOES NOT CONVEY OR IMPLY ANY RIGHTS TO REPRODUCE, DISCLOSE OR DISTRIBUTE ITS CONTENTS,
 * OR TO MANUFACTURE, USE, OR SELL ANYTHING THAT IT  MAY DESCRIBE, IN WHOLE OR IN PART.
 */
package org.thingsboard.server.dao.sql.trace;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.config.DedicatedEventsDataSource;
import org.thingsboard.server.dao.model.sql.trace.SpanEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Collection;
import java.util.List;

import static org.thingsboard.server.dao.config.DedicatedEventsJpaDaoConfig.EVENTS_PERSISTENCE_UNIT;
import static org.thingsboard.server.dao.config.DedicatedEventsJpaDaoConfig.EVENTS_TRANSACTION_MANAGER;

@DedicatedEventsDataSource
@Component
@SqlDao
public class DedicatedJpaTraceDao extends JpaTraceDao {
    public DedicatedJpaTraceDao(SpanRepository spanRepository) {
        super(spanRepository);
    }

    @PersistenceContext(unitName = EVENTS_PERSISTENCE_UNIT)
    public void setEntityManager(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(transactionManager = EVENTS_TRANSACTION_MANAGER)
    public void saveSpans(List<SpanEntity> spans) {
        super.saveSpans(spans);
    }

    @Override
    @Transactional(transactionManager = EVENTS_TRANSACTION_MANAGER)
    public int upsertTraceSummaries(Collection<TraceSummaryInfo> summaries) {
        return super.upsertTraceSummaries(summaries);
    }

    @Override
    @Transactional(transactionManager = EVENTS_TRANSACTION_MANAGER)
    public boolean tryAcquireAggregationLeader(String serviceId, long nowMs, long leaseMs) {
        return super.tryAcquireAggregationLeader(serviceId, nowMs, leaseMs);
    }
}
