// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { ChangeDetectionStrategy, Component, EventEmitter, Input, Output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import {
  diskHostPercent,
  formatBytes,
  formatCpu,
  formatDiskWithMax,
  formatMemoryWithMax,
  formatUpdatedAt,
  memoryHostPercent,
  MetricsSnapshot
} from '@home/pages/agent/util/agent-metrics';

@Component({
  selector: 'tb-agent-metrics-strip',
  templateUrl: './agent-metrics-strip.component.html',
  styleUrls: ['./agent-metrics-strip.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class AgentMetricsStripComponent {

  @Input() snapshot: MetricsSnapshot | null = null;
  @Input() showChartsButton = false;
  @Input() chartsActive = false;
  @Input() compact = false;
  @Input() aggregated = true;

  @Output() toggleCharts = new EventEmitter<void>();

  constructor(private translate: TranslateService) {}

  private get scopeLabel(): string | null {
    return this.aggregated ? this.translate.instant('agent.metrics-all-containers') : null;
  }

  get cpuLabel(): string {
    return formatCpu(this.snapshot?.cpuPercent ?? null, this.snapshot?.onlineCpus ?? null);
  }

  get cpuTooltip(): string | null {
    const snap = this.snapshot;
    if (!snap || snap.cpuPercent == null) {
      return null;
    }
    const percent = snap.cpuPercent.toFixed(2);
    const head = snap.onlineCpus == null
      ? this.translate.instant('agent.metrics-cpu-percent', { percent })
      : this.translate.instant('agent.metrics-cpu-percent-of-cores',
        { percent, cores: snap.onlineCpus, cap: (snap.onlineCpus * 100).toFixed(0) });
    return joinLines(this.scopeLabel, head, this.updatedAt(snap.cpuPercentTs));
  }

  get memLabel(): string {
    return formatMemoryWithMax(this.snapshot?.memoryBytes ?? null, this.snapshot?.hostMemoryBytes ?? null);
  }

  get memTooltip(): string | null {
    const snap = this.snapshot;
    if (!snap || snap.memoryBytes == null) {
      return null;
    }
    const pct = memoryHostPercent(snap.memoryBytes, snap.hostMemoryBytes);
    const head = pct == null
      ? formatBytes(snap.memoryBytes)
      : this.usedOfTotal(snap.memoryBytes, snap.hostMemoryBytes, pct);
    return joinLines(this.scopeLabel, head, this.updatedAt(snap.memoryBytesTs));
  }

  get hasDisk(): boolean {
    return this.snapshot?.diskBytes != null;
  }

  get diskLabel(): string {
    return formatDiskWithMax(this.snapshot?.diskBytes ?? null, this.snapshot?.hostDiskTotal ?? null);
  }

  get diskTooltip(): string | null {
    const snap = this.snapshot;
    if (!snap || snap.diskBytes == null) {
      return null;
    }
    const pct = diskHostPercent(snap.diskBytes, snap.hostDiskTotal);
    const head = pct == null
      ? formatBytes(snap.diskBytes)
      : this.usedOfTotal(snap.diskBytes, snap.hostDiskTotal, pct);
    return joinLines(head, this.updatedAt(snap.diskBytesTs));
  }

  get hasVolume(): boolean {
    return this.snapshot?.volumeBytes != null;
  }

  get volumeLabel(): string {
    return formatBytes(this.snapshot?.volumeBytes ?? null);
  }

  get volumeTooltip(): string | null {
    const snap = this.snapshot;
    if (!snap || snap.volumeBytes == null) {
      return null;
    }
    return joinLines(formatBytes(snap.volumeBytes), this.updatedAt(snap.volumeBytesTs));
  }

  private usedOfTotal(used: number, total: number, percent: number): string {
    return this.translate.instant('agent.metrics-used-of-total',
      { used: formatBytes(used), total: formatBytes(total), percent: percent.toFixed(1) });
  }

  private updatedAt(ts: number | null): string | null {
    const time = formatUpdatedAt(ts);
    return time ? this.translate.instant('agent.metrics-updated-at', { time }) : null;
  }

  onToggleCharts(event: Event): void {
    event.stopPropagation();
    this.toggleCharts.emit();
  }
}

function joinLines(...parts: (string | null | undefined)[]): string {
  return parts.filter((p): p is string => !!p).join(' · ');
}
