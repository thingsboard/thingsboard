///
/// Copyright © 2016-2020 The Thingsboard Authors
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

import { Component, Inject, InjectionToken, OnInit, ViewContainerRef } from '@angular/core';
import { OverlayRef } from '@angular/cdk/overlay';
import { PageComponent } from '@shared/components/page.component';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup } from '@angular/forms';
import {
  LegendConfig,
  LegendDirection,
  legendDirectionTranslationMap,
  LegendPosition,
  legendPositionTranslationMap
} from '@shared/models/widget.models';

export const LEGEND_CONFIG_PANEL_DATA = new InjectionToken<any>('LegendConfigPanelData');

export interface LegendConfigPanelData {
  legendConfig: LegendConfig;
  legendConfigUpdated: (legendConfig: LegendConfig) => void;
}

@Component({
  selector: 'tb-legend-config-panel',
  templateUrl: './legend-config-panel.component.html',
  styleUrls: ['./legend-config-panel.component.scss']
})
export class LegendConfigPanelComponent extends PageComponent implements OnInit {

  legendConfigForm: FormGroup;

  legendDirection = LegendDirection;

  legendDirections = Object.keys(LegendDirection);

  legendDirectionTranslations = legendDirectionTranslationMap;

  legendPosition = LegendPosition;

  legendPositions = Object.keys(LegendPosition);

  legendPositionTranslations = legendPositionTranslationMap;

  constructor(@Inject(LEGEND_CONFIG_PANEL_DATA) public data: LegendConfigPanelData,
              public overlayRef: OverlayRef,
              protected store: Store<AppState>,
              public fb: FormBuilder,
              public viewContainerRef: ViewContainerRef) {
    super(store);
  }

  ngOnInit(): void {
    this.legendConfigForm = this.fb.group({
      direction: [this.data.legendConfig.direction, []],
      position: [this.data.legendConfig.position, []],
      showMin: [this.data.legendConfig.showMin, []],
      showMax: [this.data.legendConfig.showMax, []],
      showAvg: [this.data.legendConfig.showAvg, []],
      showTotal: [this.data.legendConfig.showTotal, []]
    });
    this.legendConfigForm.get('direction').valueChanges.subscribe((direction: LegendDirection) => {
      this.onDirectionChanged(direction);
    });
    this.onDirectionChanged(this.data.legendConfig.direction);
    this.legendConfigForm.valueChanges.subscribe(() => {
      this.update();
    });
  }

  private onDirectionChanged(direction: LegendDirection) {
    if (direction === LegendDirection.row) {
      let position: LegendPosition = this.legendConfigForm.get('position').value;
      if (position !== LegendPosition.bottom && position !== LegendPosition.top) {
        position = LegendPosition.bottom;
      }
      this.legendConfigForm.patchValue(
        {
          position
        }, {emitEvent: false}
      );
    }
  }

  update() {
    const newLegendConfig: LegendConfig = this.legendConfigForm.value;
    this.data.legendConfigUpdated(newLegendConfig);
  }

}
