// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  EventEmitter,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  Output,
  SimpleChanges,
  ViewChild
} from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import * as echarts from 'echarts/core';
import { LineSeriesOption } from 'echarts/charts';
import { AttributeService } from '@core/http/attribute.service';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { AgentAppUnitType } from '@shared/models/agent.models';
import { EntityId } from '@shared/models/id/entity-id';
import {
  ECharts,
  EChartsOption,
  echartsModule
} from '@home/components/widget/lib/chart/echarts-widget.models';
import { AgentMetricsSubscription } from '@home/pages/agent/util/agent-metrics-subscription';
import {
  BYTES_EMPTY_AXIS_MAX,
  CPU_EMPTY_AXIS_MAX,
  emptyAxisMax,
  formatBytes,
  METRIC_KEY_CPU_PERCENT,
  METRIC_KEY_DISK_BYTES,
  METRIC_KEY_HOST_DISK_TOTAL,
  METRIC_KEY_MEMORY_BYTES,
  METRIC_KEY_SIZE_BYTES,
  METRIC_KEY_VOLUME_BYTES,
  MetricsSnapshot
} from '@home/pages/agent/util/agent-metrics';
import { AggregationType } from '@shared/models/time/time.models';
import { TsValue } from '@shared/models/query/query.models';

const WINDOW_MS = 10 * 60 * 1000;
const HISTORY_LIMIT = 600;

const SERIES_PALETTE = [
  '#1976d2', '#7b1fa2', '#2e7d32', '#ef6c00', '#c62828',
  '#00838f', '#283593', '#558b2f', '#6a1b9a', '#ad1457',
  '#3949ab', '#00695c', '#bf360c', '#4527a0', '#1565c0'
];

export interface MetricsEntityRef {
  entityId: EntityId;
  label: string;
  /** When set, scopes the WS subscription + history fetch to just the keys
   *  that metric type carries:
   *    CONTAINER → cpuPercent + memoryBytes
   *    VOLUME    → sizeBytes
   *    NETWORK   → entity is skipped entirely (no charted telemetry today)
   *  Undefined (e.g. AgentApp entities on the agent applications drawer)
   *  defaults to cpu + memory. */
  type?: AgentAppUnitType;
}

interface Point { ts: number; value: number; }
interface EntityRuntime {
  ref: MetricsEntityRef;
  cpuPoints: Point[];
  memPoints: Point[];
  storagePoints: Point[];
  historyLoaded: boolean;
  subscription: AgentMetricsSubscription;
}

@Component({
  selector: 'tb-agent-multi-entity-metrics-panel',
  templateUrl: './agent-multi-entity-metrics-panel.component.html',
  styleUrls: ['./agent-multi-entity-metrics-panel.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
  standalone: false
})
export class AgentMultiEntityMetricsPanelComponent implements AfterViewInit, OnChanges, OnDestroy {

  @Input() entities: MetricsEntityRef[] = [];
  @Input() title = '';
  @Input() closeable = false;
  /** When set, the Storage chart shows two lines (host disk used + limit) for
   *  this entity instead of stacking per-entity volume sizes. Use on the agent
   *  applications drawer where storage is host-wide, not per-app. */
  @Input() hostDiskEntityId: EntityId | null = null;
  @Output() closePanel = new EventEmitter<void>();

  @ViewChild('panelRoot', { static: false }) panelRootRef!: ElementRef<HTMLDivElement>;
  @ViewChild('cpuChart', { static: false }) cpuChartRef!: ElementRef<HTMLDivElement>;
  @ViewChild('memChart', { static: false }) memChartRef!: ElementRef<HTMLDivElement>;
  @ViewChild('storageChart', { static: false }) storageChartRef!: ElementRef<HTMLDivElement>;

  private destroy$ = new Subject<void>();
  private cpuChart: ECharts | null = null;
  private memChart: ECharts | null = null;
  private storageChart: ECharts | null = null;
  private runtime = new Map<string, EntityRuntime>();
  private viewReady = false;
  private resizeObserver: ResizeObserver | null = null;
  private hostDiskSub: AgentMetricsSubscription | null = null;
  private hostDiskUsedPoints: Point[] = [];
  private hostDiskLimit: number | null = null;
  private hostDiskHistoryLoaded = false;

  constructor(private attributeService: AttributeService,
              private telemetryWsService: TelemetryWebsocketService,
              private zone: NgZone) {
    echartsModule.init();
  }

  ngAfterViewInit(): void {
    this.viewReady = true;
    this.initCharts();
    this.installResize();
    this.reconcile();
    this.setupHostDisk();
  }

  ngOnChanges(c: SimpleChanges): void {
    if (!this.viewReady) { return; }
    if (c['entities']) { this.reconcile(); }
    if (c['hostDiskEntityId']) { this.setupHostDisk(); }
  }

  ngOnDestroy(): void {
    this.destroy$.next(); this.destroy$.complete();
    this.runtime.forEach(rt => rt.subscription.tearDown());
    this.runtime.clear();
    this.hostDiskSub?.tearDown();
    this.hostDiskSub = null;
    this.resizeObserver?.disconnect();
    this.cpuChart?.dispose();
    this.memChart?.dispose();
    this.storageChart?.dispose();
  }

  onCloseClick(): void { this.closePanel.emit(); }

  private initCharts(): void {
    this.zone.runOutsideAngular(() => {
      this.cpuChart = echarts.init(this.cpuChartRef.nativeElement, null, { renderer: 'canvas' });
      this.memChart = echarts.init(this.memChartRef.nativeElement, null, { renderer: 'canvas' });
      this.storageChart = echarts.init(this.storageChartRef.nativeElement, null, { renderer: 'canvas' });
      this.cpuChart.setOption(this.baseOption(v => `${(v as number).toFixed(2)}%`, v => `${+v.toFixed(2)}%`, CPU_EMPTY_AXIS_MAX));
      this.memChart.setOption(this.baseOption(v => formatBytes(v as number), v => formatBytes(v, 0), BYTES_EMPTY_AXIS_MAX));
      this.storageChart.setOption(this.baseOption(v => formatBytes(v as number), v => formatBytes(v, 0), BYTES_EMPTY_AXIS_MAX));
    });
  }

  private installResize(): void {
    if (typeof ResizeObserver === 'undefined' || !this.panelRootRef) { return; }
    this.resizeObserver = new ResizeObserver(() => {
      this.cpuChart?.resize();
      this.memChart?.resize();
      this.storageChart?.resize();
    });
    this.resizeObserver.observe(this.panelRootRef.nativeElement);
  }

  private reconcile(): void {
    // NETWORK entities have no charted telemetry — drop them entirely so we
    // neither subscribe nor pad the legend.
    const incoming = new Map<string, MetricsEntityRef>(
      this.entities
        .filter(e => e.type !== AgentAppUnitType.NETWORK)
        .map(e => [`${e.entityId.entityType}:${e.entityId.id}`, e])
    );
    for (const [key, rt] of Array.from(this.runtime.entries())) {
      if (!incoming.has(key)) { rt.subscription.tearDown(); this.runtime.delete(key); }
    }
    for (const [key, ref] of incoming.entries()) {
      if (this.runtime.has(key)) { this.runtime.get(key)!.ref = ref; continue; }
      const rt: EntityRuntime = {
        ref, cpuPoints: [], memPoints: [], storagePoints: [], historyLoaded: false,
        subscription: new AgentMetricsSubscription(this.telemetryWsService, this.zone)
      };
      this.runtime.set(key, rt);
      this.loadHistory(rt);
      rt.subscription.snapshot$.pipe(takeUntil(this.destroy$))
        .subscribe(snap => this.onSnapshot(rt, snap));
      rt.subscription.subscribe(ref.entityId, null, entityKeysFor(ref.type), []);
    }
    this.apply();
  }

  private setupHostDisk(): void {
    this.hostDiskSub?.tearDown();
    this.hostDiskSub = null;
    this.hostDiskUsedPoints = [];
    this.hostDiskLimit = null;
    this.hostDiskHistoryLoaded = false;
    if (!this.hostDiskEntityId) {
      this.apply();
      return;
    }
    const id = this.hostDiskEntityId;
    this.hostDiskSub = new AgentMetricsSubscription(this.telemetryWsService, this.zone);
    const endTs = Date.now(); const startTs = endTs - WINDOW_MS;
    this.attributeService.getEntityTimeseries(
      id, [METRIC_KEY_DISK_BYTES, METRIC_KEY_HOST_DISK_TOTAL],
      startTs, endTs, HISTORY_LIMIT, AggregationType.NONE, undefined
    ).pipe(takeUntil(this.destroy$)).subscribe(data => {
      this.hostDiskUsedPoints = parsePoints(data[METRIC_KEY_DISK_BYTES]);
      const totals = parsePoints(data[METRIC_KEY_HOST_DISK_TOTAL]);
      this.hostDiskLimit = totals.length ? totals[totals.length - 1].value : null;
      this.hostDiskHistoryLoaded = true;
      this.apply();
    });
    // Subscribe with agentId==entityId so the AGENT_NORMALIZATION_KEYS stream
    // (diskBytes + hostDiskTotal) fires for the same entity.
    this.hostDiskSub.snapshot$.pipe(takeUntil(this.destroy$))
      .subscribe(snap => this.onHostDiskSnapshot(snap));
    this.hostDiskSub.subscribe(id, id);
  }

  private onHostDiskSnapshot(snap: MetricsSnapshot): void {
    if (!this.hostDiskHistoryLoaded) { return; }
    if (snap.diskBytes != null && snap.diskBytesTs != null) {
      if (!this.hostDiskUsedPoints.length || this.hostDiskUsedPoints[this.hostDiskUsedPoints.length - 1].ts !== snap.diskBytesTs) {
        this.hostDiskUsedPoints.push({ ts: snap.diskBytesTs, value: snap.diskBytes });
      }
    }
    if (snap.hostDiskTotal != null) {
      this.hostDiskLimit = snap.hostDiskTotal;
    }
    const cutoff = Date.now() - WINDOW_MS;
    this.hostDiskUsedPoints = this.hostDiskUsedPoints.filter(p => p.ts >= cutoff);
    this.apply();
  }

  private loadHistory(rt: EntityRuntime): void {
    const endTs = Date.now(); const startTs = endTs - WINDOW_MS;
    const keys = entityKeysFor(rt.ref.type);
    if (keys.length === 0) {
      rt.historyLoaded = true;
      this.apply();
      return;
    }
    this.attributeService.getEntityTimeseries(
      rt.ref.entityId, keys,
      startTs, endTs, HISTORY_LIMIT, AggregationType.NONE, undefined
    ).pipe(takeUntil(this.destroy$)).subscribe(data => {
      rt.cpuPoints = parsePoints(data[METRIC_KEY_CPU_PERCENT]);
      rt.memPoints = parsePoints(data[METRIC_KEY_MEMORY_BYTES]);
      // Each entity reports at most one of volumeBytes (AgentApp) or sizeBytes
      // (VOLUME unit) — merge them into a single storage series.
      const vol = parsePoints(data[METRIC_KEY_VOLUME_BYTES]);
      const size = parsePoints(data[METRIC_KEY_SIZE_BYTES]);
      rt.storagePoints = vol.length ? vol : size;
      rt.historyLoaded = true;
      this.apply();
    });
  }

  private onSnapshot(rt: EntityRuntime, snap: MetricsSnapshot): void {
    if (!rt.historyLoaded) { return; }
    if (snap.cpuPercent != null && snap.cpuPercentTs != null) {
      if (!rt.cpuPoints.length || rt.cpuPoints[rt.cpuPoints.length - 1].ts !== snap.cpuPercentTs) {
        rt.cpuPoints.push({ ts: snap.cpuPercentTs, value: snap.cpuPercent });
      }
    }
    if (snap.memoryBytes != null && snap.memoryBytesTs != null) {
      if (!rt.memPoints.length || rt.memPoints[rt.memPoints.length - 1].ts !== snap.memoryBytesTs) {
        rt.memPoints.push({ ts: snap.memoryBytesTs, value: snap.memoryBytes });
      }
    }
    if (snap.storageBytes != null && snap.storageBytesTs != null) {
      if (!rt.storagePoints.length || rt.storagePoints[rt.storagePoints.length - 1].ts !== snap.storageBytesTs) {
        rt.storagePoints.push({ ts: snap.storageBytesTs, value: snap.storageBytes });
      }
    }
    const cutoff = Date.now() - WINDOW_MS;
    rt.cpuPoints = rt.cpuPoints.filter(p => p.ts >= cutoff);
    rt.memPoints = rt.memPoints.filter(p => p.ts >= cutoff);
    rt.storagePoints = rt.storagePoints.filter(p => p.ts >= cutoff);
    this.apply();
  }

  private apply(): void {
    if (!this.cpuChart || !this.memChart || !this.storageChart) { return; }
    const ordered = this.entities
      .map(e => this.runtime.get(`${e.entityId.entityType}:${e.entityId.id}`))
      .filter((rt): rt is EntityRuntime => !!rt);
    // Filter each chart's entity set to those that actually report the
    // relevant metric. Without this, volume/network units appear in CPU/MEM
    // charts as 0-valued stacks (alignToGrid pads missing data with zero).
    const cpuEntities = ordered.filter(rt => rt.cpuPoints.length > 0);
    const memEntities = ordered.filter(rt => rt.memPoints.length > 0);
    const cpuLabels = cpuEntities.map(rt => rt.ref.label);
    const memLabels = memEntities.map(rt => rt.ref.label);
    const cpuTs = collectTimestamps(cpuEntities.map(rt => rt.cpuPoints));
    const memTs = collectTimestamps(memEntities.map(rt => rt.memPoints));
    const cpuSeries = cpuEntities.map((rt, idx) =>
      this.series(rt.ref.label, alignToGrid(rt.cpuPoints, cpuTs), 'cpu', idx));
    const memSeries = memEntities.map((rt, idx) =>
      this.series(rt.ref.label, alignToGrid(rt.memPoints, memTs), 'mem', idx));
    const now = Date.now(); const windowStart = now - WINDOW_MS;

    let storageSeries: EChartsOption['series'][];
    let storageLabels: string[];
    if (this.hostDiskEntityId) {
      // Agent context: two non-stacked lines (used + dashed limit).
      const usedData = this.hostDiskUsedPoints.map(p => [p.ts, p.value]);
      const limitData = this.hostDiskLimit != null
        ? [[windowStart, this.hostDiskLimit], [now, this.hostDiskLimit]]
        : [];
      storageLabels = ['Host disk used', 'Host disk limit'];
      storageSeries = [
        this.lineSeries('Host disk used', usedData, 0, false),
        this.lineSeries('Host disk limit', limitData, 4, true)
      ];
    } else {
      // App context: stacked area, one layer per volume — matches CPU/Memory.
      const withStorage = ordered.filter(rt => rt.storagePoints.length > 0);
      storageLabels = withStorage.map(rt => rt.ref.label);
      const storageTs = collectTimestamps(withStorage.map(rt => rt.storagePoints));
      storageSeries = withStorage.map((rt, idx) =>
        this.series(rt.ref.label, alignToGrid(rt.storagePoints, storageTs), 'storage', idx));
    }

    this.zone.runOutsideAngular(() => {
      this.cpuChart!.setOption({ legend: { data: cpuLabels }, xAxis: { min: windowStart, max: now }, series: cpuSeries }, { replaceMerge: ['series'] });
      this.memChart!.setOption({ legend: { data: memLabels }, xAxis: { min: windowStart, max: now }, series: memSeries }, { replaceMerge: ['series'] });
      this.storageChart!.setOption({ legend: { data: storageLabels }, xAxis: { min: windowStart, max: now }, series: storageSeries }, { replaceMerge: ['series'] });
    });
  }

  // lineSeries builds a non-stacked line series. Used for the Storage chart
  // where stacking would obscure individual volume sizes / host disk values.
  private lineSeries(name: string, data: any[], idx: number, dashed: boolean): LineSeriesOption {
    const color = SERIES_PALETTE[idx % SERIES_PALETTE.length];
    return {
      type: 'line', name, data,
      showSymbol: false, smooth: 0.2,
      emphasis: { disabled: true },
      lineStyle: { width: 2, color, type: dashed ? 'dashed' : 'solid' }
    };
  }

  private series(name: string, points: Point[], stack: string, idx: number): LineSeriesOption {
    const color = SERIES_PALETTE[idx % SERIES_PALETTE.length];
    const r = parseInt(color.slice(1, 3), 16), g = parseInt(color.slice(3, 5), 16), b = parseInt(color.slice(5, 7), 16);
    const rgba = (a: number) => `rgba(${r},${g},${b},${a})`;
    return {
      type: 'line', name, data: points.map(p => [p.ts, p.value]),
      showSymbol: false, smooth: 0.2, stack, stackStrategy: 'all',
      emphasis: { disabled: true },
      lineStyle: { width: 1, color },
      areaStyle: { color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1, colorStops: [
        { offset: 0, color: rgba(0.4) }, { offset: 1, color: rgba(0.05) }
      ]}}
    };
  }

  private baseOption(tipFmt: (v: number) => string, yFmt: (v: number) => string, emptyMax: number): EChartsOption {
    return {
      color: SERIES_PALETTE,
      grid: { left: 64, right: 16, top: 36, bottom: 32 },
      legend: { type: 'scroll', top: 4, left: 12, right: 12,
        textStyle: { fontSize: 11, color: 'rgba(0,0,0,0.7)' },
        itemHeight: 8, itemWidth: 12, icon: 'roundRect' },
      tooltip: { trigger: 'axis', formatter: (params: any) => {
        const arr = Array.isArray(params) ? params : [params];
        if (!arr.length || !arr[0]?.value) { return ''; }
        const ts = (arr[0].value as [number, number])[0];
        const time = new Date(ts).toLocaleTimeString();
        const lines = arr.map((p: any) => {
          const v = (p.value as [number, number])[1];
          const sw = `<span style="display:inline-block;width:8px;height:8px;border-radius:2px;background:${p.color};margin-right:6px"></span>`;
          return `<div style="font-size:11px;display:flex;align-items:center;justify-content:space-between;gap:12px"><span>${sw}${p.seriesName}</span><span style="font-weight:600">${tipFmt(v)}</span></div>`;
        }).join('');
        return `<div style="font-size:11px;color:rgba(0,0,0,0.6);margin-bottom:4px">${time}</div>${lines}`;
      }},
      xAxis: { type: 'time', axisLine: { lineStyle: { color: 'rgba(0,0,0,0.16)' }},
        axisLabel: { color: 'rgba(0,0,0,0.6)', fontSize: 11 }, splitLine: { show: false }},
      yAxis: { type: 'value', min: 0, max: emptyAxisMax(emptyMax), axisLine: { show: false }, axisTick: { show: false },
        axisLabel: { color: 'rgba(0,0,0,0.6)', fontSize: 11, formatter: yFmt },
        splitLine: { lineStyle: { color: 'rgba(0,0,0,0.06)' }}},
      animation: false, series: []
    } as EChartsOption;
  }
}

function collectTimestamps(seriesPoints: Point[][]): number[] {
  const all = new Set<number>();
  for (const points of seriesPoints) {
    for (const p of points) { all.add(p.ts); }
  }
  return Array.from(all).sort((a, b) => a - b);
}

function alignToGrid(points: Point[], grid: number[]): Point[] {
  if (!grid.length) { return []; }
  const out: Point[] = [];
  let i = 0;
  let lastValue: number | null = null;
  for (const ts of grid) {
    while (i < points.length && points[i].ts <= ts) {
      lastValue = points[i].value;
      i++;
    }
    out.push({ ts, value: lastValue ?? 0 });
  }
  return out;
}

function entityKeysFor(type: AgentAppUnitType | undefined): string[] {
  if (type === AgentAppUnitType.VOLUME) {
    return [METRIC_KEY_SIZE_BYTES];
  }
  if (type === AgentAppUnitType.NETWORK) {
    return [];
  }
  // CONTAINER and AgentApp (undefined): only cpu/memory matter for these charts.
  return [METRIC_KEY_CPU_PERCENT, METRIC_KEY_MEMORY_BYTES];
}

function parsePoints(entries: Array<TsValue> | undefined): Point[] {
  if (!entries) { return []; }
  const out: Point[] = [];
  for (const e of entries) {
    const v = typeof e.value === 'number' ? e.value as number : parseFloat(e.value);
    if (Number.isFinite(v)) { out.push({ ts: e.ts, value: v }); }
  }
  return out.sort((a, b) => a.ts - b.ts);
}
