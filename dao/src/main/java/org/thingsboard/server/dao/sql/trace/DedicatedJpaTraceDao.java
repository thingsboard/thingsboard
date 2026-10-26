// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.trace;

import org.springframework.stereotype.Component;
import org.thingsboard.server.dao.config.DedicatedEventsDataSource;
import org.thingsboard.server.dao.util.SqlDao;

@DedicatedEventsDataSource
@Component
@SqlDao
public class DedicatedJpaTraceDao extends JpaTraceDao {

    public DedicatedJpaTraceDao(SpanRepository spanRepository,
                                DedicatedTraceInsertRepository traceInsertRepository,
                                DedicatedTraceQueryRepository traceQueryRepository) {
        super(spanRepository, traceInsertRepository, traceQueryRepository);
    }

}
