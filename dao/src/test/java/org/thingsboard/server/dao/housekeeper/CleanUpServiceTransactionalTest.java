// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.housekeeper;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTask;
import org.thingsboard.server.common.data.housekeeper.HousekeeperTaskType;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.msg.housekeeper.HousekeeperClient;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Integration test for the transactional half of the {@link CleanUpService} enqueue contract that
 * {@link CleanUpServiceTest} cannot cover: {@code handleEntityDeletionEvent} is a
 * {@code @TransactionalEventListener(fallbackExecution = true)}, so when the entity-deletion event is published
 * inside a transaction, the Housekeeper task must be submitted only after that transaction commits, and must not
 * be submitted at all if it rolls back.
 */
@DaoSqlTest
public class CleanUpServiceTransactionalTest extends AbstractServiceTest {

    @MockitoBean
    private HousekeeperClient housekeeperClient;

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Before
    public void resetHousekeeperClient() {
        Mockito.clearInvocations(housekeeperClient);
    }

    @Test
    public void whenDeletionEventTransactionRollsBack_thenNoHousekeeperTaskIsSubmitted() {
        DeleteEntityEvent<Alarm> event = alarmDeletionEvent();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
            status.setRollbackOnly();
        });

        verify(housekeeperClient, never()).submitTask(any());
    }

    @Test
    public void whenDeletionEventTransactionCommits_thenHousekeeperTaskIsSubmittedAfterCommit() {
        DeleteEntityEvent<Alarm> event = alarmDeletionEvent();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publishEvent(event);
            // the listener is deferred to the after-commit phase, so nothing may be submitted mid-transaction
            verify(housekeeperClient, never()).submitTask(any());
        });

        ArgumentCaptor<HousekeeperTask> captor = ArgumentCaptor.forClass(HousekeeperTask.class);
        verify(housekeeperClient).submitTask(captor.capture());
        assertThat(captor.getValue().getTaskType()).isEqualTo(HousekeeperTaskType.DELETE_ALARM_COMMENTS);
        assertThat(captor.getValue().getEntityId()).isEqualTo(event.getEntityId());
    }

    private DeleteEntityEvent<Alarm> alarmDeletionEvent() {
        return DeleteEntityEvent.<Alarm>builder()
                .tenantId(tenantId)
                .entityId(new AlarmId(UUID.randomUUID()))
                .build();
    }

}
