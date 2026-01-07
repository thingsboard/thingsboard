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

import { AfterViewInit, ChangeDetectorRef, Component, OnDestroy, OnInit, ViewEncapsulation } from '@angular/core';
import { BasicActionWidgetComponent } from '@home/components/widget/lib/action/action-widget.models';
import { ImagePipe } from '@shared/pipe/image.pipe';
import { DomSanitizer } from '@angular/platform-browser';
import { ValueType } from '@shared/models/constants';
import {
  ButtonToggleAppearance,
  segmentedButtonDefaultSettings,
  SegmentedButtonWidgetSettings
} from '@home/components/widget/lib/button/segmented-button-widget.models';
import { ComponentStyle } from '@shared/models/widget-settings.models';
import { MatButtonToggleChange } from '@angular/material/button-toggle';

@Component({
  selector: 'tb-two-segment-button-widget',
  templateUrl: './two-segment-button-widget.component.html',
  styleUrls: ['../action/action-widget.scss', './two-segment-button-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class TwoSegmentButtonWidgetComponent extends
  BasicActionWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  settings: SegmentedButtonWidgetSettings;

  overlayStyle: ComponentStyle = {};

  value = false;
  disabled = false;

  autoScale: boolean;
  appearance: ButtonToggleAppearance;

  constructor(protected imagePipe: ImagePipe,
              protected sanitizer: DomSanitizer,
              protected cd: ChangeDetectorRef) {
    super(cd);
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.settings = {...segmentedButtonDefaultSettings, ...this.ctx.settings};

    this.autoScale = this.settings.appearance.autoScale;

    const getInitialStateSettings =
      {...this.settings.initialState, actionLabel: this.ctx.translate.instant('widgets.rpc-state.initial-state')};
    this.createValueGetter(getInitialStateSettings, ValueType.BOOLEAN, {
      next: (value) => this.onValue(value)
    });

    const disabledStateSettings =
      {...this.settings.disabledState, actionLabel: this.ctx.translate.instant('widgets.button-state.disabled-state')};
    this.createValueGetter(disabledStateSettings, ValueType.BOOLEAN, {
      next: (value) => this.onDisabled(value)
    });

    this.appearance = this.settings.appearance;
  }

  ngAfterViewInit(): void {
    super.ngAfterViewInit();
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  public onInit() {
    super.onInit();
    const borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.overlayStyle = {...this.overlayStyle, ...{borderRadius}};
    this.cd.detectChanges();
  }

  private onValue(value: boolean): void {
    const newValue = !!value;
    if (this.value !== newValue) {
      this.value = newValue;
      this.appearance = this.settings.appearance;
      this.cd.markForCheck();
    }
  }

  private onDisabled(value: boolean): void {
    const newDisabled = !!value;
    if (this.disabled !== newDisabled) {
      this.disabled = newDisabled;
      this.cd.markForCheck();
    }
  }

  public onClick(_$event: MatButtonToggleChange) {
    if (!this.ctx.isEdit && !this.ctx.isPreview) {
      if (_$event.value) {
        this.ctx.actionsApi.onWidgetAction(event, this.settings.leftButtonClick);
      } else {
        this.ctx.actionsApi.onWidgetAction(event, this.settings.rightButtonClick);
      }
    }
  }

}
