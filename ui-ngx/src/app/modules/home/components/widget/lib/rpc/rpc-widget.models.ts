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
  telemetryTypeTranslationsShort
} from '@shared/models/telemetry/telemetry.models';
import { WidgetContext } from '@home/models/widget-component.models';
import { BehaviorSubject, Observable, of, throwError } from 'rxjs';
import { catchError, delay, map, share } from 'rxjs/operators';
import { UtilsService } from '@core/services/utils.service';
import { AfterViewInit, ChangeDetectorRef, Directive, Input, OnInit, TemplateRef } from '@angular/core';
import { backgroundStyle, ComponentStyle, overlayStyle } from '@shared/models/widget-settings.models';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import {
  RpcActionSettings,
  RpcGetAttributeSettings,
  RpcSetAttributeSettings,
  RpcSettings,
  RpcStateToParamsSettings,
  RpcStateToParamsType,
  RpcStateWidgetSettings,
  RpcTelemetrySettings,
  RpcUpdateStateAction
} from '@shared/models/rpc-widget-settings.models';
import {
  RpcDataToStateSettings,
  RpcDataToStateType,
  RpcInitialStateAction,
  RpcInitialStateSettings,
  RpcStateBehaviourSettings,
  RpcUpdateStateSettings
} from '@app/shared/models/rpc-widget-settings.models';
import { ValueType } from '@shared/models/constants';
import { EntityType, entityTypeTranslations } from '@shared/models/entity-type.models';

@Directive()
// eslint-disable-next-line @angular-eslint/directive-class-suffix
export abstract class BasicRpcStateWidgetComponent<V, S extends RpcStateWidgetSettings<V>> implements OnInit, AfterViewInit {

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  settings: S;

  behaviorApi: RpcStateBehaviorApi<V>;
  loading$: Observable<boolean>;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};

  value: V;

  error = '';

  protected constructor(protected imagePipe: ImagePipe,
                        protected sanitizer: DomSanitizer,
                        protected cd: ChangeDetectorRef) {
  }

  ngOnInit(): void {
    this.ctx.$scope.rpcWidget = this;
    this.settings = {...this.defaultSettings(), ...this.ctx.settings};
    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);

    const behaviourSettings: RpcStateBehaviourSettings<V> = {
      initialState: this.initialState(),
      updateStateByValue: val => this.getUpdateStateSettingsForValue(val)
    };

    const callbacks: RpcStateCallbacks<V> = {
      onStateValue: val => this.setValue(val),
      onError: err => this.onError(err),
      validateStateValue: val => this.validateValue(val)
    };

    this.behaviorApi = new RpcStateBehaviorApi<V>(this.defaultValue(), this.ctx,
      behaviourSettings, callbacks, this.stateValueType());
    this.loading$ = this.behaviorApi.loading$.pipe(share());
  }

  ngAfterViewInit(): void {
    this.behaviorApi.initState();
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  public clearError() {
    this.error = '';
  }

  public updateValue() {
    this.behaviorApi.updateState(this.value);
  }

  private onError(error: string) {
    this.error = error;
    this.ctx.detectChanges();
  }

  protected abstract defaultSettings(): S;

  protected abstract initialState(): RpcInitialStateSettings<V>;

  protected abstract getUpdateStateSettingsForValue(value: V): RpcUpdateStateSettings;

  protected stateValueType(): ValueType {
    return ValueType.BOOLEAN;
  }

  protected defaultValue(): V {
    return null;
  }

  protected setValue(value: V) {
    this.value = value;
  }

  protected validateValue(value: any): V {
    return value;
  }

}

export abstract class RpcHasLoading {

  public get loading$() {
    return this.loadingSubject.asObservable();
  }

  protected loadingSubject = new BehaviorSubject(false);
}

export interface RpcStateCallbacks<V> {
  onStateValue: (value: V) => void;
  validateStateValue: (value: any) => V;
  onError: (error: string) => void;
}

export class RpcStateBehaviorApi<V> extends RpcHasLoading {

  private readonly initialStateGetter: RpcInitialStateGetter<V>;
  private readonly stateUpdatersMap: Map<RpcUpdateStateSettings, RpcStateUpdater<V>>;

  constructor(private state: V,
              private ctx: WidgetContext,
              private settings: RpcStateBehaviourSettings<V>,
              private callbacks: RpcStateCallbacks<V>,
              stateValueType: ValueType) {
    super();
    this.initialStateGetter = RpcInitialStateGetter.fromSettings(ctx, settings.initialState, stateValueType, callbacks);
    this.stateUpdatersMap = new Map<RpcUpdateStateSettings, RpcStateUpdater<V>>();
  }

  initState() {
    if (this.ctx.defaultSubscription.targetEntityId || this.ctx.defaultSubscription.rpcEnabled) {
      this.loadingSubject.next(true);
      this.initialStateGetter.initState().subscribe(
        {
          next: (value) => {
            this.state = value;
            this.loadingSubject.next(false);
            this.callbacks.onStateValue(value);
            this.ctx.detectChanges();
          },
          error: (err: any) => {
            this.loadingSubject.next(false);
            const message = parseError(this.ctx, err);
            this.callbacks.onError(message);
          }
        }
      );
    } else {
      this.callbacks.onError(this.ctx.translate.instant('widgets.rpc-state.error.target-entity-is-not-set'));
    }
  }

  updateState(value: V) {
    this.callbacks.onError(null);
    let updater: RpcStateUpdater<V>;
    const updateStateSettings = this.settings.updateStateByValue(value);
    if (updateStateSettings) {
      updater = this.stateUpdatersMap.get(updateStateSettings);
      if (!updater) {
        updater = RpcStateUpdater.fromSettings(this.ctx, updateStateSettings);
        this.stateUpdatersMap.set(updateStateSettings, updater);
      }
    }
    if (updater) {
      this.loadingSubject.next(true);
      updater.updateState(value).subscribe(
        {
          next: () => {
            this.state = value;
            this.loadingSubject.next(false);
            this.ctx.detectChanges();
          },
          error: (err: any) => {
            this.loadingSubject.next(false);
            const message = parseError(this.ctx, err);
            this.callbacks.onStateValue(this.state);
            this.callbacks.onError(message);
            this.ctx.detectChanges();
          }
        });
    }
  }

}

type RpcDataToStateFunction<V> = (data: any) => V;

export class RpcDataToStateConverter<V> {

  private readonly dataToStateFunction: RpcDataToStateFunction<V>;
  private readonly compareToValue: any;

  constructor(private settings: RpcDataToStateSettings,
              private stateValueType: ValueType,
              private callbacks: RpcStateCallbacks<V>) {
    this.compareToValue = settings.compareToValue;
    switch (settings.type) {
      case RpcDataToStateType.FUNCTION:
        try {
          this.dataToStateFunction = new Function('data', settings.dataToStateFunction) as RpcDataToStateFunction<V>;
        } catch (e) {
          this.dataToStateFunction = (data) => data;
        }
        break;
      case RpcDataToStateType.NONE:
        break;
    }
  }

  dataToStateValue(data: any): V {
    let result: V;
    switch (this.settings.type) {
      case RpcDataToStateType.FUNCTION:
        result = data;
        try {
          result = this.dataToStateFunction(!!data ? JSON.parse(data) : data);
        } catch (e) {}
        break;
      case RpcDataToStateType.NONE:
        result = data;
        break;
    }
    if (this.stateValueType === ValueType.BOOLEAN) {
      result = (result === this.compareToValue) as any;
    }
    result = this.callbacks.validateStateValue(result);
    return result;
  }
}

export abstract class RpcAction {

  protected constructor(protected ctx: WidgetContext,
                        protected settings: RpcActionSettings) {}

  handleError(err: any): Error {
    const reason = parseError(this.ctx, err);
    let errorMessage = this.ctx.translate.instant('widgets.rpc-state.error.failed-to-perform-action',
      {actionLabel: this.settings.actionLabel});
    if (reason) {
      errorMessage += '<br>' + reason;
    }
    return new Error(errorMessage);
  }
}

export abstract class RpcInitialStateGetter<V> extends RpcAction {

  static fromSettings<V>(ctx: WidgetContext,
                         settings: RpcInitialStateSettings<V>,
                         stateValueType: ValueType,
                         callbacks: RpcStateCallbacks<V>): RpcInitialStateGetter<V> {
    switch (settings.action) {
      case RpcInitialStateAction.DO_NOTHING:
        return new RpcDefaultStateGetter<V>(ctx, settings, stateValueType, callbacks);
      case RpcInitialStateAction.EXECUTE_RPC:
        return new ExecuteRpcStateGetter<V>(ctx, settings, stateValueType, callbacks);
      case RpcInitialStateAction.GET_ATTRIBUTE:
        return new RpcAttributeStateGetter<V>(ctx, settings, stateValueType, callbacks);
      case RpcInitialStateAction.GET_TIME_SERIES:
        return new RpcTimeSeriesStateGetter<V>(ctx, settings, stateValueType, callbacks);
    }
  }

  private readonly isSimulated: boolean;
  private readonly dataConverter: RpcDataToStateConverter<V>;

  protected constructor(protected ctx: WidgetContext,
                        protected settings: RpcInitialStateSettings<V>,
                        protected stateValueType: ValueType,
                        protected callbacks: RpcStateCallbacks<V>) {
    super(ctx, settings);
    this.isSimulated = this.ctx.$injector.get(UtilsService).widgetEditMode;
    if (this.settings.action !== RpcInitialStateAction.DO_NOTHING) {
      this.dataConverter = new RpcDataToStateConverter<V>(settings.dataToState, stateValueType, this.callbacks);
    }
  }

  initState(): Observable<V> {
    const stateObservable: Observable<V> = this.isSimulated ? of(null).pipe(delay(500)) : this.doGetState();
    return stateObservable.pipe(
      map((data) => {
        if (this.dataConverter) {
          return this.dataConverter.dataToStateValue(data);
        } else {
          return data;
        }
      }),
      catchError(err => {
        throw this.handleError(err);
      })
    );
  }

  protected abstract doGetState(): Observable<any>;
}

type RpcStateToParamsFunction<V> = (state: V) => any;

export class RpcStateToParamsConverter<V> {

  private readonly constantValue: any;
  private readonly stateToParamsFunction: RpcStateToParamsFunction<V>;

  constructor(protected settings: RpcStateToParamsSettings) {
    switch (settings.type) {
      case RpcStateToParamsType.CONSTANT:
        this.constantValue = this.settings.constantValue;
        break;
      case RpcStateToParamsType.FUNCTION:
        try {
          this.stateToParamsFunction = new Function('value', settings.stateToParamsFunction) as RpcStateToParamsFunction<V>;
        } catch (e) {
          this.stateToParamsFunction = (data) => data;
        }
        break;
      case RpcStateToParamsType.NONE:
        break;
    }
  }

  stateToParams(state: V): any {
    switch (this.settings.type) {
      case RpcStateToParamsType.CONSTANT:
        return this.constantValue;
      case RpcStateToParamsType.FUNCTION:
        let result = state;
        try {
          result = this.stateToParamsFunction(state);
        } catch (e) {}
        return result;
      case RpcStateToParamsType.NONE:
        return null;
    }
  }
}

export abstract class RpcStateUpdater<V> extends RpcAction {

  static fromSettings<V>(ctx: WidgetContext,
                         settings: RpcUpdateStateSettings): RpcStateUpdater<V> {
    switch (settings.action) {
      case RpcUpdateStateAction.EXECUTE_RPC:
        return new ExecuteRpcStateUpdater<V>(ctx, settings);
      case RpcUpdateStateAction.SET_ATTRIBUTE:
        return new RpcAttributeStateUpdater<V>(ctx, settings);
      case RpcUpdateStateAction.ADD_TIME_SERIES:
        return new RpcTimeSeriesStateUpdater<V>(ctx, settings);
    }
  }

  private readonly isSimulated: boolean;
  private readonly paramsConverter: RpcStateToParamsConverter<V>;

  protected constructor(protected ctx: WidgetContext,
                        protected settings: RpcUpdateStateSettings) {
    super(ctx, settings);
    this.isSimulated = this.ctx.$injector.get(UtilsService).widgetEditMode;
    this.paramsConverter = new RpcStateToParamsConverter<V>(settings.stateToParams);
  }

  updateState(state: V): Observable<any> {
    if (this.isSimulated) {
      return of(null).pipe(delay(500));
    } else {
      return this.doUpdateState(this.paramsConverter.stateToParams(state)).pipe(
        catchError(err => {
          throw this.handleError(err);
        })
      );
    }
  }

  protected abstract doUpdateState(params: any): Observable<any>;
}

export class RpcDefaultStateGetter<V> extends RpcInitialStateGetter<V> {

  private readonly defaultValue: V;

  constructor(protected ctx: WidgetContext,
              protected settings: RpcInitialStateSettings<V>,
              protected stateValueType: ValueType,
              protected callbacks: RpcStateCallbacks<V>) {
    super(ctx, settings, stateValueType, callbacks);
    this.defaultValue = settings.defaultValue;
  }

  protected doGetState(): Observable<V> {
    return of(this.defaultValue);
  }
}

export class ExecuteRpcStateGetter<V> extends RpcInitialStateGetter<V> {

  private readonly executeRpcSettings: RpcSettings;

  constructor(protected ctx: WidgetContext,
              protected settings: RpcInitialStateSettings<V>,
              protected stateValueType: ValueType,
              protected callbacks: RpcStateCallbacks<V>) {
    super(ctx, settings, stateValueType, callbacks);
    this.executeRpcSettings = settings.executeRpc;
  }

  protected doGetState(): Observable<V> {
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

export class RpcAttributeStateGetter<V> extends RpcInitialStateGetter<V> {

  private readonly getAttributeSettings:  RpcGetAttributeSettings;

  constructor(protected ctx: WidgetContext,
              protected settings: RpcInitialStateSettings<V>,
              protected stateValueType: ValueType,
              protected callbacks: RpcStateCallbacks<V>) {
    super(ctx, settings, stateValueType, callbacks);
    this.getAttributeSettings = settings.getAttribute;
  }

  protected doGetState(): Observable<V> {
    if (this.ctx.defaultSubscription.targetEntityId) {
      const err = validateAttributeScope(this.ctx, this.getAttributeSettings.scope);
      if (err) {
        return throwError(() => err);
      }
      return this.ctx.attributeService.getEntityAttributes(this.ctx.defaultSubscription.targetEntityId,
        this.getAttributeSettings.scope, [this.getAttributeSettings.key], {ignoreLoading: true, ignoreErrors: true})
      .pipe(
        map((data) => data.find(attr => attr.key === this.getAttributeSettings.key)?.value)
      );
    } else {
      return of(null);
    }
  }

}

export class RpcTimeSeriesStateGetter<V> extends RpcInitialStateGetter<V> {

  private readonly getTimeSeriesSettings:  RpcTelemetrySettings;

  constructor(protected ctx: WidgetContext,
              protected settings: RpcInitialStateSettings<V>,
              protected stateValueType: ValueType,
              protected callbacks: RpcStateCallbacks<V>) {
    super(ctx, settings, stateValueType, callbacks);
    this.getTimeSeriesSettings = settings.getTimeSeries;
  }

  protected doGetState(): Observable<V> {
    if (this.ctx.defaultSubscription.targetEntityId) {
      return this.ctx.attributeService.getEntityTimeseriesLatest(this.ctx.defaultSubscription.targetEntityId,
        [this.getTimeSeriesSettings.key], true, {ignoreLoading: true, ignoreErrors: true})
      .pipe(
        map((data) => {
          let value: any = null;
          if (data[this.getTimeSeriesSettings.key]) {
            const dataSet = data[this.getTimeSeriesSettings.key];
            if (dataSet.length) {
              value = dataSet[0].value;
            }
          }
          return value;
        })
      );
    } else {
      return of(null);
    }
  }

}

export class ExecuteRpcStateUpdater<V> extends RpcStateUpdater<V> {

  private readonly executeRpcSettings: RpcSettings;

  constructor(protected ctx: WidgetContext,
              protected settings: RpcUpdateStateSettings) {
    super(ctx, settings);
    this.executeRpcSettings = settings.executeRpc;
  }

  protected doUpdateState(params: any): Observable<any> {
    return this.ctx.controlApi.sendOneWayCommand(this.executeRpcSettings.method, params,
      this.executeRpcSettings.requestTimeout,
      this.executeRpcSettings.requestPersistent,
      this.executeRpcSettings.persistentPollingInterval).pipe(
      catchError((err) => {
        throw handleRpcError(this.ctx, err);
      })
    );
  }
}

export class RpcAttributeStateUpdater<V> extends RpcStateUpdater<V> {

  private readonly setAttributeSettings:  RpcSetAttributeSettings;

  constructor(protected ctx: WidgetContext,
              protected settings: RpcUpdateStateSettings) {
    super(ctx, settings);
    this.setAttributeSettings = settings.setAttribute;
  }

  protected doUpdateState(params: any): Observable<any> {
    if (this.ctx.defaultSubscription.targetEntityId) {
      const err = validateAttributeScope(this.ctx, this.setAttributeSettings.scope);
      if (err) {
        return throwError(() => err);
      }
      const attributes: Array<AttributeData> = [{key: this.setAttributeSettings.key, value: params}];
      return this.ctx.attributeService.saveEntityAttributes(this.ctx.defaultSubscription.targetEntityId,
        this.setAttributeSettings.scope, attributes, {ignoreLoading: true, ignoreErrors: true});
    } else {
      return of(null);
    }
  }

}

export class RpcTimeSeriesStateUpdater<V> extends RpcStateUpdater<V> {

  private readonly putTimeSeriesSettings:  RpcTelemetrySettings;

  constructor(protected ctx: WidgetContext,
              protected settings: RpcUpdateStateSettings) {
    super(ctx, settings);
    this.putTimeSeriesSettings = settings.putTimeSeries;
  }

  protected doUpdateState(params: any): Observable<any> {
    if (this.ctx.defaultSubscription.targetEntityId) {
      const timeSeries: Array<AttributeData> = [{key: this.putTimeSeriesSettings.key, value: params}];
      return this.ctx.attributeService.saveEntityTimeseries(this.ctx.defaultSubscription.targetEntityId,
        LatestTelemetry.LATEST_TELEMETRY, timeSeries, {ignoreLoading: true, ignoreErrors: true});
    } else {
      return of(null);
    }
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

const validateAttributeScope = (ctx: WidgetContext, scope?: AttributeScope): Error | null => {
  if (ctx.defaultSubscription.targetEntityId.entityType !== EntityType.DEVICE && scope && scope !== AttributeScope.SERVER_SCOPE) {
    const scopeStr = ctx.translate.instant(telemetryTypeTranslationsShort.get(scope));
    const entityType =
      ctx.translate.instant(entityTypeTranslations.get(ctx.defaultSubscription.targetEntityId.entityType).type);
    const errorMessage =
      ctx.translate.instant('widgets.rpc-state.error.invalid-attribute-scope', {scope: scopeStr, entityType});
    return new Error(errorMessage);
  } else {
    return null;
  }
};
