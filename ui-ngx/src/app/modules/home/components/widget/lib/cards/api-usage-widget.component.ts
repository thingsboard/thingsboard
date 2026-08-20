///
/// Copyright © 2016-2026 The Thingsboard Authors
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

import { ChangeDetectorRef, Component, Input, OnDestroy, OnInit, TemplateRef, ViewEncapsulation } from '@angular/core';
import { WidgetContext } from '@home/models/widget-component.models';
import { backgroundStyle, ComponentStyle, overlayStyle } from '@shared/models/widget-settings.models';
import { Observable } from 'rxjs';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { DatasourceType, widgetType } from '@shared/models/widget.models';
import { WidgetSubscriptionOptions } from '@core/api/widget-api.models';
import { formattedDataFormDatasourceData } from '@core/utils';

import { UtilsService } from '@core/services/utils.service';
import {
  apiUsageDefaultSettings,
  ApiUsageWidgetSettings,
  getUniqueDataKeys
} from '@home/components/widget/lib/settings/cards/api-usage-settings.component.models';
import { ShortNumberPipe } from '@shared/pipe/short-number.pipe';

@Component({
    selector: 'tb-api-usage-widget',
    templateUrl: './api-usage-widget.component.html',
    styleUrls: ['api-usage-widget.component.scss'],
    encapsulation: ViewEncapsulation.None,
    standalone: false
})
export class ApiUsageWidgetComponent implements OnInit, OnDestroy {

  settings: ApiUsageWidgetSettings;

  @Input()
  ctx: WidgetContext;

  @Input()
  widgetTitlePanel: TemplateRef<any>;

  backgroundStyle$: Observable<ComponentStyle>;
  overlayStyle: ComponentStyle = {};
  padding: string;

  apiUsages = [];
  currentState = '';
  noDataDisplayMessageText: string;

  private contentResize$: ResizeObserver;

  constructor(private imagePipe: ImagePipe,
              private utils: UtilsService,
              private sanitizer: DomSanitizer,
              private cd: ChangeDetectorRef,
              private shortNumberPipe: ShortNumberPipe) {
  }

  ngOnInit(): void {
    this.ctx.$scope.apiUsageWidget = this;
    this.settings = {...apiUsageDefaultSettings, ...this.ctx.settings};

    this.parseApiUsages();

    const ds = {
      type: DatasourceType.entity,
      name: '',
      entityAliasId: this.settings.dsEntityAliasId,
      dataKeys: getUniqueDataKeys(this.settings.apiUsageDataKeys)
    }

    const apiUsageSubscriptionOptions: WidgetSubscriptionOptions = {
      datasources: [ds],
      useDashboardTimewindow: false,
      type: widgetType.latest,
      callbacks: {
        onDataUpdated: (subscription) => {
          const data = formattedDataFormDatasourceData(subscription.data);
          this.apiUsages.forEach(key => {
            const progress = (this.isFiniteNumber(data[0][key.maxLimit.key]) && data[0][key.maxLimit.key] !== 0) ? Math.min(100, ((data[0][key.current.key] / data[0][key.maxLimit.key]) * 100)) : 0;
            key.progress = isFinite(progress) ? progress : 0;
            key.status.value = data[0][key.status.key] ? data[0][key.status.key].toLowerCase() : 'enabled';
            key.maxLimit.value = this.isFiniteNumber(data[0][key.maxLimit.key]) && data[0][key.maxLimit.key] !== 0 ? this.shortNumberPipe.transform(data[0][key.maxLimit.key]) : '∞';
            key.current.value = this.isFiniteNumber(data[0][key.current.key]) ? this.shortNumberPipe.transform(data[0][key.current.key], {roundDown: true}) : 0;
          });
          this.cd.detectChanges();
        }
      }
    };
    this.ctx.subscriptionApi.createSubscription(apiUsageSubscriptionOptions, true).subscribe();

    this.currentState = this.ctx.stateController.getStateId();
    this.ctx.stateController.stateId().subscribe((state) => {
      this.ctx.updateParamsFromData(true);
      this.currentState = state;
      this.cd.markForCheck();
    });
    this.backgroundStyle$ = backgroundStyle(this.settings.background, this.imagePipe, this.sanitizer);
    this.overlayStyle = overlayStyle(this.settings.background.overlay);
    this.padding = this.settings.background.overlay.enabled ? undefined : this.settings.padding;
  }

  private isFiniteNumber(value: any): boolean {
    return typeof value === 'number' && isFinite(value);
  }

  updateState($event: MouseEvent, stateName: string) {
    $event?.preventDefault();
    if (stateName?.length) {
      this.ctx.stateController.updateState(stateName, this.ctx.stateController.getStateParams(), this.ctx.isMobile);
    }
  }

  parseApiUsages() {
    this.settings.apiUsageDataKeys.forEach((key) => {
      this.apiUsages.push({
        label: this.utils.customTranslation(key.label, key.label),
        state: key.state,
        progress: 0,
        status: {key: key.status.name, value: 'enabled'},
        maxLimit: {key: key.maxLimit.name, value: '∞'},
        current: {key: key.current.name, value: 0},
      });
    })
  }

  ngOnDestroy() {
    if (this.contentResize$) {
      this.contentResize$.disconnect();
    }
  }

  public onInit() {
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

}
