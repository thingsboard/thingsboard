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

import { Component, forwardRef, Input, OnInit, Renderer2, ViewContainerRef, ViewEncapsulation } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import {
  BackgroundSettings,
  backgroundStyle,
  BackgroundType,
  ComponentStyle,
  overlayStyle
} from '@shared/models/widget-settings.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import {
  BackgroundSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/background-settings-panel.component';

@Component({
  selector: 'tb-background-settings',
  templateUrl: './background-settings.component.html',
  styleUrls: ['./background-settings.component.scss'],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => BackgroundSettingsComponent),
      multi: true
    }
  ],
  encapsulation: ViewEncapsulation.None
})
export class BackgroundSettingsComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  backgroundType = BackgroundType;

  modelValue: BackgroundSettings;

  backgroundStyle: ComponentStyle = {};

  overlayStyle: ComponentStyle = {};

  private propagateChange = null;

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef) {}

  ngOnInit(): void {
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.updateBackgroundStyle();
  }

  writeValue(value: BackgroundSettings): void {
    this.modelValue = value;
    this.updateBackgroundStyle();
  }

  openBackgroundSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        backgroundSettings: this.modelValue
      };
     const backgroundSettingsPanelPopover = this.popoverService.displayPopover(trigger, this.renderer,
        this.viewContainerRef, BackgroundSettingsPanelComponent, 'left', true, null,
        ctx,
        {},
        {}, {}, true);
      backgroundSettingsPanelPopover.tbComponentRef.instance.popover = backgroundSettingsPanelPopover;
      backgroundSettingsPanelPopover.tbComponentRef.instance.backgroundSettingsApplied.subscribe((backgroundSettings) => {
        backgroundSettingsPanelPopover.hide();
        this.modelValue = backgroundSettings;
        this.updateBackgroundStyle();
        this.propagateChange(this.modelValue);
      });
    }
  }

  private updateBackgroundStyle() {
    if (!this.disabled) {
      this.backgroundStyle = backgroundStyle(this.modelValue);
      this.overlayStyle = overlayStyle(this.modelValue.overlay);
    } else {
      this.backgroundStyle = {};
      this.overlayStyle = {};
    }
  }

}
