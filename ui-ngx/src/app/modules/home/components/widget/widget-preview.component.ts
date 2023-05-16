///
/// Copyright © 2016-2023 The Thingsboard Authors
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

import { Component, forwardRef, Input, OnInit } from '@angular/core';
import { ControlValueAccessor, NG_VALIDATORS, NG_VALUE_ACCESSOR, Validator } from '@angular/forms';
import { PageComponent } from '@shared/components/page.component';
import { IAliasController, IStateController } from '@core/api/widget-api.models';
import { Widget, WidgetConfig } from '@shared/models/widget.models';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { deepClone } from '@core/utils';

@Component({
  selector: 'tb-widget-preview',
  templateUrl: './widget-preview.component.html',
  styleUrls: ['./widget-preview.component.scss']
})
export class WidgetPreviewComponent extends PageComponent implements OnInit {

  @Input()
  aliasController: IAliasController;

  @Input()
  stateController: IStateController;

  @Input()
  widget: Widget;

  @Input()
  widgetConfig: WidgetConfig;

  widgets: Widget[];

  constructor(protected store: Store<AppState>) {
    super(store);
  }

  ngOnInit(): void {
    const sizeX = this.widget.sizeX * 2;
    const sizeY = this.widget.sizeY * 2;
    const col = Math.floor(Math.max(0, (20 - sizeX) / 2));
    const widget = deepClone(this.widget);
    widget.sizeX = sizeX;
    widget.sizeY = sizeY;
    widget.row = 0;
    widget.col = col;
    widget.config = this.widgetConfig;
    this.widgets = [widget];
  }

}
