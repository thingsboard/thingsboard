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
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { isDefined } from '@core/utils';
import * as tinycolor_ from 'tinycolor2';
import { UtilsService } from '@core/services/utils.service';
import { IWidgetSubscription, SubscriptionInfo, WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { DatasourceType, widgetType } from '@shared/models/widget.models';
import { EntityType } from '@shared/models/entity-type.models';
import { ResizeObserver } from '@juggle/resize-observer';
import Timeout = NodeJS.Timeout;

const tinycolor = tinycolor_;

const checkStatusPollingInterval = 10000;

type RetrieveValueMethod = 'attribute' | 'timeseries';

interface LedIndicatorSettings {
  initialValue: boolean;
  title: string;
  ledColor: string;
  performCheckStatus: boolean;
  checkStatusMethod: string;
  retrieveValueMethod: RetrieveValueMethod;
  valueAttribute: string;
  parseValueFunction: string;
  requestTimeout: number;
}

@Component({
  selector: 'tb-led-indicator',
  templateUrl: './led-indicator.component.html',
  styleUrls: ['./led-indicator.component.scss']
})
export class LedIndicatorComponent extends PageComponent implements OnInit, OnDestroy {

  @ViewChild('led', {static: true}) ledRef: ElementRef<HTMLElement>;
  @ViewChild('ledContainer', {static: true}) ledContainerRef: ElementRef<HTMLElement>;
  @ViewChild('textMeasure', {static: true}) textMeasureRef: ElementRef<HTMLElement>;
  @ViewChild('ledTitleContainer', {static: true}) ledTitleContainerRef: ElementRef<HTMLElement>;
  @ViewChild('ledTitle', {static: true}) ledTitleRef: ElementRef<HTMLElement>;
  @ViewChild('ledErrorContainer', {static: true}) ledErrorContainerRef: ElementRef<HTMLElement>;
  @ViewChild('ledError', {static: true}) ledErrorRef: ElementRef<HTMLElement>;

  @Input()
  ctx: WidgetContext;

  showTitle = false;
  value = false;
  error = '';
  title = '';

  private valueAttribute: string;
  private ledColor: string;
  private ledMiddleColor: string;
  private disabledColor: string;
  private disabledMiddleColor: string;

  private isSimulated: boolean;
  private requestTimeout: number;
  private retrieveValueMethod: RetrieveValueMethod;
  private parseValueFunction: (data: any) => boolean;
  private performCheckStatus: boolean;
  private checkStatusMethod: string;

  private destroyed = false;
  private checkStatusTimeoutHandle: Timeout;
  private subscription: IWidgetSubscription;

  private led: JQuery<HTMLElement>;
  private ledContainer: JQuery<HTMLElement>;
  private textMeasure: JQuery<HTMLElement>;
  private ledTitleContainer: JQuery<HTMLElement>;
  private ledTitle: JQuery<HTMLElement>;
  private ledErrorContainer: JQuery<HTMLElement>;
  private ledError: JQuery<HTMLElement>;

  private ledResize$: ResizeObserver;

  private subscriptionOptions: WidgetSubscriptionOptions = {
    callbacks: {
      onDataUpdated: (subscription, detectChanges) => this.ctx.ngZone.run(() => {
        this.onDataUpdated(subscription, detectChanges);
      }),
      onDataUpdateError: (subscription, e) => this.ctx.ngZone.run(() => {
        this.onDataUpdateError(subscription, e);
      }),
      dataLoading: () => {}
    }
  };


  constructor(private utils: UtilsService,
              protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    this.led = $(this.ledRef.nativeElement);
    this.ledContainer = $(this.ledContainerRef.nativeElement);
    this.textMeasure = $(this.textMeasureRef.nativeElement);
    this.ledTitleContainer = $(this.ledTitleContainerRef.nativeElement);
    this.ledTitle = $(this.ledTitleRef.nativeElement);
    this.ledErrorContainer = $(this.ledErrorContainerRef.nativeElement);
    this.ledError = $(this.ledErrorRef.nativeElement);

    this.ledResize$ = new ResizeObserver(() => {
      this.resize();
    });
    this.ledResize$.observe(this.ledContainerRef.nativeElement);
    this.init();
  }

  ngOnDestroy(): void {
    this.destroyed = true;
    if (this.checkStatusTimeoutHandle) {
      clearTimeout(this.checkStatusTimeoutHandle);
    }
    if (this.subscription) {
      this.ctx.subscriptionApi.removeSubscription(this.subscription.id);
    }
    if (this.ledResize$) {
      this.ledResize$.disconnect();
    }
  }

  private init() {
    const settings: LedIndicatorSettings = this.ctx.settings;
    this.title = isDefined(settings.title) ? settings.title : '';
    this.showTitle = !!(this.title && this.title.length);

    const origColor = isDefined(settings.ledColor) ? settings.ledColor : 'green';
    this.valueAttribute = isDefined(settings.valueAttribute) ? settings.valueAttribute : 'value';

    this.ledColor = tinycolor(origColor).brighten(30).toHexString();
    this.ledMiddleColor = tinycolor(origColor).toHexString();
    this.disabledColor = tinycolor(origColor).darken(40).toHexString();
    this.disabledMiddleColor = tinycolor(origColor).darken(60).toHexString();

    const initialValue = isDefined(settings.initialValue) ? settings.initialValue : false;
    this.setValue(initialValue, true);

    const subscription = this.ctx.defaultSubscription;
    const rpcEnabled = subscription.rpcEnabled;

    this.isSimulated = this.utils.widgetEditMode;

    this.requestTimeout = 500;
    if (settings.requestTimeout) {
      this.requestTimeout = settings.requestTimeout;
    }
    this.retrieveValueMethod = 'attribute';
    if (settings.retrieveValueMethod && settings.retrieveValueMethod.length) {
      this.retrieveValueMethod = settings.retrieveValueMethod;
    }
    this.parseValueFunction = (data) => !!data;
    if (settings.parseValueFunction && settings.parseValueFunction.length) {
      try {
        this.parseValueFunction = new Function('data', settings.parseValueFunction) as (data: any) => boolean;
      } catch (e) {
        this.parseValueFunction = (data) => !!data;
      }
    }
    this.performCheckStatus = settings.performCheckStatus !== false;
    if (this.performCheckStatus) {
      this.checkStatusMethod = 'checkStatus';
      if (settings.checkStatusMethod && settings.checkStatusMethod.length) {
        this.checkStatusMethod = settings.checkStatusMethod;
      }
    }
    if (!rpcEnabled) {
      this.onError('Target device is not set!');
    } else {
      if (!this.isSimulated) {
        if (this.performCheckStatus) {
          this.rpcCheckStatus();
        } else {
          this.subscribeForValue();
        }
      }
    }
  }

  private resize() {
    const width = this.ledContainer.width();
    const height = this.ledContainer.height();
    const size = Math.min(width, height);

    this.led.css({width: size, height: size});

    if (this.showTitle) {
      this.setFontSize(this.ledTitle, this.title, this.ledTitleContainer.height() * 2 / 3, this.ledTitleContainer.width());
    }
    this.setFontSize(this.ledError, this.error, this.ledErrorContainer.height(), this.ledErrorContainer.width());
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
    this.setFontSize(this.ledError, this.error, this.ledErrorContainer.height(), this.ledErrorContainer.width());
    this.ctx.detectChanges();
  }

  private setValue(value: boolean, forceUpdate?: boolean) {
    if (this.value !== value || forceUpdate) {
      this.value = value;
      this.updateColor();
    }
  }

  private updateColor() {
    const color = this.value ? this.ledColor : this.disabledColor;
    const middleColor = this.value ? this.ledMiddleColor : this.disabledMiddleColor;
    const boxShadow = `#000 0 -1px 6px 1px, inset ${middleColor} 0 -1px 8px, ${color} 0 3px 11px`;
    this.led.css({backgroundColor: color});
    this.led.css({boxShadow});
    if (this.value) {
      this.led.removeClass( 'disabled' );
    } else {
      this.led.addClass( 'disabled' );
    }
  }

  private rpcCheckStatus() {
    if (this.destroyed) {
      return;
    }
    this.error = '';
    this.ctx.controlApi.sendTwoWayCommand(this.checkStatusMethod, null, this.requestTimeout).subscribe(
      (responseBody) => {
        const status = !!responseBody;
        if (status) {
          if (this.checkStatusTimeoutHandle) {
            clearTimeout(this.checkStatusTimeoutHandle);
            this.checkStatusTimeoutHandle = null;
          }
          this.subscribeForValue();
        } else {
          const errorText = 'Unknown device status!';
          this.onError(errorText);
          if (this.checkStatusTimeoutHandle) {
            clearTimeout(this.checkStatusTimeoutHandle);
          }
          this.checkStatusTimeoutHandle = setTimeout(this.rpcCheckStatus.bind(this), checkStatusPollingInterval);
        }
      },
      () => {
        const errorText = this.ctx.defaultSubscription.rpcErrorText;
        this.onError(errorText);
        if (this.checkStatusTimeoutHandle) {
          clearTimeout(this.checkStatusTimeoutHandle);
        }
        this.checkStatusTimeoutHandle = setTimeout(this.rpcCheckStatus.bind(this), checkStatusPollingInterval);
      }
    );
  }

  private subscribeForValue() {
    const subscriptionsInfo: SubscriptionInfo[] = [{
      type: DatasourceType.entity,
      entityType: EntityType.DEVICE,
      entityId: this.ctx.defaultSubscription.targetDeviceId
    }];

    if (this.retrieveValueMethod === 'attribute') {
      subscriptionsInfo[0].attributes = [
        {name: this.valueAttribute}
      ];
    } else {
      subscriptionsInfo[0].timeseries = [
        {name: this.valueAttribute}
      ];
    }

    this.ctx.subscriptionApi.createSubscriptionFromInfo (
      widgetType.latest, subscriptionsInfo, this.subscriptionOptions, false, true).subscribe(
      (subscription) => {
        this.subscription = subscription;
      }
    );
  }

  private onDataUpdated (subscription: IWidgetSubscription, detectChanges: boolean) {
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
