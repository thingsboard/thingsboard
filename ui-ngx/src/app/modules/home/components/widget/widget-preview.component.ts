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

import { Component, Input, OnChanges, OnInit, SimpleChanges } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { IAliasController, IStateController } from '@core/api/widget-api.models';
import { Widget, WidgetConfig } from '@shared/models/widget.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { deepClone } from '@core/utils';
import { Timewindow } from '@shared/models/time/time.models';

@Component({
  selector: 'tb-widget-preview',
  templateUrl: './widget-preview.component.html',
  styleUrls: ['./widget-preview.component.scss']
})
export class WidgetPreviewComponent extends PageComponent implements OnInit, OnChanges {

  @Input()
  aliasController: IAliasController;

  @Input()
  stateController: IStateController;

  @Input()
  dashboardTimewindow: Timewindow;

  @Input()
  widget: Widget;

  @Input()
  widgetConfig: WidgetConfig;

  @Input()
  previewWidth = '100%';

  @Input()
  previewHeight = '70%';

  widgets: Widget[];

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    this.loadPreviewWidget();
  }

  ngOnChanges(changes: SimpleChanges): void {
    let reloadPreviewWidget = false;
    for (const propName of Object.keys(changes)) {
      const change = changes[propName];
      if (!change.firstChange && change.currentValue !== change.previousValue) {
        if (['widget', 'widgetConfig'].includes(propName)) {
          reloadPreviewWidget = true;
        }
      }
    }
    if (reloadPreviewWidget) {
      this.loadPreviewWidget();
    }
  }

  private loadPreviewWidget() {
    if (this.widget) {
      const widget = deepClone(this.widget);
      widget.sizeX = 24;
      widget.sizeY = this.widget.sizeY * 2;
      widget.row = 0;
      widget.col = 0;
      widget.config = this.widgetConfig;
      this.widgets = [widget];
    }
  }

}
