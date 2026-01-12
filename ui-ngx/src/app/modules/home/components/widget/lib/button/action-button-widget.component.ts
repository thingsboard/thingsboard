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
  actionButtonDefaultSettings,
  ActionButtonWidgetSettings
} from '@home/components/widget/lib/button/action-button-widget.models';
import { WidgetButtonAppearance } from '@shared/components/button/widget-button.models';

@Component({
  selector: 'tb-action-button-widget',
  templateUrl: './action-button-widget.component.html',
  styleUrls: ['../action/action-widget.scss', './action-button-widget.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class ActionButtonWidgetComponent extends
  BasicActionWidgetComponent implements OnInit, AfterViewInit, OnDestroy {

  settings: ActionButtonWidgetSettings;

  disabled = false;
  activated = false;

  appearance: WidgetButtonAppearance;
  borderRadius = '4px';

  constructor(protected imagePipe: ImagePipe,
              protected sanitizer: DomSanitizer,
              protected cd: ChangeDetectorRef) {
    super(cd);
  }

  ngOnInit(): void {
    super.ngOnInit();
    this.settings = {...actionButtonDefaultSettings, ...this.ctx.settings};

    this.appearance = this.settings.appearance;

    const activatedStateSettings =
      {...this.settings.activatedState, actionLabel: this.ctx.translate.instant('widgets.button-state.activated-state')};
    this.createValueGetter(activatedStateSettings, ValueType.BOOLEAN, {
      next: (value) => this.onActivated(value)
    });

    const disabledStateSettings =
      {...this.settings.disabledState, actionLabel: this.ctx.translate.instant('widgets.button-state.disabled-state')};
    this.createValueGetter(disabledStateSettings, ValueType.BOOLEAN, {
      next: (value) => this.onDisabled(value)
    });
  }

  ngAfterViewInit(): void {
    super.ngAfterViewInit();
  }

  ngOnDestroy() {
    super.ngOnDestroy();
  }

  public onInit() {
    super.onInit();
    this.borderRadius = this.ctx.$widgetElement.css('borderRadius');
    this.cd.detectChanges();
  }

  public onClick($event: MouseEvent) {
    if (!this.ctx.isEdit && !this.ctx.isPreview) {
      this.ctx.actionsApi.click($event);
    }
  }

  private onActivated(value: boolean): void {
    this.activated = !!value;
    this.cd.markForCheck();
  }

  private onDisabled(value: boolean): void {
    this.disabled = !!value;
    this.cd.markForCheck();
  }

}
