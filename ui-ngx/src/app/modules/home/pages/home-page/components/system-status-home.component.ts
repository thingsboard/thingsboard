// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  afterRenderEffect,
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  NgZone,
  Renderer2,
  signal,
  viewChild
} from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import * as echarts from 'echarts/core';
import { CallbackDataParams } from 'echarts/types/dist/shared';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { EntityDataCmd, IntervalType, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityKeyType } from '@shared/models/query/query.models';
import { AggregationType } from '@shared/models/time/time.models';
import { UpdateMessage } from '@shared/models/settings.models';
import { HomeCardId } from '@shared/models/user-settings.models';
import { ECharts, echartsModule, EChartsOption } from '@home/components/widget/lib/chart/echarts-widget.models';
import { HOME_CARDS } from '@home/pages/home-page/home-cards.models';
import { SysAdminHomeService } from '@home/pages/home-page/sys-admin-home.service';
import { homeChartHoverSymbol, homeChartTooltipHtml, homeChartTooltipOptions } from '@home/pages/home-page/home-chart.models';

type SystemMetric = 'cpu' | 'ram' | 'disk';
type SystemLevel = 'crit' | 'warn' | '';
type ServiceSortKey = 'serviceId' | 'serviceType' | SystemMetric;

interface MetricConfig {
  id: SystemMetric;
  key: string;
  label: string;
  color: string;
}

interface ClusterService {
  serviceId: string;
  serviceType: string;
  cpuUsage: number;
  cpuCount: number;
  memoryUsage: number;
  totalMemory: number;
  discUsage: number;
  totalDiscSpace: number;
}

interface SystemWorst {
  metric: SystemMetric;
  value: number;
  level: SystemLevel;
  services: string[];
}

const METRICS: MetricConfig[] = [
  {id: 'cpu', key: 'cpuUsage', label: 'widgets.system-info.cpu', color: '#305680'},
  {id: 'ram', key: 'memoryUsage', label: 'widgets.system-info.ram', color: '#9c27b0'},
  {id: 'disk', key: 'discUsage', label: 'widgets.system-info.disk', color: '#26a69a'}
];

const SERVICE_KEYS: Record<SystemMetric, 'cpuUsage' | 'memoryUsage' | 'discUsage'> = {cpu: 'cpuUsage', ram: 'memoryUsage', disk: 'discUsage'};
const LATEST_KEYS = ['clusterMode', 'clusterSystemData', 'cpuUsage', 'cpuCount', 'memoryUsage', 'totalMemory', 'discUsage', 'totalDiscSpace'];
const HOUR = 3600000;
const MINUTE = 60000;
const LABEL_STEP = 15 * MINUTE;
const LOADING_HEIGHT = 304;
const SERVICES_PREVIEW = 8;
const SERVICES_SLACK = 2;
const SERVICES_LIMIT = 20;
const AXIS_LABEL = {color: 'rgba(0, 0, 0, 0.56)', fontFamily: 'Roboto', fontSize: 12, fontWeight: 400, lineHeight: 16};

const GB = 1024 * 1024 * 1024;

const gigabytes = (bytes: number): number => Math.round(bytes / GB);

const level = (value: number): SystemLevel => value > 85 ? 'crit' : value > 75 ? 'warn' : '';

@Component({
  selector: 'tb-system-status-home',
  templateUrl: './system-status-home.component.html',
  styleUrls: ['./system-status-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class SystemStatusHomeComponent {

  readonly metrics = METRICS;

  private telemetryWsService = inject(TelemetryWebsocketService);
  private zone = inject(NgZone);
  private renderer = inject(Renderer2);
  private translate = inject(TranslateService);
  private host = inject<ElementRef<HTMLElement>>(ElementRef);
  private sysAdminHome = inject(SysAdminHomeService);

  private readonly latest = signal<Record<string, string> | null>(null);
  private readonly history = signal<Record<SystemMetric, [number, number][]>>({cpu: [], ram: [], disk: []});
  private readonly window = signal({start: Date.now() - HOUR, end: Date.now()});

  readonly servicesPreview = SERVICES_PREVIEW;
  readonly servicesSlack = SERVICES_SLACK;
  readonly servicesLimit = SERVICES_LIMIT;

  readonly expanded = signal(false);

  readonly update = signal<UpdateMessage | null>(null);
  readonly sort = signal<[ServiceSortKey, 'asc' | 'desc']>(['serviceId', 'asc']);

  readonly cluster = computed(() => this.latest()?.clusterMode === 'true');

  readonly loaded = computed(() => this.latest() !== null);

  readonly cardDefinition = computed(() => {
    const card = HOME_CARDS[HomeCardId.SYSTEM_STATUS];
    return this.loaded() ? card : {...card, height: LOADING_HEIGHT};
  });

  readonly services = computed<ClusterService[]>(() => {
    const data = this.latest()?.clusterSystemData;
    if (!this.cluster() || !data) {
      return [];
    }
    try {
      return JSON.parse(data);
    } catch {
      return [];
    }
  });

  readonly sortedServices = computed(() => {
    const [key, direction] = this.sort();
    const value = (service: ClusterService) => key === 'serviceId' || key === 'serviceType' ? service[key] : service[SERVICE_KEYS[key]];
    return [...this.services()].sort((a, b) => {
      const first = value(a);
      const second = value(b);
      return (first > second ? 1 : first < second ? -1 : 0) * (direction === 'asc' ? 1 : -1);
    });
  });

  readonly shownServices = computed(() => {
    const services = this.sortedServices();
    if (this.expanded() || services.length <= SERVICES_PREVIEW + SERVICES_SLACK) {
      return services;
    }
    return services.slice(0, SERVICES_PREVIEW);
  });

  readonly scrolling = computed(() => this.expanded() && this.services().length > SERVICES_LIMIT);

  readonly gauges = computed(() => {
    const latest = this.latest() ?? {};
    const totals: Record<SystemMetric, string> = {
      cpu: this.translate.instant('home.system.cores', {count: this.number(latest.cpuCount)}),
      ram: this.translate.instant('home.system.gb', {value: gigabytes(this.number(latest.totalMemory))}),
      disk: this.translate.instant('home.system.gb', {value: gigabytes(this.number(latest.totalDiscSpace))})
    };
    const totalValues: Record<SystemMetric, number> = {
      cpu: 100, ram: this.number(latest.totalMemory), disk: this.number(latest.totalDiscSpace)
    };
    return METRICS.map(metric => {
      const value = this.number(latest[metric.key]);
      return {
        ...metric,
        value,
        level: level(value),
        total: totals[metric.id],
        tooltip: this.valueTooltip(metric.id, value, totalValues[metric.id])
      };
    });
  });

  readonly worst = computed<SystemWorst | null>(() => {
    if (!this.loaded()) {
      return null;
    }
    let loads: {metric: SystemMetric; value: number; service?: string}[];
    if (this.cluster()) {
      loads = this.services().flatMap(service => METRICS.map(metric =>
        ({metric: metric.id, value: Number(service[SERVICE_KEYS[metric.id]]) || 0, service: service.serviceId})));
    } else {
      loads = this.gauges().map(gauge => ({metric: gauge.id, value: gauge.value}));
    }
    if (!loads.length) {
      return null;
    }
    const top = loads.reduce((max, load) => load.value > max.value ? load : max);
    return {
      metric: top.metric,
      value: top.value,
      level: level(top.value),
      services: loads.filter(load => load.service && load.metric === top.metric && load.value === top.value).map(load => load.service)
    };
  });

  readonly callout = computed(() => {
    const worst = this.worst();
    if (!worst?.level) {
      return null;
    }
    const name = this.metricName(worst.metric);
    const value = Math.round(worst.value);
    const title = worst.services.length
      ? this.translate.instant('home.system.at-on', {name, value, services: worst.services.join(this.translate.instant('home.system.and'))})
      : this.translate.instant('home.system.at', {name, value});
    return {level: worst.level, title, text: this.levelText(worst.metric, worst.level).replace(/^[^.]+\.\s*/, '')};
  });

  readonly releaseHidden = computed(() => {
    const update = this.update();
    return !!update?.updateAvailable && this.sysAdminHome.hiddenRelease() === update.latestVersion;
  });

  private readonly chartElement = viewChild<ElementRef<HTMLElement>>('chart');

  private chart: ECharts | null = null;

  constructor() {
    this.sysAdminHome.update$.subscribe(update => this.update.set(update));
    const subscriber = this.subscribe();
    const tick = setInterval(() => this.window.set({start: Date.now() - HOUR, end: Date.now()}), MINUTE);
    const resizeObserver = new ResizeObserver(() => this.chart?.resize());
    resizeObserver.observe(this.host.nativeElement);
    afterRenderEffect(() => this.render());
    inject(DestroyRef).onDestroy(() => {
      subscriber.unsubscribe();
      clearInterval(tick);
      resizeObserver.disconnect();
      this.chart?.dispose();
    });
  }

  sortBy(key: ServiceSortKey): void {
    const [current, direction] = this.sort();
    if (current === key) {
      this.sort.set([key, direction === 'asc' ? 'desc' : 'asc']);
    } else {
      this.sort.set([key, key === 'serviceId' || key === 'serviceType' ? 'asc' : 'desc']);
    }
  }

  sortIcon(key: ServiceSortKey): string {
    const [current, direction] = this.sort();
    return current === key && direction === 'desc' ? 'arrow_downward' : 'arrow_upward';
  }

  ariaSort(key: ServiceSortKey): string {
    const [current, direction] = this.sort();
    return current === key ? (direction === 'asc' ? 'ascending' : 'descending') : 'none';
  }

  serviceValue(service: ClusterService, metric: SystemMetric): number {
    return Math.round(Number(service[SERVICE_KEYS[metric]]) || 0);
  }

  serviceLevel(service: ClusterService, metric: SystemMetric): SystemLevel {
    return level(Number(service[SERVICE_KEYS[metric]]) || 0);
  }

  serviceTooltip(service: ClusterService, metric: SystemMetric): string {
    const total = metric === 'cpu' ? 100 : metric === 'ram' ? service.totalMemory : service.totalDiscSpace;
    return this.valueTooltip(metric, Number(service[SERVICE_KEYS[metric]]) || 0, total);
  }

  showRelease(): void {
    this.sysAdminHome.setHiddenRelease(null);
    setTimeout(() => document.querySelector('[data-home-card="productRelease"]')?.scrollIntoView({behavior: 'smooth', block: 'nearest'}));
  }

  private subscribe(): TelemetrySubscriber {
    const now = Date.now();
    const cmd = new EntityDataCmd();
    cmd.query = {
      entityFilter: {type: AliasFilterType.apiUsageState},
      pageLink: {page: 0, pageSize: 1},
      entityFields: [],
      latestValues: LATEST_KEYS.map(key => ({type: EntityKeyType.TIME_SERIES, key}))
    };
    cmd.historyCmd = {
      keys: METRICS.map(metric => metric.key),
      startTs: now - HOUR,
      endTs: now,
      intervalType: IntervalType.MILLISECONDS,
      interval: 1000,
      timeZoneId: Intl.DateTimeFormat().resolvedOptions().timeZone,
      limit: 1000,
      agg: AggregationType.NONE
    };
    cmd.latestCmd = {keys: LATEST_KEYS.map(key => ({type: EntityKeyType.TIME_SERIES, key}))};
    const subscriber = new TelemetrySubscriber(this.telemetryWsService, this.zone);
    subscriber.subscriptionCommands.push(cmd);
    subscriber.entityData$.subscribe(update => {
      const entity = (update.data?.data ?? update.update ?? [])[0];
      if (!entity) {
        if (update.data) {
          this.latest.set({});
        }
        return;
      }
      const latest = {...(this.latest() ?? {})};
      LATEST_KEYS.forEach(key => {
        const value = entity.latest?.[EntityKeyType.TIME_SERIES]?.[key];
        if (value?.ts) {
          latest[key] = value.value;
        }
      });
      const history = {...this.history()};
      METRICS.forEach(metric => {
        const points = new Map<number, number>(history[metric.id]);
        (entity.timeseries?.[metric.key] ?? []).forEach(value => points.set(value.ts, Number(value.value)));
        const value = entity.latest?.[EntityKeyType.TIME_SERIES]?.[metric.key];
        if (value?.ts) {
          points.set(value.ts, Number(value.value));
        }
        history[metric.id] = [...points.entries()].filter(([, number]) => !isNaN(number)).sort((a, b) => a[0] - b[0]);
      });
      this.history.set(history);
      this.latest.set(latest);
      this.window.set({start: Date.now() - HOUR, end: Date.now()});
    });
    subscriber.subscribe();
    return subscriber;
  }

  private render(): void {
    const element = this.chartElement()?.nativeElement;
    const history = this.history();
    const window = this.window();
    if (!element) {
      this.chart?.dispose();
      this.chart = null;
      return;
    }
    echartsModule.init();
    if (this.chart && this.chart.getDom() !== element) {
      this.chart.dispose();
      this.chart = null;
    }
    if (!this.chart) {
      this.renderer.setStyle(element, 'letterSpacing', 'normal');
      this.chart = echarts.init(element, null, {renderer: 'svg'});
    }
    this.chart.setOption(this.option(history, window), true);
  }

  private option(history: Record<SystemMetric, [number, number][]>, window: {start: number; end: number}): EChartsOption {
    const span = window.end - window.start;
    const series = METRICS.map(metric => ({
      metric,
      points: history[metric.id].filter(([ts]) => ts >= window.start).map(([ts, value]) => [ts - window.start, value] as [number, number])
    }));
    return {
      backgroundColor: 'transparent',
      tooltip: [{
        ...homeChartTooltipOptions,
        formatter: (params: CallbackDataParams[]) => this.tooltip(params.filter(param => METRICS.some(metric => metric.id === param.seriesId)),
          window.start)
      }],
      grid: [{left: 40, right: 12, top: 6, bottom: 26, containLabel: false}],
      xAxis: [{
        type: 'value',
        min: 0,
        max: span,
        interval: LABEL_STEP,
        axisLine: {show: false},
        axisTick: {show: false},
        splitLine: {show: false},
        axisLabel: {
          ...AXIS_LABEL,
          margin: 12,
          showMinLabel: true,
          showMaxLabel: true,
          alignMinLabel: 'left',
          alignMaxLabel: 'right',
          formatter: (value: number) => this.xLabel(window.start, span, value)
        }
      }],
      yAxis: [{
        type: 'value',
        min: 0,
        max: 100,
        interval: 50,
        axisLine: {show: false},
        axisTick: {show: false},
        splitLine: {show: true, lineStyle: {color: 'rgba(0, 0, 0, 0.06)', width: 1}},
        axisLabel: {...AXIS_LABEL, margin: 8, formatter: (value: number) => `${value}%`}
      }],
      series: series.flatMap(({metric, points}) => [{
        id: metric.id,
        type: 'line' as const,
        name: this.translate.instant(metric.label),
        color: metric.color,
        smooth: false,
        showSymbol: false,
        ...homeChartHoverSymbol(metric.color),
        lineStyle: {width: 2, color: metric.color, cap: 'round' as const, join: 'round' as const},
        data: points
      }, {
        id: `${metric.id}-now`,
        type: 'line' as const,
        silent: true,
        tooltip: {show: false},
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 8,
        z: 3,
        itemStyle: {color: metric.color, borderColor: '#fff', borderWidth: 2},
        lineStyle: {width: 0},
        data: points.length ? [points[points.length - 1]] : []
      }]),
      animation: false
    };
  }

  private tooltip(params: CallbackDataParams[], start: number): string {
    if (!params.length) {
      return '';
    }
    const date = new Date(Number(params[0].value[0]) + start);
    const time = `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
    return homeChartTooltipHtml(time, params.map(param => {
      const metric = METRICS.find(item => item.id === param.seriesId);
      return {color: metric.color, name: this.metricName(metric.id), value: `${Math.round(Number(param.value[1]))}%`};
    }));
  }

  private xLabel(start: number, span: number, offset: number): string {
    if (offset >= span - 1000) {
      return this.translate.instant('home.system.now');
    }
    if (offset > span - LABEL_STEP + MINUTE) {
      return '';
    }
    const date = new Date(start + offset);
    return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
  }

  private valueTooltip(metric: SystemMetric, value: number, total: number): string {
    const rounded = Math.round(value);
    const used = metric === 'cpu' ? `${rounded} / 100 %` : `${gigabytes(total * value / 100)} / ${gigabytes(total)} GB`;
    const valueLevel = level(value);
    return valueLevel ? `${used}\n\n${this.levelText(metric, valueLevel)}` : used;
  }

  private levelText(metric: SystemMetric, valueLevel: SystemLevel): string {
    return this.translate.instant(`widgets.system-info.${metric}-${valueLevel === 'crit' ? 'critical' : 'warning'}-text`);
  }

  private metricName(metric: SystemMetric): string {
    return this.translate.instant(`widgets.system-info.${metric}`);
  }

  private number(value: string | undefined): number {
    const number = Number(value);
    return isFinite(number) ? number : 0;
  }
}
