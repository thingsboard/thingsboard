///
/// Copyright © 2016-2024 The Thingsboard Authors
///
/// Licensed under the Apache License, Version 2.0 (the "License");
/// you may not use this file except in compliance with the License.
/// You may obtain a copy of the License at
///
///     http://www.apache.org/licenses/LICENSE-2.0
///
/// Unless required by applicable law or agreed to in writing, software
/// distributed under the License is distributed on an "AS IS" BASIS,
/// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
/// See the License for the specific language governing permissions and
/// limitations under the License.
///

import {
  AttributeData,
  AttributeScope,
  LatestTelemetry,
  TelemetrySubscriber,
  TelemetryType,
  telemetryTypeTranslationsShort
} from '@shared/models/telemetry/telemetry.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { BehaviorSubject, forkJoin, Observable, Observer, of, Subscription, throwError } from 'rxjs';
import { catchError, delay, map, share, take } from 'rxjs/operators';
import { UtilsService } from '@core/services/utils.service';
import { AfterViewInit, ChangeDetectorRef, Directive, Input, OnDestroy, OnInit, TemplateRef } from '@angular/core';
import {
  DataToValueSettings,
  DataToValueType,
  GetAttributeValueSettings,
  GetValueAction,
  GetValueSettings,
  RpcSettings,
  SetAttributeValueSettings,
  SetValueAction,
  SetValueSettings,
  TelemetryValueSettings,
  ValueActionSettings,
  ValueToDataSettings,
  ValueToDataType
} from '@shared/models/action-widget-settings.models';
import { ValueType } from '@shared/models/constants';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';
import { EntityId } from '@shared/models/id/entity-id';
import { isDefinedAndNotNull } from '@core/utils';

@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export abstract class BasicActionWidgetComponent implements OnInit, OnDestroy, AfterViewInit {

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  private loadingSubject = new BehaviorSubject(false);
  private valueGetters: ValueGetter<any>[] = [];
  private valueActions: ValueAction[] = [];

  loading$ = this.loadingSubject.asObservable().pipe(share());

  protected constructor(protected cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.actionWidget = this;
  }

  ngAfterViewInit(): void {
    const getValueObservables: Array<Observable<any>> = [];
    this.valueGetters.forEach(valueGetter => {
      getValueObservables.push(valueGetter.getValue());
    });
    this.loadingSubject.next(true);
    forkJoin(getValueObservables).subscribe(
      {
        next: () => {
          this.loadingSubject.next(false);
        },
        error: () => {
          this.loadingSubject.next(false);
        }
      }
    );
  }

  ngOnDestroy() {
    this.valueActions.forEach(v => v.destroy());
    this.loadingSubject.complete();
    this.loadingSubject.unsubscribe();
  }

  public onInit() {
  }

  public clearError() {
    this.ctx.hideToast(this.ctx.toastTargetId);
  }

  protected createValueGetter<V>(getValueSettings: GetValueSettings<V>,
                                 valueType: ValueType,
                                 valueObserver?: Partial<Observer<V>>): ValueGetter<V> {
    const observer: Partial<Observer<V>> = {
      next: (value: V) => {
        if (valueObserver?.next) {
          valueObserver.next(value);
        }
      },
      error: (err: any) => {
        const message = parseError(this.ctx, err);
        this.onError(message);
        if (valueObserver?.error) {
          valueObserver.error(err);
        }
      }
    };
    const simulated = this.ctx.utilsService.widgetEditMode || this.ctx.isPreview;
    const valueGetter = ValueGetter.fromSettings(this.ctx, getValueSettings, valueType, observer, simulated);
    this.valueGetters.push(valueGetter);
    this.valueActions.push(valueGetter);
    return valueGetter;
  }

  protected createValueSetter<V>(setValueSettings: SetValueSettings): ValueSetter<V> {
    const simulated = this.ctx.utilsService.widgetEditMode || this.ctx.isPreview;
    const valueSetter = ValueSetter.fromSettings<V>(this.ctx, setValueSettings, simulated);
    this.valueActions.push(valueSetter);
    return valueSetter;
  }

  private onError(error: string) {
    this.ctx.showErrorToast(error, 'bottom', 'center', this.ctx.toastTargetId, true);
  }

  protected updateValue<V>(valueSetter: ValueSetter<V>,
                           value: V,
                           setValueObserver?: Partial<Observer<void>>): void {
    this.clearError();
    this.loadingSubject.next(true);
    valueSetter.setValue(value).subscribe({
      next: () => {
        if (setValueObserver?.next) {
          setValueObserver.next();
        }
        this.loadingSubject.next(false);
      },
      error: (err) => {
        this.loadingSubject.next(false);
        if (setValueObserver?.error) {
          setValueObserver.error(err);
        }
        const message = parseError(this.ctx, err);
        this.onError(message);
      }
    });
  }
}

type DataToValueFunction<V> = (data: any) => V;

export class DataToValueConverter<V> {

  private readonly dataToValueFunction: DataToValueFunction<V>;
  private readonly compareToValue: any;

  constructor(private settings: DataToValueSettings,
              private valueType: ValueType) {
    this.compareToValue = settings.compareToValue;
    switch (settings.type) {
      case DataToValueType.FUNCTION:
        try {
          this.dataToValueFunction = new Function('data', settings.dataToValueFunction) as DataToValueFunction<V>;
        } catch (e) {
          this.dataToValueFunction = (data) => data;
        }
        break;
      case DataToValueType.NONE:
        break;
    }
  }

  dataToValue(data: any): V {
    let result: V;
    switch (this.settings.type) {
      case DataToValueType.FUNCTION:
        result = data;
        try {
          let input = data;
          if (!!data) {
            try {
              input = JSON.parse(data);
            } catch (_e) {}
          }
          result = this.dataToValueFunction(input);
        } catch (_e) {}
        break;
      case DataToValueType.NONE:
        result = data;
        break;
    }
    if (this.valueType === ValueType.BOOLEAN) {
      result = (result === this.compareToValue) as any;
    }
    return result;
  }
}

export abstract class ValueAction {

  protected constructor(protected ctx: WidgetContext,
                        protected settings: ValueActionSettings) {}

  protected handleError(err: any): Error {
    const reason = parseError(this.ctx, err);
    let errorMessage = this.ctx.translate.instant('widgets.value-action.error.failed-to-perform-action',
      {actionLabel: this.settings.actionLabel});
    if (reason) {
      errorMessage += '<br>' + reason;
    }
    return new Error(errorMessage);
  }

  destroy(): void {}
}

export abstract class ValueGetter<V> extends ValueAction {

  static fromSettings<V>(ctx: WidgetContext,
                         settings: GetValueSettings<V>,
                         valueType: ValueType,
                         valueObserver: Partial<Observer<V>>,
                         simulated: boolean): ValueGetter<V> {
    switch (settings.action) {
      case GetValueAction.DO_NOTHING:
        return new DefaultValueGetter<V>(ctx, settings, valueType, valueObserver, simulated);
      case GetValueAction.EXECUTE_RPC:
        return new ExecuteRpcValueGetter<V>(ctx, settings, valueType, valueObserver, simulated);
      case GetValueAction.GET_ATTRIBUTE:
        return new AttributeValueGetter<V>(ctx, settings, valueType, valueObserver, simulated);
      case GetValueAction.GET_TIME_SERIES:
        return new TimeSeriesValueGetter<V>(ctx, settings, valueType, valueObserver, simulated);
      case GetValueAction.GET_DASHBOARD_STATE:
        return new DashboardStateGetter<V>(ctx, settings, valueType, valueObserver, simulated);
    }
  }

  private readonly dataConverter: DataToValueConverter<V>;

  private getValueSubscription: Subscription;

  protected constructor(protected ctx: WidgetContext,
                        protected settings: GetValueSettings<V>,
                        protected valueType: ValueType,
                        protected valueObserver: Partial<Observer<V>>,
                        protected simulated: boolean) {
    super(ctx, settings);
    if (this.settings.action !== GetValueAction.DO_NOTHING) {
      this.dataConverter = new DataToValueConverter<V>(settings.dataToValue, valueType);
    }
  }

  getValue(): Observable<V> {
    const valueObservable: Observable<V> = this.doGetValue().pipe(
      map((data) => {
        if (this.dataConverter) {
          return this.dataConverter.dataToValue(data);
        } else {
          return data;
        }
      }),
      catchError(err => {
        throw this.handleError(err);
      })
    );
    if (this.getValueSubscription) {
      this.getValueSubscription.unsubscribe();
    }
    this.getValueSubscription = valueObservable.subscribe({
      next: (value) => {
        this.valueObserver.next(value);
      },
      error: (err) => {
        this.valueObserver.error(err);
      }
    });
    return valueObservable.pipe(
      take(1)
    );
  }

  destroy() {
    if (this.getValueSubscription) {
      this.getValueSubscription.unsubscribe();
    }
    super.destroy();
  }

  protected abstract doGetValue(): Observable<any>;
}

type ValueToDataFunction<V> = (value: V) => any;

export class ValueToDataConverter<V> {

  private readonly constantValue: any;
  private readonly valueToDataFunction: ValueToDataFunction<V>;

  constructor(protected settings: ValueToDataSettings) {
    switch (settings.type) {
      case ValueToDataType.VALUE:
        break;
      case ValueToDataType.CONSTANT:
        this.constantValue = this.settings.constantValue;
        break;
      case ValueToDataType.FUNCTION:
        try {
          this.valueToDataFunction = new Function('value', settings.valueToDataFunction) as ValueToDataFunction<V>;
        } catch (e) {
          this.valueToDataFunction = (data) => data;
        }
        break;
      case ValueToDataType.NONE:
        break;
    }
  }

  valueToData(value: V): any {
    switch (this.settings.type) {
      case ValueToDataType.VALUE:
        return value;
      case ValueToDataType.CONSTANT:
        return this.constantValue;
      case ValueToDataType.FUNCTION:
        let result = value;
        try {
          result = this.valueToDataFunction(value);
        } catch (e) {}
        return result;
      case ValueToDataType.NONE:
        return null;
    }
  }
}

export abstract class ValueSetter<V> extends ValueAction {

  static fromSettings<V>(ctx: WidgetContext,
                         settings: SetValueSettings,
                         simulated: boolean): ValueSetter<V> {
    switch (settings.action) {
      case SetValueAction.EXECUTE_RPC:
        return new ExecuteRpcValueSetter<V>(ctx, settings, simulated);
      case SetValueAction.SET_ATTRIBUTE:
        return new AttributeValueSetter<V>(ctx, settings, simulated);
      case SetValueAction.ADD_TIME_SERIES:
        return new TimeSeriesValueSetter<V>(ctx, settings, simulated);
    }
  }

  private readonly valueToDataConverter: ValueToDataConverter<V>;

  protected constructor(protected ctx: WidgetContext,
                        protected settings: SetValueSettings,
                        protected simulated: boolean) {
    super(ctx, settings);
    this.valueToDataConverter = new ValueToDataConverter<V>(settings.valueToData);
  }

  setValue(value: V): Observable<any> {
    if (this.simulated) {
      return of(null).pipe(delay(500));
    } else {
      return this.doSetValue(this.valueToDataConverter.valueToData(value)).pipe(
        catchError(err => {
          throw this.handleError(err);
        })
      );
    }
  }

  protected abstract doSetValue(data: any): Observable<any>;
}

export class DefaultValueGetter<V> extends ValueGetter<V> {

  private readonly defaultValue: V;

  constructor(protected ctx: WidgetContext,
              protected settings: GetValueSettings<V>,
              protected valueType: ValueType,
              protected valueObserver: Partial<Observer<V>>,
              protected simulated: boolean) {
    super(ctx, settings, valueType, valueObserver, simulated);
    this.defaultValue = settings.defaultValue;
  }

  protected doGetValue(): Observable<V> {
    return of(this.defaultValue);
  }
}

export class ExecuteRpcValueGetter<V> extends ValueGetter<V> {

  private readonly executeRpcSettings: RpcSettings;

  constructor(protected ctx: WidgetContext,
              protected settings: GetValueSettings<V>,
              protected valueType: ValueType,
              protected valueObserver: Partial<Observer<V>>,
              protected simulated: boolean) {
    super(ctx, settings, valueType, valueObserver, simulated);
    this.executeRpcSettings = settings.executeRpc;
  }

  protected doGetValue(): Observable<V> {
    if (this.simulated) {
      const defaultValue = isDefinedAndNotNull(this.settings.defaultValue) ? this.settings.defaultValue : null;
      return of(defaultValue).pipe(delay(500));
    } else {
      return this.ctx.controlApi.sendTwoWayCommand(this.executeRpcSettings.method, null,
        this.executeRpcSettings.requestTimeout,
        this.executeRpcSettings.requestPersistent,
        this.executeRpcSettings.persistentPollingInterval).pipe(
        catchError((err) => {
          throw handleRpcError(this.ctx, err);
        })
      );
    }
  }
}

export abstract class TelemetryValueGetter<V, S extends TelemetryValueSettings> extends ValueGetter<V> {

  protected targetEntityId: EntityId;
  private telemetrySubscriber: TelemetrySubscriber;

  protected constructor(protected ctx: WidgetContext,
                        protected settings: GetValueSettings<V>,
                        protected valueType: ValueType,
                        protected valueObserver: Partial<Observer<V>>,
                        protected simulated: boolean) {
    super(ctx, settings, valueType, valueObserver, simulated);
    const entityInfo = this.ctx.defaultSubscription.getFirstEntityInfo();
    this.targetEntityId = entityInfo?.entityId;
  }

  protected doGetValue(): Observable<V> {
    if (this.simulated) {
      const defaultValue = isDefinedAndNotNull(this.settings.defaultValue) ? this.settings.defaultValue : null;
      return of(defaultValue).pipe(delay(100));
    } else {
      if (!this.targetEntityId && !this.ctx.defaultSubscription.rpcEnabled) {
        return throwError(() => new Error(this.ctx.translate.instant('widgets.value-action.error.target-entity-is-not-set')));
      }
      if (this.targetEntityId) {
        const err = validateAttributeScope(this.ctx, this.targetEntityId, this.scope());
        if (err) {
          return throwError(() => err);
        }
        return this.subscribeForTelemetryValue();
      } else {
        return of(null);
      }
    }
  }

  private subscribeForTelemetryValue(): Observable<V> {
    this.telemetrySubscriber =
      TelemetrySubscriber.createEntityAttributesSubscription(this.ctx.telemetryWsService, this.targetEntityId,
        this.scope(), this.ctx.ngZone, [this.getTelemetryValueSettings().key]);
    this.telemetrySubscriber.subscribe();
    return this.telemetrySubscriber.attributeData$().pipe(
      map((data) => {
        let value: V = null;
        const entry = data.find(attr => attr.key === this.getTelemetryValueSettings().key);
        if (entry) {
          value = entry.value;
          try {
            value = JSON.parse(entry.value);
          } catch (_e) {}
        }
        return value;
      })
    );
  }

  protected scope(): TelemetryType {
    return LatestTelemetry.LATEST_TELEMETRY;
  }

  protected abstract getTelemetryValueSettings(): S;

  destroy() {
    if (this.telemetrySubscriber) {
      this.telemetrySubscriber.unsubscribe();
      this.telemetrySubscriber = null;
    }
    super.destroy();
  }
}

export class AttributeValueGetter<V> extends TelemetryValueGetter<V, GetAttributeValueSettings> {

  constructor(protected ctx: WidgetContext,
              protected settings: GetValueSettings<V>,
              protected valueType: ValueType,
              protected valueObserver: Partial<Observer<V>>,
              protected simulated: boolean) {
    super(ctx, settings, valueType, valueObserver, simulated);
  }

  protected getTelemetryValueSettings(): GetAttributeValueSettings {
    return this.settings.getAttribute;
  }

  protected scope(): TelemetryType {
    return this.getTelemetryValueSettings().scope;
  }

}

export class TimeSeriesValueGetter<V> extends TelemetryValueGetter<V, TelemetryValueSettings> {

  constructor(protected ctx: WidgetContext,
              protected settings: GetValueSettings<V>,
              protected valueType: ValueType,
              protected valueObserver: Partial<Observer<V>>,
              protected simulated: boolean) {
    super(ctx, settings, valueType, valueObserver, simulated);
  }

  protected getTelemetryValueSettings(): TelemetryValueSettings {
    return this.settings.getTimeSeries;
  }
}

export class DashboardStateGetter<V> extends ValueGetter<V> {
  constructor(protected ctx: WidgetContext,
              protected settings: GetValueSettings<V>,
              protected valueType: ValueType,
              protected valueObserver: Partial<Observer<V>>,
              protected simulated: boolean) {
    super(ctx, settings, valueType, valueObserver, simulated);
  }

  protected doGetValue(): Observable<string> {
    if (this.simulated) {
      return of('default');
    } else {
      return this.ctx.stateController.dashboardCtrl.dashboardCtx.stateId;
    }
  }
}

export class ExecuteRpcValueSetter<V> extends ValueSetter<V> {

  private readonly executeRpcSettings: RpcSettings;

  constructor(protected ctx: WidgetContext,
              protected settings: SetValueSettings,
              protected simulated: boolean) {
    super(ctx, settings, simulated);
    this.executeRpcSettings = settings.executeRpc;
  }

  protected doSetValue(data: any): Observable<any> {
    return this.ctx.controlApi.sendOneWayCommand(this.executeRpcSettings.method, data,
      this.executeRpcSettings.requestTimeout,
      this.executeRpcSettings.requestPersistent,
      this.executeRpcSettings.persistentPollingInterval).pipe(
      catchError((err) => {
        throw handleRpcError(this.ctx, err);
      })
    );
  }
}

export abstract class TelemetryValueSetter<V> extends ValueSetter<V> {

  protected targetEntityId: EntityId;

  protected constructor(protected ctx: WidgetContext,
                        protected settings: SetValueSettings,
                        protected simulated: boolean) {
    super(ctx, settings, simulated);
    const entityInfo = this.ctx.defaultSubscription.getFirstEntityInfo();
    this.targetEntityId = entityInfo?.entityId;
  }

  protected doSetValue(data: any): Observable<V> {
    if (!this.targetEntityId && !this.ctx.defaultSubscription.rpcEnabled) {
      return throwError(() => new Error(this.ctx.translate.instant('widgets.value-action.error.target-entity-is-not-set')));
    }
    if (this.targetEntityId) {
      const err = validateAttributeScope(this.ctx, this.targetEntityId, this.scope());
      if (err) {
        return throwError(() => err);
      }
      return this.doSetTelemetryValue(data);
    } else {
      return of(null);
    }
  }

  protected scope(): TelemetryType {
    return LatestTelemetry.LATEST_TELEMETRY;
  }

  protected abstract doSetTelemetryValue(data: any): Observable<V>;

}

export class AttributeValueSetter<V> extends TelemetryValueSetter<V> {

  private readonly setAttributeValueSettings:  SetAttributeValueSettings;

  constructor(protected ctx: WidgetContext,
              protected settings: SetValueSettings,
              protected simulated: boolean) {
    super(ctx, settings, simulated);
    this.setAttributeValueSettings = settings.setAttribute;
  }

  protected doSetTelemetryValue(data: any): Observable<any> {
      const attributes: Array<AttributeData> = [{key: this.setAttributeValueSettings.key, value: data}];
      return this.ctx.attributeService.saveEntityAttributes(this.targetEntityId,
        this.setAttributeValueSettings.scope, attributes, {ignoreLoading: true, ignoreErrors: true});
  }

  protected scope(): TelemetryType {
    return this.setAttributeValueSettings.scope;
  }

}

export class TimeSeriesValueSetter<V> extends TelemetryValueSetter<V> {

  private readonly putTimeSeriesValueSettings:  TelemetryValueSettings;

  constructor(protected ctx: WidgetContext,
              protected settings: SetValueSettings,
              protected simulated: boolean) {
    super(ctx, settings, simulated);
    this.putTimeSeriesValueSettings = settings.putTimeSeries;
  }

  protected doSetTelemetryValue(data: any): Observable<any> {
    const timeSeries: Array<AttributeData> = [{key: this.putTimeSeriesValueSettings.key, value: data}];
    return this.ctx.attributeService.saveEntityTimeseries(this.targetEntityId,
      LatestTelemetry.LATEST_TELEMETRY, timeSeries, {ignoreLoading: true, ignoreErrors: true});
  }

}

const parseError = (ctx: WidgetContext, err: any): string =>
  ctx.$injector.get(UtilsService).parseException(err).message || 'Unknown Error';

const handleRpcError = (ctx: WidgetContext, err: any): Error => {
  let reason: string;
  if (ctx.defaultSubscription.rpcErrorText) {
    reason = ctx.defaultSubscription.rpcErrorText;
  } else {
    reason = parseError(ctx, err);
  }
  return new Error(reason);
};

const validateAttributeScope = (ctx: WidgetContext, targetEntityId: EntityId, scope?: TelemetryType): Error | null => {
  if (targetEntityId.entityType !== EntityType.DEVICE && scope &&
    ![AttributeScope.SERVER_SCOPE, LatestTelemetry.LATEST_TELEMETRY].includes(scope)) {
    const scopeStr = ctx.translate.instant(telemetryTypeTranslationsShort.get(scope));
    const entityType =
      ctx.translate.instant(entityTypeTranslations.get(targetEntityId.entityType).type);
    const errorMessage =
      ctx.translate.instant('widgets.value-action.error.invalid-attribute-scope', {scope: scopeStr, entityType});
    return new Error(errorMessage);
  } else {
    return null;
  }
};
