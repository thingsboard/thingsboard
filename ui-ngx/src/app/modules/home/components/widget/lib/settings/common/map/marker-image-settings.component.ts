// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
        preferredPlacement: 'leftOnly',
        context: {
          markerImageSettings: this.modelValue,
        },
        isModal: true,
        overlayStyle: {padding: '10px'}
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
