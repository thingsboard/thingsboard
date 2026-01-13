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

import { Component, forwardRef, Input, OnInit, Renderer2, ViewContainerRef } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';
import { ComponentStyle, cssUnit, Font } from '@shared/models/widget-settings.models';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { FontSettingsPanelComponent } from '@home/components/widget/lib/settings/common/font-settings-panel.component';
import { isDefinedAndNotNull } from '@core/utils';
import { coerceBoolean } from '@shared/decorators/coercion';

@Component({
  selector: 'tb-font-settings',
  templateUrl: './font-settings.component.html',
  styleUrls: [],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => FontSettingsComponent),
      multi: true
    }
  ]
})
export class FontSettingsComponent implements OnInit, ControlValueAccessor {

  @Input()
  disabled: boolean;

  @Input()
  previewText: string | (() => string);

  @Input()
  initialPreviewStyle: ComponentStyle;

  @Input()
  @coerceBoolean()
  clearButton = false;

  @Input()
  @coerceBoolean()
  autoScale = false;

  @Input()
  @coerceBoolean()
  disabledLineHeight = false;

  @Input()
  forceSizeUnit: cssUnit;

  private modelValue: Font;

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

  writeValue(value: Font): void {
    this.modelValue = value;
  }

  openFontSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const ctx: any = {
        font: this.modelValue,
        initialPreviewStyle: this.initialPreviewStyle,
        clearButton: this.clearButton,
        autoScale: this.autoScale,
        disabledLineHeight: this.disabledLineHeight,
        forceSizeUnit: this.forceSizeUnit
      };
      if (isDefinedAndNotNull(this.previewText)) {
        const previewText = typeof this.previewText === 'string' ? this.previewText : this.previewText();
        if (previewText) {
          ctx.previewText = previewText;
        }
      }
      const fontSettingsPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: FontSettingsPanelComponent,
        hostView: this.viewContainerRef,
        preferredPlacement: 'left',
        context: ctx,
        isModal: true
      });
      fontSettingsPanelPopover.tbComponentRef.instance.popover = fontSettingsPanelPopover;
      fontSettingsPanelPopover.tbComponentRef.instance.fontApplied.subscribe((font) => {
        fontSettingsPanelPopover.hide();
        this.modelValue = font;
        this.propagateChange(this.modelValue);
      });
    }
  }

}
