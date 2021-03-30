///
/// Copyright © 2016-2021 The Thingsboard Authors
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

import { Component, ElementRef, Input, OnDestroy, OnInit, ViewChild } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { WidgetContext } from '@home/models/widget-component.models';
import { UtilsService } from '@core/services/utils.service';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { isDefined } from '@core/utils';
import { IWidgetSubscription, SubscriptionInfo, WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { DatasourceType, widgetType } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { MatSlideToggle } from '@angular/material/slide-toggle';
import { ResizeObserver } from '@juggle/resize-observer';

const switchAspectRation = 2.7893;

type RetrieveValueMethod = 'rpc' | 'attribute' | 'timeseries';

interface SwitchSettings {
  initialValue: boolean;
  title: string;
  showOnOffLabels: boolean;
  retrieveValueMethod: RetrieveValueMethod;
  valueKey: string;
  getValueMethod: string;
  setValueMethod: string;
  parseValueFunction: string;
  convertValueFunction: string;
  requestTimeout: number;
}

@Component({
  selector: 'tb-switch',
  templateUrl: './switch.component.html',
  styleUrls: ['./switch.component.scss']
})
export class SwitchComponent extends PageComponent implements OnInit, OnDestroy {

  @ViewChild('switch', {static: true}) switchElementRef: ElementRef<HTMLElement>;
  @ViewChild('switchContainer', {static: true}) switchContainerRef: ElementRef<HTMLElement>;
  @ViewChild('matSlideToggle', {static: true}) matSlideToggleRef: MatSlideToggle;
  @ViewChild('onoffContainer', {static: true}) onoffContainerRef: ElementRef<HTMLElement>;
  @ViewChild('onLabel', {static: true}) onLabelRef: ElementRef<HTMLElement>;
  @ViewChild('offLabel', {static: true}) offLabelRef: ElementRef<HTMLElement>;
  @ViewChild('switchTitleContainer', {static: true}) switchTitleContainerRef: ElementRef<HTMLElement>;
  @ViewChild('switchTitle', {static: true}) switchTitleRef: ElementRef<HTMLElement>;
  @ViewChild('textMeasure', {static: true}) textMeasureRef: ElementRef<HTMLElement>;
  @ViewChild('switchErrorContainer', {static: true}) switchErrorContainerRef: ElementRef<HTMLElement>;
  @ViewChild('switchError', {static: true}) switchErrorRef: ElementRef<HTMLElement>;

  @Input()
  ctx: WidgetContext;

  showTitle = false;
  value = false;
  error = '';
  title = '';
  showOnOffLabels = false;

  private isSimulated: boolean;
  private requestTimeout: number;
  private retrieveValueMethod: RetrieveValueMethod;
  private valueKey: string;
  private parseValueFunction: (data: any) => boolean;
  private convertValueFunction: (value: any) => any;
  private getValueMethod: string;
  private setValueMethod: string;

  private valueSubscription: IWidgetSubscription;

  private executingUpdateValue: boolean;
  private scheduledValue: boolean;
  private rpcValue: boolean;

  private switchElement: JQuery<HTMLElement>;
  private switchContainer: JQuery<HTMLElement>;
  private matSlideToggle: JQuery<HTMLElement>;
  private onoffContainer: JQuery<HTMLElement>;
  private onLabel: JQuery<HTMLElement>;
  private offLabel: JQuery<HTMLElement>;
  private switchTitleContainer: JQuery<HTMLElement>;
  private switchTitle: JQuery<HTMLElement>;
  private textMeasure: JQuery<HTMLElement>;
  private switchErrorContainer: JQuery<HTMLElement>;
  private switchError: JQuery<HTMLElement>;

  private switchResize$: ResizeObserver;

  constructor(private utils: UtilsService,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    this.switchElement = $(this.switchElementRef.nativeElement);
    this.switchContainer = $(this.switchContainerRef.nativeElement);
    this.matSlideToggle = $(this.matSlideToggleRef._elementRef.nativeElement);
    this.onoffContainer = $(this.onoffContainerRef.nativeElement);
    this.onLabel = $(this.onLabelRef.nativeElement);
    this.offLabel = $(this.offLabelRef.nativeElement);
    this.switchTitleContainer = $(this.switchTitleContainerRef.nativeElement);
    this.switchTitle = $(this.switchTitleRef.nativeElement);
    this.textMeasure = $(this.textMeasureRef.nativeElement);
    this.switchErrorContainer = $(this.switchErrorContainerRef.nativeElement);
    this.switchError = $(this.switchErrorRef.nativeElement);

    this.switchResize$ = new ResizeObserver(() => {
      this.resize();
    })
    this.switchResize$.observe(this.switchContainerRef.nativeElement);
    this.init();
  }

  ngOnDestroy(): void {
    if (this.valueSubscription) {
      this.ctx.subscriptionApi.removeSubscription(this.valueSubscription.id);
    }
    if (this.switchResize$) {
      this.switchResize$.disconnect();
    }
  }

  private init() {
    const settings: SwitchSettings = this.ctx.settings;
    this.title = isDefined(settings.title) ? settings.title : '';
    this.showTitle = !!(this.title && this.title.length);
    this.showOnOffLabels = isDefined(settings.showOnOffLabels) ? settings.showOnOffLabels : true;
    const initialValue = isDefined(settings.initialValue) ? settings.initialValue : false;
    this.setValue(initialValue);

    const subscription = this.ctx.defaultSubscription;
    const rpcEnabled = subscription.rpcEnabled;

    this.isSimulated = this.utils.widgetEditMode;

    this.requestTimeout = 500;
    if (settings.requestTimeout) {
      this.requestTimeout = settings.requestTimeout;
    }
    this.retrieveValueMethod = 'rpc';
    if (settings.retrieveValueMethod && settings.retrieveValueMethod.length) {
      this.retrieveValueMethod = settings.retrieveValueMethod;
    }
    this.valueKey = 'value';
    if (settings.valueKey && settings.valueKey.length) {
      this.valueKey = settings.valueKey;
    }
    this.parseValueFunction = (data) => !!data;
    if (settings.parseValueFunction && settings.parseValueFunction.length) {
      try {
        this.parseValueFunction = new Function('data', settings.parseValueFunction) as (data: any) => boolean;
      } catch (e) {
        this.parseValueFunction = (data) => !!data;
      }
    }
    this.convertValueFunction = (value) => value;
    if (settings.convertValueFunction && settings.convertValueFunction.length) {
      try {
        this.convertValueFunction = new Function('value', settings.convertValueFunction) as (value: any) => any;
      } catch (e) {
        this.convertValueFunction = (value) => value;
      }
    }
    this.getValueMethod = 'getValue';
    if (settings.getValueMethod && settings.getValueMethod.length) {
      this.getValueMethod = settings.getValueMethod;
    }
    this.setValueMethod = 'setValue';
    if (settings.setValueMethod && settings.setValueMethod.length) {
      this.setValueMethod = settings.setValueMethod;
    }
    if (!rpcEnabled) {
      this.onError('Target device is not set!');
    } else {
      if (!this.isSimulated) {
        if (this.retrieveValueMethod === 'rpc') {
          this.rpcRequestValue();
        } else if (this.retrieveValueMethod === 'attribute' || this.retrieveValueMethod === 'timeseries') {
          this.subscribeForValue();
        }
      }
    }

  }

  private resize() {
    let width = this.switchContainer.width();
    let height = this.switchContainer.height();
    if (this.showOnOffLabels) {
      height = height*2/3;
    }
    const ratio = width/height;
    if (ratio > switchAspectRation) {
      width = height*switchAspectRation;
    } else {
      height = width/switchAspectRation;
    }
    this.switchElement.css({width, height});
    this.matSlideToggle.css({width, height, minWidth: width});

    if (this.showTitle) {
      this.setFontSize(this.switchTitle, this.title, this.switchTitleContainer.height() * 2 / 3, this.switchTitleContainer.width());
    }

    if (this.showOnOffLabels) {
      this.onoffContainer.css({width, height: this.switchContainer.height() / 3});
      this.setFontSize(this.onLabel, 'OFF', this.onoffContainer.height(), this.onoffContainer.width() / 2);
      this.setFontSize(this.offLabel, 'OFF', this.onoffContainer.height(), this.onoffContainer.width() / 2);
    }

    this.setFontSize(this.switchError, this.error, this.switchErrorContainer.height(), this.switchErrorContainer.width());
  }

  private setFontSize(element: JQuery<HTMLElement>, text: string, fontSize: number, maxWidth: number) {
    let textWidth = this.measureTextWidth(text, fontSize);
    while (textWidth > maxWidth) {
      fontSize--;
      textWidth = this.measureTextWidth(text, fontSize);
    }
    element.css({fontSize: fontSize+'px', lineHeight: fontSize+'px'});
  }

  private measureTextWidth(text: string, fontSize: number): number {
    this.textMeasure.css({fontSize: fontSize+'px', lineHeight: fontSize+'px'});
    this.textMeasure.text(text);
    return this.textMeasure.width();
  }

  private onError(error: string) {
    this.error = error;
    this.setFontSize(this.switchError, this.error, this.switchErrorContainer.height(), this.switchErrorContainer.width());
    this.ctx.detectChanges();
  }

  private setValue(value: boolean) {
    this.value = value ? true : false;
  }

  public onValue() {
    this.rpcUpdateValue(this.value);
  }

  private rpcRequestValue() {
    this.error = '';
    this.ctx.controlApi.sendTwoWayCommand(this.getValueMethod, null, this.requestTimeout).subscribe(
      (responseBody) => {
        this.setValue(this.parseValueFunction(responseBody));
      },
      () => {
        const errorText = this.ctx.defaultSubscription.rpcErrorText;
        this.onError(errorText);
      }
    );
  }

  private rpcUpdateValue(value) {
    if (this.executingUpdateValue) {
      this.scheduledValue = value;
      return;
    } else {
      this.scheduledValue = null;
      this.rpcValue = value;
      this.executingUpdateValue = true;
    }
    this.error = '';
    this.ctx.controlApi.sendOneWayCommand(this.setValueMethod, this.convertValueFunction(value), this.requestTimeout).subscribe(
      () => {
        this.executingUpdateValue = false;
        if (this.scheduledValue != null && this.scheduledValue !== this.rpcValue) {
          this.rpcUpdateValue(this.scheduledValue);
        }
      },
      () => {
        this.executingUpdateValue = false;
        const errorText = this.ctx.defaultSubscription.rpcErrorText;
        this.onError(errorText);
      }
    );
  }

  private subscribeForValue() {
    const valueSubscriptionInfo: SubscriptionInfo[] = [{
      type: DatasourceType.entity,
      entityType: EntityType.DEVICE,
      entityId: this.ctx.defaultSubscription.targetDeviceId
    }];
    if (this.retrieveValueMethod === 'attribute') {
      valueSubscriptionInfo[0].attributes = [
        {name: this.valueKey}
      ];
    } else {
      valueSubscriptionInfo[0].timeseries = [
        {name: this.valueKey}
      ];
    }
    const subscriptionOptions: WidgetSubscriptionOptions = {
      callbacks: {
        onDataUpdated: (subscription, detectChanges) => this.ctx.ngZone.run(() => {
            this.onDataUpdated(subscription, detectChanges);
        }),
        onDataUpdateError: (subscription, e) => this.ctx.ngZone.run(() => {
          this.onDataUpdateError(subscription, e);
        })
      }
    };
    this.ctx.subscriptionApi.createSubscriptionFromInfo (
      widgetType.latest, valueSubscriptionInfo, subscriptionOptions, false, true).subscribe(
      (subscription) => {
        this.valueSubscription = subscription;
      }
    );
  }

  private onDataUpdated(subscription: IWidgetSubscription, detectChanges: boolean) {
    let value = false;
    const data = subscription.data;
    if (data.length) {
      const keyData = data[0];
      if (keyData && keyData.data && keyData.data[0]) {
        const attrValue = keyData.data[0][1];
        if (isDefined(attrValue)) {
          let parsed = null;
          try {
            parsed = this.parseValueFunction(JSON.parse(attrValue));
          } catch (e){/**/}
          value = !!parsed;
        }
      }
    }
    this.setValue(value);
    if (detectChanges) {
      this.ctx.detectChanges();
    }
  }

  private onDataUpdateError(subscription: IWidgetSubscription, e: any) {
    const exceptionData = this.utils.parseException(e);
    let errorText = exceptionData.name;
    if (exceptionData.message) {
      errorText += ': ' + exceptionData.message;
    }
    this.onError(errorText);
  }
}
