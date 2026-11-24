// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  Injector,
  NgZone,
  Renderer2,
  signal,
  viewChild
} from '@angular/core';
import { DomSanitizer } from '@angular/platform-browser';
import { TranslateService } from '@ngx-translate/core';
import * as echarts from 'echarts/core';
import tinycolor from 'tinycolor2';
import { CallbackDataParams } from 'echarts/types/dist/shared';
import { TelemetryWebsocketService } from '@core/ws/telemetry-websocket.service';
import { EntityDataCmd, IntervalType, TelemetrySubscriber } from '@shared/models/telemetry/telemetry.models';
import { AliasFilterType } from '@shared/models/alias.models';
import { EntityKeyType } from '@shared/models/query/query.models';
import { AggregationType } from '@shared/models/time/time.models';
import { DateFormatProcessor } from '@shared/models/widget-settings.models';
import { HomeCardId } from '@shared/models/user-settings.models';
import { formatValue } from '@core/utils';
import { ECharts, echartsModule, EChartsOption } from '@home/components/widget/lib/chart/echarts-widget.models';
import { TimeSeriesChartTooltip } from '@home/components/widget/lib/chart/time-series-chart-tooltip.models';
import { TimeSeriesChartDataItem } from '@home/components/widget/lib/chart/time-series-chart.models';
import { HOME_CARDS } from '@home/pages/home-page/home-cards.models';
import { HomeToggleOption } from '@home/pages/home-page/components/home-toggle.component';

type ActivitySeries = 'devices' | 'messages';

const DAY = 86400000;
const DAYS = 30;
const LABEL_STEP = 7;
const SCALE_STEPS = [1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000];

const startOfFirstDay = (now: number): number => {
  const date = new Date(now - (DAYS - 1) * DAY);
  date.setHours(0, 0, 0, 0);
  return date.getTime();
};

const shortNumber = (value: number): string => {
  const units: [number, string][] = [[1e9, 'B'], [1e6, 'M'], [1e3, 'K']];
  const unit = units.find(([size]) => value >= size);
  if (!unit) {
    return `${Math.round(value)}`;
  }
  return `${(Math.floor(value / unit[0] * 10) / 10).toString().replace(/\.0$/, '')}${unit[1]}`;
};

const scale = (points: [number, number][]): {max: number; interval: number} => {
  const top = Math.max(0, ...points.map(point => point[1]));
  const step = SCALE_STEPS.find(value => top / value <= 4) ?? SCALE_STEPS[SCALE_STEPS.length - 1];
  const max = Math.max(step, Math.ceil(top / step) * step);
  return {max, interval: (max / step) % 2 === 0 ? max / 2 : max};
};
const LIMIT = 25000;
const AXIS_LABEL = {color: 'rgba(0, 0, 0, 0.56)', fontFamily: 'Roboto', fontSize: 12, fontWeight: 400, lineHeight: 16};

interface ActivitySeriesConfig {
  id: ActivitySeries;
  key: string;
  label: string;
  daily: boolean;
}

const SERIES: ActivitySeriesConfig[] = [
  {id: 'devices', key: 'activeDevicesCountHourly', label: 'home.activity.devices', daily: false},
  {id: 'messages', key: 'transportMsgCountHourly', label: 'home.activity.messages', daily: true}
];

const font = (size: number, weight: string) => ({family: 'Roboto', size, sizeUnit: 'px', style: 'normal', weight, lineHeight: '16px'});

@Component({
  selector: 'tb-devices-activity-home',
  templateUrl: './devices-activity-home.component.html',
  styleUrls: ['./devices-activity-home.component.scss'],
  standalone: false,
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class DevicesActivityHomeComponent implements AfterViewInit {

  readonly card = HOME_CARDS[HomeCardId.ACTIVITY];

  readonly series = SERIES;

  readonly tabOptions: HomeToggleOption<ActivitySeries>[] = SERIES.map(series => ({value: series.id, label: series.label}));

  private telemetryWsService = inject(TelemetryWebsocketService);
  private zone = inject(NgZone);
  private renderer = inject(Renderer2);
  private sanitizer = inject(DomSanitizer);
  private translate = inject(TranslateService);
  private injector = inject(Injector);
  private host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly tab = signal<ActivitySeries>('devices');
  private readonly data = signal<Record<ActivitySeries, [number, number][]> | null>(null);

  readonly loaded = computed(() => this.data() !== null);
  readonly empty = computed(() => {
    const data = this.data();
    return !!data && SERIES.every(series => !data[series.id].some(point => point[1] > 0));
  });

  private readonly devicesChart = viewChild<ElementRef<HTMLElement>>('devicesChart');
  private readonly messagesChart = viewChild<ElementRef<HTMLElement>>('messagesChart');

  private charts: Partial<Record<ActivitySeries, ECharts>> = {};
  private subscribers: TelemetrySubscriber[] = [];
  private resizeObserver: ResizeObserver;

  private readonly tooltip = new TimeSeriesChartTooltip(this.renderer, this.sanitizer, {
    showTooltip: true,
    tooltipTrigger: undefined,
    tooltipLabelFont: font(12, '400'),
    tooltipLabelColor: 'rgba(0, 0, 0, 0.76)',
    tooltipValueFont: font(12, '500'),
    tooltipValueColor: 'rgba(0, 0, 0, 0.76)',
    tooltipShowDate: true,
    tooltipDateInterval: false,
    tooltipDateFormat: {format: null, lastUpdateAgo: false, custom: false, auto: true, autoDateFormatSettings: {}},
    tooltipDateFont: {...font(11, '400')},
    tooltipDateColor: 'rgba(0, 0, 0, 0.76)',
    tooltipBackgroundColor: 'rgba(255, 255, 255, 0.76)',
    tooltipBackgroundBlur: 4
  }, DateFormatProcessor.fromSettings(this.injector,
    {format: null, lastUpdateAgo: false, custom: false, auto: true, autoDateFormatSettings: {}}),
    (value, _latestData, units, decimals) => formatValue(value, decimals, units, false), this.translate);

  private endTs = Date.now();

  private startTs = startOfFirstDay(this.endTs);

  private readonly raw: Record<ActivitySeries, Map<number, number>> = {devices: new Map(), messages: new Map()};

  private historyLoaded = false;

  private subscriber: TelemetrySubscriber | null = null;

  private dayTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.subscribe();
    inject(DestroyRef).onDestroy(() => {
      this.subscriber?.unsubscribe();
      clearTimeout(this.dayTimer);
      this.resizeObserver?.disconnect();
      Object.values(this.charts).forEach(chart => chart.dispose());
    });
  }

  private subscribe(): void {
    this.subscriber?.unsubscribe();
    this.endTs = Date.now();
    this.startTs = startOfFirstDay(this.endTs);
    this.historyLoaded = false;
    SERIES.forEach(series => this.raw[series.id].clear());
    const keys = SERIES.map(series => series.key);
    const cmd = new EntityDataCmd();
    cmd.query = {
      entityFilter: {type: AliasFilterType.apiUsageState},
      pageLink: {page: 0, pageSize: 1},
      entityFields: [],
      latestValues: keys.map(key => ({type: EntityKeyType.TIME_SERIES, key}))
    };
    cmd.historyCmd = {
      keys,
      startTs: this.startTs,
      endTs: this.endTs,
      intervalType: IntervalType.MILLISECONDS,
      interval: 1000,
      timeZoneId: Intl.DateTimeFormat().resolvedOptions().timeZone,
      limit: LIMIT,
      agg: AggregationType.NONE
    };
    cmd.latestCmd = {keys: keys.map(key => ({type: EntityKeyType.TIME_SERIES, key}))};
    const subscriber = new TelemetrySubscriber(this.telemetryWsService, this.zone);
    subscriber.subscriptionCommands.push(cmd);
    subscriber.entityData$.subscribe(update => {
      const entities = update.data?.data ?? update.update ?? [];
      entities.forEach(entity => {
        SERIES.forEach(series => {
          (entity.timeseries?.[series.key] ?? []).forEach(value => this.put(series.id, value.ts, value.value));
          const latest = entity.latest?.[EntityKeyType.TIME_SERIES]?.[series.key];
          if (latest?.ts) {
            this.put(series.id, latest.ts, latest.value);
          }
        });
      });
      if (update.data) {
        this.historyLoaded = true;
      }
      if (this.historyLoaded) {
        this.refresh();
      }
    });
    subscriber.subscribe();
    this.subscriber = subscriber;
    clearTimeout(this.dayTimer);
    this.dayTimer = setTimeout(() => this.subscribe(), this.dayStart(DAYS) - Date.now() + 1000);
  }

  private put(series: ActivitySeries, ts: number, value: string): void {
    const number = Number(value);
    if (ts >= this.startTs && !isNaN(number)) {
      this.raw[series].set(ts, number);
    }
  }

  private refresh(): void {
    this.endTs = Math.max(this.endTs, Date.now());
    const points = (series: ActivitySeries) => [...this.raw[series].entries()].sort((a, b) => a[0] - b[0]);
    this.data.set({
      devices: points('devices').map(([ts, value]) => [ts - this.startTs, value] as [number, number]),
      messages: this.dailyBuckets(points('messages'))
    });
    setTimeout(() => this.render());
  }

  ngAfterViewInit(): void {
    this.resizeObserver = new ResizeObserver(() => Object.values(this.charts).forEach(chart => chart.resize()));
    this.resizeObserver.observe(this.host.nativeElement);
  }

  private dailyBuckets(values: [number, number][]): [number, number][] {
    const buckets: [number, number][] = Array.from({length: DAYS}, (_, index) => [this.dayStart(index) - this.startTs, 0]);
    buckets[DAYS - 1][0] = this.endTs - this.startTs;
    values.forEach(([ts, value]) => {
      const index = buckets.findIndex((_, day) => ts < this.dayStart(day + 1));
      if (ts >= this.startTs && index > -1) {
        buckets[index][1] += value;
      }
    });
    return buckets;
  }

  private dayStart(index: number): number {
    const date = new Date(this.startTs);
    date.setDate(date.getDate() + index);
    return date.getTime();
  }

  private xLabel(offset: number): string {
    const span = this.endTs - this.startTs;
    if (offset >= span - 1000) {
      return this.translate.instant('home.activity.today');
    }
    if (offset > span - (LABEL_STEP - 1) * DAY) {
      return '';
    }
    const date = new Date(this.dayStart(Math.round(offset / DAY)));
    return `${String(date.getDate()).padStart(2, '0')} ${date.toLocaleString(this.translate.currentLang?.replace('_', '-') || 'en-US', {month: 'short'})}`;
  }

  setTab(tab: ActivitySeries): void {
    this.tab.set(tab);
    setTimeout(() => Object.values(this.charts).forEach(chart => chart.resize()));
  }

  private render(): void {
    const data = this.data();
    if (!data || this.empty()) {
      return;
    }
    echartsModule.init();
    const color = getComputedStyle(this.host.nativeElement).getPropertyValue('--tb-primary-500').trim() || '#305680';
    SERIES.forEach(series => {
      const element = (series.id === 'devices' ? this.devicesChart() : this.messagesChart())?.nativeElement;
      if (!element) {
        return;
      }
      let chart = this.charts[series.id];
      if (chart && chart.getDom() !== element) {
        chart.dispose();
        chart = null;
      }
      if (!chart) {
        this.renderer.setStyle(element, 'letterSpacing', 'normal');
        chart = echarts.init(element, null, {renderer: 'svg'});
        this.charts[series.id] = chart;
      }
      chart.setOption(this.option(series, data[series.id], color), true);
    });
  }

  private option(series: ActivitySeriesConfig, points: [number, number][], color: string): EChartsOption {
    const label = this.translate.instant(series.label);
    const daily = series.daily;
    const dataItems = [{id: series.id, units: '', decimals: 0, latestData: {}} as unknown as TimeSeriesChartDataItem];
    const span = this.endTs - this.startTs;
    const yScale = scale(points);
    const toAbsolute = (params: CallbackDataParams[]) => params.map(param =>
      ({...param, value: [Number(param.value[0]) + this.startTs, param.value[1]]}) as CallbackDataParams);
    return {
      backgroundColor: 'transparent',
      tooltip: [{
        trigger: 'axis',
        confine: true,
        appendTo: 'body',
        axisPointer: {type: 'line'},
        formatter: (params: CallbackDataParams[]) =>
          this.tooltip.formatted(toAbsolute(params.filter(param => param.seriesId === series.id)), -1, dataItems, daily ? DAY : null),
        padding: [8, 12],
        backgroundColor: 'rgba(255, 255, 255, 0.76)',
        borderWidth: 0,
        extraCssText: 'line-height: 1; backdrop-filter: blur(4px);'
      }],
      grid: [{left: 36, right: 28, top: 6, bottom: 26, containLabel: false}],
      xAxis: [{
        type: 'value',
        min: 0,
        max: span,
        interval: LABEL_STEP * DAY,
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
          formatter: (value: number) => this.xLabel(value)
        }
      }],
      yAxis: [{
        type: 'value',
        min: 0,
        max: yScale.max,
        interval: yScale.interval,
        axisLine: {show: false},
        axisTick: {show: false},
        splitLine: {show: true, lineStyle: {color: 'rgba(0, 0, 0, 0.06)', width: 1}},
        axisLabel: {
          ...AXIS_LABEL,
          margin: 8,
          formatter: (value: number) => value >= 1000 ? `${value / 1000}k` : `${value}`
        }
      }],
      series: [{
        id: series.id,
        type: 'line',
        name: label,
        color,
        smooth: false,
        showSymbol: false,
        lineStyle: {width: 2, color, cap: 'round', join: 'round'},
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            {offset: 0, color: tinycolor(color).setAlpha(0.12).toRgbString()},
            {offset: 1, color: tinycolor(color).setAlpha(0).toRgbString()}
          ])
        },
        data: points
      }, {
        id: `${series.id}-now`,
        type: 'line',
        silent: true,
        tooltip: {show: false},
        showSymbol: true,
        symbol: 'circle',
        symbolSize: 8,
        z: 3,
        itemStyle: {color, borderColor: '#fff', borderWidth: 2},
        lineStyle: {width: 0},
        label: {
          show: true,
          position: 'right',
          distance: 3,
          color: 'rgba(0, 0, 0, 0.76)',
          fontFamily: 'Roboto',
          fontSize: 12,
          fontWeight: 500,
          lineHeight: 16,
          formatter: (param: CallbackDataParams) => shortNumber(Number(param.value[1]))
        },
        data: points.length ? [points[points.length - 1]] : []
      }],
      animation: true,
      animationThreshold: 2000,
      animationDuration: 200,
      animationEasing: 'cubicOut',
      animationDelay: 0,
      animationDurationUpdate: 300,
      animationEasingUpdate: 'cubicOut',
      animationDelayUpdate: 0
    };
  }
}
