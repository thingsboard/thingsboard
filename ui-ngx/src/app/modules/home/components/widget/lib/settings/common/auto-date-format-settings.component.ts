// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Component, forwardRef, Input, OnInit, Renderer2, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { AutoDateFormatSettings, defaultAutoDateFormatSettings } from '@shared/models/widget-settings.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { deepClone, mergeDeep } from '@core/utils';
import {
  AutoDateFormatSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/auto-date-format-settings-panel.component';

@Component({
    selector: 'tb-auto-date-format-settings',
    templateUrl: './auto-date-format-settings.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => AutoDateFormatSettingsComponent),
            multi: true
        }
    ],
    standalone: false
})
export class AutoDateFormatSettingsComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  defaultValues = defaultAutoDateFormatSettings;

  private modelValue: AutoDateFormatSettings;

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
  }

  writeValue(value: AutoDateFormatSettings): void {
    this.modelValue = value;
  }

  openAutoFormatSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const autoDateFormatSettingsPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: AutoDateFormatSettingsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: ['leftOnly', 'leftTopOnly', 'leftBottomOnly'],
        context: {
          autoDateFormatSettings: deepClone(this.modelValue),
          defaultValues: this.defaultValues
        },
        isModal: true
      });
      autoDateFormatSettingsPanelPopover.tbComponentRef.instance.popover = autoDateFormatSettingsPanelPopover;
      autoDateFormatSettingsPanelPopover.tbComponentRef.instance.autoDateFormatSettingsApplied.subscribe((autoDateFormatSettings) => {
        autoDateFormatSettingsPanelPopover.hide();
        this.modelValue = autoDateFormatSettings;
        this.propagateChange(this.modelValue);
      });
    }
  }

}
