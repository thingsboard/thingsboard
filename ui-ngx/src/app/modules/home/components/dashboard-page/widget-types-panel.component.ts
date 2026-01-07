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

import { Component, Inject, InjectionToken } from '@angular/core';
import { widgetType } from '@shared/models/widget.models';

export const DISPLAY_WIDGET_TYPES_PANEL_DATA = new InjectionToken<any>('DisplayWidgetTypesPanelData');

export interface WidgetTypes {
  type: widgetType;
  display: boolean;
}

export interface DisplayWidgetTypesPanelData {
  types: WidgetTypes[];
  typesUpdated: (columns: WidgetTypes[]) => void;
}

@Component({
  selector: 'tb-widget-types-panel',
  templateUrl: './widget-types-panel.component.html',
  styleUrls: ['./widget-types-panel.component.scss']
})
export class DisplayWidgetTypesPanelComponent {

  types: WidgetTypes[];

  constructor(@Inject(DISPLAY_WIDGET_TYPES_PANEL_DATA) public data: DisplayWidgetTypesPanelData) {
    this.types = this.data.types;
  }

  public update() {
    this.data.typesUpdated(this.types);
  }
}
