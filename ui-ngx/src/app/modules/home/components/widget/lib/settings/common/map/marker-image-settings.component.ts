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
import { MarkerImageSettings, MarkerImageType } from '@shared/models/widget/maps/map.models';
import {
  MarkerImageSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/map/marker-image-settings-panel.component';

@Component({
    selector: 'tb-marker-image-settings',
    templateUrl: './marker-image-settings.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => MarkerImageSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class MarkerImageSettingsComponent implements ControlValueAccessor {

  @Input()
  disabled: boolean;

  MarkerImageType = MarkerImageType;

  modelValue: MarkerImageSettings;

  private propagateChange: (v: any) => void = () => { };

  constructor(private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private cd: ChangeDetectorRef,
              private viewContainerRef: ViewContainerRef) {}

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  writeValue(value: MarkerImageSettings): void {
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
      const markerImageSettingsPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: MarkerImageSettingsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: 'left',
        context: {
          markerImageSettings: this.modelValue,
        },
        isModal: true
      });
      markerImageSettingsPanelPopover.tbComponentRef.instance.popover = markerImageSettingsPanelPopover;
      markerImageSettingsPanelPopover.tbComponentRef.instance.markerImageSettingsApplied.subscribe((markerImageSettings) => {
        markerImageSettingsPanelPopover.hide();
        this.modelValue = markerImageSettings;
        this.propagateChange(this.modelValue);
        this.cd.detectChanges();
      });
    }
  }
}
