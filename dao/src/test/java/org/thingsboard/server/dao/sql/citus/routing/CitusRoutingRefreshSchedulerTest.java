// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CitusRoutingRefreshSchedulerTest {

    // The refresh body lives in CitusRoutingRefresher (shared with the failover trigger); the scheduler
    // is its fixed-delay entry point. These tests drive the scheduler over a REAL refresher so the
    // pre-extraction scheduled-tick behavior (ordering + per-step failure isolation) stays pinned
    // end-to-end through the scheduler.

    private static CitusRoutingRefreshScheduler scheduler(CitusShardPlacement placement, CitusWorkerRegistry workerRegistry) {
        return new CitusRoutingRefreshScheduler(new CitusRoutingRefresher(placement, workerRegistry));
    }

    @Test
    void refreshesWorkerRegistryBeforePlacement() {
        CitusShardPlacement placement = mock(CitusShardPlacement.class);
        CitusWorkerRegistry workerRegistry = mock(CitusWorkerRegistry.class);
        CitusRoutingRefreshScheduler scheduler = scheduler(placement, workerRegistry);

        scheduler.refresh();

        InOrder inOrder = inOrder(workerRegistry, placement);
        inOrder.verify(workerRegistry).refresh();
        inOrder.verify(placement).refresh();
    }

    @Test
    void placementIsStillRefreshedWhenWorkerRegistryRefreshThrows() {
        CitusShardPlacement placement = mock(CitusShardPlacement.class);
        CitusWorkerRegistry workerRegistry = mock(CitusWorkerRegistry.class);
        doThrow(new RuntimeException("registry down")).when(workerRegistry).refresh();
        CitusRoutingRefreshScheduler scheduler = scheduler(placement, workerRegistry);

        assertThatCode(scheduler::refresh).doesNotThrowAnyException();

        verify(placement).refresh();
    }

    @Test
    void throwingPlacementRefreshIsIsolatedAndTheRegistryKeepsRefreshingOnTheNextTick() {
        CitusShardPlacement placement = mock(CitusShardPlacement.class);
        CitusWorkerRegistry workerRegistry = mock(CitusWorkerRegistry.class);
        doThrow(new RuntimeException("placement down")).when(placement).refresh();
        CitusRoutingRefreshScheduler scheduler = scheduler(placement, workerRegistry);

        assertThatCode(scheduler::refresh).doesNotThrowAnyException();
        assertThatCode(scheduler::refresh).doesNotThrowAnyException();

        // The registry refresh runs first and is never affected by the placement failure, so it fires once per tick.
        verify(workerRegistry, times(2)).refresh();
    }
}
