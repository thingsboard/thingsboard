// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { NgZone } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { distinctUntilChanged, map } from 'rxjs/operators';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { LatestTelemetry, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { EntityId } from '@shared/models/id/entity-id';
import {
  AGENT_NORMALIZATION_KEYS,
  ENTITY_METRIC_KEYS,
  EMPTY_METRICS_SNAPSHOT,
  LatestValues,
  METRIC_KEY_CPU_PERCENT,
  METRIC_KEY_DISK_BYTES,
  METRIC_KEY_HOST_DISK_TOTAL,
  METRIC_KEY_HOST_MEMORY_BYTES,
  METRIC_KEY_MEMORY_BYTES,
  METRIC_KEY_ONLINE_CPUS,
  METRIC_KEY_SIZE_BYTES,
  METRIC_KEY_VOLUME_BYTES,
  MetricsSnapshot,
  pickLatest
} from './agent-metrics';

export class AgentMetricsSubscription {

  private entitySub: TelemetrySubscriber | null = null;
  private agentSub: TelemetrySubscriber | null = null;

  private readonly entityValues$ = new BehaviorSubject<LatestValues>({});
  private readonly agentValues$ = new BehaviorSubject<LatestValues>({});

  readonly snapshot$: Observable<MetricsSnapshot>;

  constructor(private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone) {
    this.snapshot$ = this.entityValues$.pipe(
      map(() => this.buildSnapshot()),
      distinctUntilChanged((a, b) =>
        a.cpuPercent === b.cpuPercent
        && a.cpuPercentTs === b.cpuPercentTs
        && a.memoryBytes === b.memoryBytes
        && a.memoryBytesTs === b.memoryBytesTs
        && a.onlineCpus === b.onlineCpus
        && a.hostMemoryBytes === b.hostMemoryBytes
        && a.diskBytes === b.diskBytes
        && a.diskBytesTs === b.diskBytesTs
        && a.hostDiskTotal === b.hostDiskTotal
        && a.volumeBytes === b.volumeBytes
        && a.volumeBytesTs === b.volumeBytesTs
        && a.storageBytes === b.storageBytes
        && a.storageBytesTs === b.storageBytesTs
      )
    );
  }

  private buildSnapshot(): MetricsSnapshot {
    const entity = this.entityValues$.value;
    const agent = this.agentValues$.value;
    const cpu = entity[METRIC_KEY_CPU_PERCENT];
    const mem = entity[METRIC_KEY_MEMORY_BYTES];
    const vol = entity[METRIC_KEY_VOLUME_BYTES];
    const size = entity[METRIC_KEY_SIZE_BYTES];
    const disk = agent[METRIC_KEY_DISK_BYTES];
    // storageBytes = whichever storage metric the entity reports (volumeBytes
    // for AgentApps, sizeBytes for volume-typed units). Lets the multi-entity
    // chart panel plot a single Storage stack without knowing entity type.
    const storage = vol ?? size;
    return {
      cpuPercent: cpu?.value ?? null,
      cpuPercentTs: cpu?.ts ?? null,
      memoryBytes: mem?.value ?? null,
      memoryBytesTs: mem?.ts ?? null,
      onlineCpus: agent[METRIC_KEY_ONLINE_CPUS]?.value ?? null,
      hostMemoryBytes: agent[METRIC_KEY_HOST_MEMORY_BYTES]?.value ?? null,
      diskBytes: disk?.value ?? null,
      diskBytesTs: disk?.ts ?? null,
      hostDiskTotal: agent[METRIC_KEY_HOST_DISK_TOTAL]?.value ?? null,
      volumeBytes: vol?.value ?? null,
      volumeBytesTs: vol?.ts ?? null,
      storageBytes: storage?.value ?? null,
      storageBytesTs: storage?.ts ?? null
    };
  }

  // entityKeys / agentKeys let callers narrow the WS subscription to only the
  // metrics relevant for the entity type — e.g. CONTAINER units only need
  // cpu/mem, VOLUME units only need sizeBytes. Defaults to the full key sets.
  subscribe(entityId: EntityId, agentId: EntityId | null,
            entityKeys: string[] = ENTITY_METRIC_KEYS,
            agentKeys: string[] = AGENT_NORMALIZATION_KEYS): void {
    this.tearDown();
    if (entityKeys.length > 0) {
      this.entitySub = TelemetrySubscriber.createEntityAttributesSubscription(
        this.telemetryWsService, entityId, LatestTelemetry.LATEST_TELEMETRY, this.zone, entityKeys
      );
      this.entitySub.data$.subscribe(update => {
        const next = { ...this.entityValues$.value };
        for (const key of entityKeys) {
          const value = pickLatest(update, key);
          if (value != null) {
            next[key] = value;
          }
        }
        this.entityValues$.next(next);
      });
      this.entitySub.subscribe();
    }

    if (agentId && agentKeys.length > 0) {
      this.agentSub = TelemetrySubscriber.createEntityAttributesSubscription(
        this.telemetryWsService, agentId, LatestTelemetry.LATEST_TELEMETRY, this.zone, agentKeys
      );
      this.agentSub.data$.subscribe(update => {
        const next = { ...this.agentValues$.value };
        for (const key of agentKeys) {
          const value = pickLatest(update, key);
          if (value != null) {
            next[key] = value;
          }
        }
        this.agentValues$.next(next);
        this.entityValues$.next({ ...this.entityValues$.value });
      });
      this.agentSub.subscribe();
    }
  }

  tearDown(): void {
    this.entitySub?.unsubscribe();
    this.entitySub?.complete();
    this.entitySub = null;
    this.agentSub?.unsubscribe();
    this.agentSub?.complete();
    this.agentSub = null;
    this.entityValues$.next({});
    this.agentValues$.next({});
  }

  static empty(): MetricsSnapshot {
    return { ...EMPTY_METRICS_SNAPSHOT };
  }
}
