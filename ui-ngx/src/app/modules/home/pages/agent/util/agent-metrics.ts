// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { SubscriptionData, SubscriptionUpdate } from '@shared/models/telemetry/telemetry.models';
import { FileSizePipe } from '@shared/pipe/file-size.pipe';

export const METRIC_KEY_CPU_PERCENT = 'cpuPercent';
export const METRIC_KEY_MEMORY_BYTES = 'memoryBytes';
export const METRIC_KEY_ONLINE_CPUS = 'onlineCpus';
export const METRIC_KEY_HOST_MEMORY_BYTES = 'hostMemoryBytes';
export const METRIC_KEY_DISK_BYTES = 'diskBytes';
export const METRIC_KEY_HOST_DISK_TOTAL = 'hostDiskTotal';
export const METRIC_KEY_VOLUME_BYTES = 'volumeBytes';
export const METRIC_KEY_SIZE_BYTES = 'sizeBytes';

export const ENTITY_METRIC_KEYS = [METRIC_KEY_CPU_PERCENT, METRIC_KEY_MEMORY_BYTES, METRIC_KEY_VOLUME_BYTES, METRIC_KEY_SIZE_BYTES];
export const AGENT_NORMALIZATION_KEYS = [
  METRIC_KEY_ONLINE_CPUS,
  METRIC_KEY_HOST_MEMORY_BYTES,
  METRIC_KEY_DISK_BYTES,
  METRIC_KEY_HOST_DISK_TOTAL
];

export interface MetricsSnapshot {
  cpuPercent: number | null;
  cpuPercentTs: number | null;
  memoryBytes: number | null;
  memoryBytesTs: number | null;
  onlineCpus: number | null;
  hostMemoryBytes: number | null;
  diskBytes: number | null;
  diskBytesTs: number | null;
  hostDiskTotal: number | null;
  volumeBytes: number | null;
  volumeBytesTs: number | null;
  /** Unified per-entity storage byte count. For AgentApp entities this is
   *  volumeBytes (sum of project's volumes). For volume-typed AgentAppUnit
   *  entities this is sizeBytes (the volume's own size). Lets the multi-entity
   *  chart panel plot a single "Storage" stack regardless of entity type. */
  storageBytes: number | null;
  storageBytesTs: number | null;
}

export const EMPTY_METRICS_SNAPSHOT: MetricsSnapshot = {
  cpuPercent: null,
  cpuPercentTs: null,
  memoryBytes: null,
  memoryBytesTs: null,
  onlineCpus: null,
  hostMemoryBytes: null,
  diskBytes: null,
  diskBytesTs: null,
  hostDiskTotal: null,
  volumeBytes: null,
  volumeBytesTs: null,
  storageBytes: null,
  storageBytesTs: null
};

export function formatCpu(cpuPercent: number | null, onlineCpus: number | null): string {
  if (cpuPercent == null) {
    return '—';
  }
  const value = `${cpuPercent.toFixed(2)}%`;
  if (onlineCpus == null || onlineCpus <= 0) {
    return value;
  }
  return `${value} / ${(onlineCpus * 100).toFixed(0)}%`;
}

const fileSizePipe = new FileSizePipe();

export function formatBytes(bytes: number | null, fractionDigits?: number): string {
  if (bytes == null || !Number.isFinite(bytes)) {
    return '—';
  }
  return fileSizePipe.transform(bytes, fractionDigits);
}

// Fallback y-axis ceilings used only when a chart has no data yet, so the axis
// still renders its scale (0-100% / 0-100MB) instead of collapsing.
export const CPU_EMPTY_AXIS_MAX = 100;
export const BYTES_EMPTY_AXIS_MAX = 100 * 1024 * 1024;

// Returns an ECharts value-axis `max` resolver: keeps auto-scaling when there
// is real (positive) data, but falls back to a fixed ceiling when the series is
// empty/all-zero so the axis still draws its scale instead of collapsing.
export function emptyAxisMax(fallback: number): (value: { min: number; max: number }) => number | null {
  return value => (Number.isFinite(value.max) && value.max > 0 ? null : fallback);
}

export function formatMemoryWithMax(memoryBytes: number | null, hostMemoryBytes: number | null): string {
  if (memoryBytes == null) {
    return '—';
  }
  if (hostMemoryBytes == null || hostMemoryBytes <= 0) {
    return formatBytes(memoryBytes);
  }
  return `${formatBytes(memoryBytes)} / ${formatBytes(hostMemoryBytes)}`;
}

export function memoryHostPercent(memoryBytes: number | null, hostMemoryBytes: number | null): number | null {
  if (memoryBytes == null || hostMemoryBytes == null || hostMemoryBytes <= 0) {
    return null;
  }
  return (memoryBytes / hostMemoryBytes) * 100;
}

export function formatDiskWithMax(diskBytes: number | null, hostDiskTotal: number | null): string {
  if (diskBytes == null) {
    return '—';
  }
  if (hostDiskTotal == null || hostDiskTotal <= 0) {
    return formatBytes(diskBytes);
  }
  return `${formatBytes(diskBytes)} / ${formatBytes(hostDiskTotal)}`;
}

export function diskHostPercent(diskBytes: number | null, hostDiskTotal: number | null): number | null {
  if (diskBytes == null || hostDiskTotal == null || hostDiskTotal <= 0) {
    return null;
  }
  return (diskBytes / hostDiskTotal) * 100;
}

export interface LatestValues {
  [key: string]: LatestPoint | null;
}

export interface LatestPoint {
  value: number;
  ts: number;
}

export function pickLatest(update: SubscriptionUpdate | null, key: string): LatestPoint | null {
  const data = update?.data as SubscriptionData | undefined;
  if (!data) {
    return null;
  }
  const entries = data[key];
  if (!entries?.length) {
    return null;
  }
  const [ts, raw] = entries[0];
  const parsed = typeof raw === 'number' ? raw : parseFloat(raw);
  if (!Number.isFinite(parsed)) {
    return null;
  }
  return { value: parsed, ts };
}

export function formatUpdatedAt(ts: number | null): string | null {
  if (ts == null || !Number.isFinite(ts)) {
    return null;
  }
  return new Date(ts).toLocaleTimeString();
}

