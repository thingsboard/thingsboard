// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable, Injector, NgZone } from '@angular/core';
import { UtilsService } from '@core/services/utils.service';
import { IDynamicWidgetComponent, WidgetContext } from '@home/models/widget-component.models';
import {
  DataKey,
  Datasource,
  DatasourceType,
  Widget,
  WidgetComparisonSettings,
  WidgetConfig,
  widgetType
} from '@shared/models/widget.models';
import { defaultTimewindow, Timewindow } from '@shared/models/time/time.models';
import { DataKeyType } from '@shared/models/telemetry/telemetry.models';
import { Observable, ReplaySubject } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  IWidgetSubscription,
  WidgetSubscriptionCallbacks,
  WidgetSubscriptionContext,
  WidgetSubscriptionOptions
} from '@core/api/widget-api.models';
import { WidgetSubscription } from '@core/api/widget-subscription';
import { IDashboardComponent } from '@home/models/dashboard-component.models';
import { DashboardUtilsService } from '@core/services/dashboard-utils.service';
import { EntityDataService } from '@core/api/entity-data.service';
import { TranslateService } from '@ngx-translate/core';
import { TimeService } from '@core/services/time.service';
import { RafService } from '@core/services/raf.service';
import { DatePipe } from '@angular/common';

export type GenerateDataFunction = (random: () => number, time: number) => any;

@Injectable()
export class ReportWidgetContextService {

  constructor(private utils: UtilsService,
              private ngZone: NgZone,
              private dashboardUtils: DashboardUtilsService,
              private timeService: TimeService,
              private raf: RafService,
              private translate: TranslateService,
              private entityDataService: EntityDataService,
              private date: DatePipe,
              private injector: Injector) {
  }

  public createWidgetContext(type: widgetType,
                             settings: any,
                             timewindow: Timewindow,
                             datasources: Datasource[],
                             units: string,
                             decimals: number,
                             stateData: boolean,
                             callbacks: WidgetSubscriptionCallbacks,
                             genDataFunc?: GenerateDataFunction): Observable<WidgetContext> {
    const widget = this.createWidget(type, settings, timewindow, datasources, units, decimals, genDataFunc);
    const ctx = new WidgetContext(null, null, widget);
    ctx.$scope = {} as IDynamicWidgetComponent;
    ctx.$injector = this.injector;
    ctx.date = this.date;
    ctx.utilsService = this.utils;
    return this.createDefaultSubscription(widget, ctx, stateData, callbacks).pipe(
      map(() => {
        ctx.inited = true;
        return ctx;
      })
    );
  }

  public destroyWidgetContext(ctx: WidgetContext) {
    for (const id of Object.keys(ctx.subscriptions)) {
      const subscription = ctx.subscriptions[id];
      subscription.destroy();
    }
    ctx.subscriptions = {};
    ctx.destroy();
  }

  private createWidget(type: widgetType,
                       settings: any,
                       timewindow: Timewindow,
                       datasources: Datasource[],
                       units: string,
                       decimals: number,
                       genDataFunc?: GenerateDataFunction): Widget {
    return {
      type,
      config: {
        timewindow,
        datasources: this.prepareDatasources(datasources, genDataFunc),
        units,
        decimals,
        settings
      } as WidgetConfig
    } as Widget;
  }

  private prepareDatasources(datasources: Datasource[], genDataFunc?: GenerateDataFunction): Datasource[] {
    datasources = datasources || [];
    let dataKeyIndex = 0;
    for (let i = 0; i < datasources.length; i++) {
      const datasource = datasources[i];
      datasource.type = DatasourceType.function;
      datasource.name = 'Entity'+(i+1);
      datasource.entityName = 'Entity'+(i+1);
      for (const dataKey of (datasource.dataKeys || [])) {
        this.prepareDataKey(dataKey, dataKeyIndex, false, genDataFunc);
        dataKeyIndex++;
      }
      for (const dataKey of (datasource.latestDataKeys || [])) {
        this.prepareDataKey(dataKey, dataKeyIndex, true, genDataFunc);
        dataKeyIndex++;
      }
    }
    return datasources;
  }

  private prepareDataKey(dataKey: DataKey, index: number, latest: boolean, genDataFunc?: GenerateDataFunction): DataKey {
    const keyType = dataKey.type;
    dataKey.type = DataKeyType.function;
    if (latest) {
      dataKey.label = dataKey.name;
    }
    const keyRandom = this.createKeyRandom(index + 1);
    dataKey.builtInFunc = (time, _prevValue) => {
      if (keyType === DataKeyType.entityField) {
        return dataKey.label;
      } else if (genDataFunc) {
        return genDataFunc(keyRandom, time);
      } else {
        return this.reportPreviewKeyData(keyRandom, 5000);
      }
    };
    return dataKey;
  }

  private createKeyRandom(seed: number): () => number {
    let state = seed;
    return function() {
      state ^= state << 13;
      state ^= state >> 17;
      state ^= state << 5;
      return (state & 0x7fffffff) / 0x7fffffff;
    };
  }

  private reportPreviewKeyData(random: () => number, time: number): any {
    const numWaves = 5;
    let value = 0;
    for (let i = 0; i < numWaves; i++) {
      const amplitude = random() * 20;
      const frequency = random() * 0.001 + 0.0001;
      const phase = random() * Math.PI * 2;
      value += amplitude * Math.sin(frequency * time + phase);
    }

    const positiveOffset = random() * 80 + 20;
    value += positiveOffset;

    return value;
  }

  private createDefaultSubscription(widget: Widget, widgetContext: WidgetContext, stateData: boolean, callbacks: WidgetSubscriptionCallbacks): Observable<any> {
    const createSubscriptionSubject = new ReplaySubject<void>();
    const comparisonSettings: WidgetComparisonSettings = widgetContext.settings;
    const options: WidgetSubscriptionOptions = {
      type: widget.type,
      comparisonEnabled: comparisonSettings.comparisonEnabled,
      timeForComparison: comparisonSettings.timeForComparison,
      comparisonCustomIntervalValue: comparisonSettings.comparisonCustomIntervalValue,
      stateData,
      datasources: widget.config.datasources,
      useDashboardTimewindow: false,
      displayTimewindow: false,
      timeWindowConfig: widget.config.timewindow,
      dashboardTimewindow: defaultTimewindow(this.timeService),
      dataGenerationOptions: {
        fixedGenDataPoints: 10,
        generateLatestUpdates: false
      }
    };
    options.legendConfig = null;
    if (widget.config.settings.showLegend === true) {
      options.legendConfig = widget.config.settings.legendConfig;
    }
    options.decimals = widgetContext.decimals;
    options.units = widgetContext.units;
    options.callbacks = callbacks;

    this.createSubscription(widgetContext, options).subscribe({
      next: (subscription) => {
        widgetContext.datasources = subscription.datasources;
        widgetContext.data = subscription.data;
        widgetContext.latestData = subscription.latestData;
        widgetContext.hiddenData = subscription.hiddenData;
        widgetContext.timeWindow = subscription.timeWindow;
        widgetContext.defaultSubscription = subscription;
        this.ngZone.run(() => {
          createSubscriptionSubject.next();
          createSubscriptionSubject.complete();
        });
      },
      error: (err) => {
        this.ngZone.run(() => {
          createSubscriptionSubject.error(err);
        });
      }
    });
    return createSubscriptionSubject.asObservable();
  }

  private createSubscription(widgetContext: WidgetContext, options: WidgetSubscriptionOptions, subscribe?: boolean): Observable<IWidgetSubscription> {
    const createSubscriptionSubject = new ReplaySubject<IWidgetSubscription>();
    const subscription: IWidgetSubscription = new WidgetSubscription(this.createSubscriptionContext(widgetContext), options);
    subscription.init$.subscribe({
      next: () => {
        widgetContext.subscriptions[subscription.id] = subscription;
        if (subscribe) {
          subscription.subscribe();
        }
        createSubscriptionSubject.next(subscription);
        createSubscriptionSubject.complete();
      },
      error: (err) => {
        createSubscriptionSubject.error(err);
      }
    });
    return createSubscriptionSubject.asObservable();
  }

  private createSubscriptionContext(widgetContext: WidgetContext): WidgetSubscriptionContext {
    const subscriptionContext = new WidgetSubscriptionContext({
      onResetTimewindow: () => {},
      onUpdateTimewindow: (_startTimeMs, _endTimeMs, _interval, _persist) => {}
    } as IDashboardComponent);
    subscriptionContext.dashboardUtils = this.dashboardUtils;
    subscriptionContext.entityDataService = this.entityDataService;
    subscriptionContext.timeService = this.timeService;
    subscriptionContext.raf = this.raf;
    subscriptionContext.utils = this.utils;
    subscriptionContext.translate = this.translate;
    subscriptionContext.widgetUtils = widgetContext.utils;
    return subscriptionContext;
  }

}
