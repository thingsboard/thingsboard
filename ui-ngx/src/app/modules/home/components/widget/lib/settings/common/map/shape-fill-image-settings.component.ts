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

import { ChangeDetectorRef, Component, forwardRef, Input, Renderer2, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { ShapeFillImageSettings, ShapeFillImageType } from '@shared/models/widget/maps/map.models';
import {
  ShapeFillImageSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/map/shape-fill-image-settings-panel.component';

@Component({
  selector: 'tb-shape-fill-image-settings',
  templateUrl: './shape-fill-image-settings.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => ShapeFillImageSettingsComponent),
      multi: true
    }
  ]
})
export class ShapeFillImageSettingsComponent implements ControlValueAccessor {

  @Input()
  disabled: boolean;

  ShapeFillImageType = ShapeFillImageType;

  modelValue: ShapeFillImageSettings;

  private propagateChange: (v: any) => void = () => { };

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef,
              private viewContainerRef: ViewContainerRef) {}

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(_fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: ShapeFillImageSettings): void {
    if (value) {
      this.modelValue = value;
    }
  }

  openImageSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: ShapeFillImageSettingsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: 'left',
        context: {
          shapeFillImageSettings: this.modelValue,
        },
        isModal: true
      }).tbComponentRef.instance.shapeFillImageSettingsApplied.subscribe((shapeFillImageSettings) => {
        this.modelValue = shapeFillImageSettings;
        this.propagateChange(this.modelValue);
        this.cd.detectChanges();
      });
    }
  }
}
