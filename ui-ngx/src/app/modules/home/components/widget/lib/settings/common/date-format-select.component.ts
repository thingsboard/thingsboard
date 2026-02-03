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

import {
  Component,
  DestroyRef,
  forwardRef,
  Input,
  OnInit,
  Renderer2,
  ViewChild,
  ViewContainerRef
} from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, UntypedFormControl } from '@angular/forms';
import {
  AutoDateFormatSettings,
  compareDateFormats,
  dateFormats,
  DateFormatSettings,
  dateFormatsWithAuto,
  defaultAutoDateFormatSettings
} from '@shared/models/widget-settings.models';
import { TranslateService } from '@ngx-translate/core';
import { DatePipe } from '@angular/common';
import { MatButton } from '@angular/material/button';
import { TbPopoverService } from '@shared/components/popover.service';
import { deepClone, mergeDeep } from '@core/utils';
import {
  DateFormatSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/date-format-settings-panel.component';
import { coerceBoolean } from '@shared/decorators/coercion';
import {
  AutoDateFormatSettingsPanelComponent
} from '@home/components/widget/lib/settings/common/auto-date-format-settings-panel.component';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
    selector: 'tb-date-format-select',
    templateUrl: './date-format-select.component.html',
    styleUrls: [],
    providers: [
        {
            provide: NG_VALUE_ACCESSOR,
            useExisting: forwardRef(() => DateFormatSelectComponent),
            multi: true
        }
    ],
    standalone: false
})
export class DateFormatSelectComponent implements OnInit, ControlValueAccessor {

  @ViewChild('customFormatButton', {static: false})
  customFormatButton: MatButton;

  @Input()
  disabled: boolean;

  @Input()
  @coerceBoolean()
  excludeLastUpdateAgo = false;

  @Input()
  @coerceBoolean()
  includeAuto = false;

  dateFormatList: DateFormatSettings[];

  dateFormatsCompare = compareDateFormats;

  dateFormatFormControl: UntypedFormControl;

  modelValue: DateFormatSettings;

  private propagateChange = null;

  private formatCache: {[format: string]: string} = {};

  constructor(private translate: TranslateService,
              private date: DatePipe,
              private popoverService: TbPopoverService,
              private renderer: Renderer2,
              private viewContainerRef: ViewContainerRef,
              private destroyRef: DestroyRef) {}

  ngOnInit(): void {
    const targetDateFormats = this.includeAuto ? dateFormatsWithAuto : dateFormats;
    this.dateFormatList = this.excludeLastUpdateAgo ?
      targetDateFormats.filter(format => !format.lastUpdateAgo) : dateFormats;
    this.dateFormatFormControl = new UntypedFormControl();
    this.dateFormatFormControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value: DateFormatSettings) => {
      this.updateModel(value);
      if (value?.custom) {
        setTimeout(() => {
          this.openDateFormatSettingsPopup(null, this.customFormatButton);
        }, 0);
      }
    });
  }

  registerOnChange(fn: any): void {
    this.propagateChange = fn;
  }

  registerOnTouched(fn: any): void {
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    if (this.disabled) {
      this.dateFormatFormControl.disable({emitEvent: false});
    } else {
      this.dateFormatFormControl.enable({emitEvent: false});
    }
  }

  writeValue(value: DateFormatSettings): void {
    this.modelValue = value;
    this.dateFormatFormControl.patchValue(this.modelValue, {emitEvent: false});
  }

  updateModel(value: DateFormatSettings): void {
    if (!compareDateFormats(this.modelValue, value)) {
      this.modelValue = value;
      this.propagateChange(this.modelValue);
    }
  }

  dateFormatDisplayValue(value: DateFormatSettings): string {
    if (value.custom) {
      return this.translate.instant('date.custom-date');
    } else if (value.lastUpdateAgo) {
      return this.translate.instant('date.last-update-n-ago');
    } else if (value.auto) {
      return this.translate.instant('date.auto');
    } else {
      if (!this.formatCache[value.format]) {
        this.formatCache[value.format] = this.date.transform(Date.now(), value.format);
      }
      return this.formatCache[value.format];
    }
  }

  openDateFormatSettingsPopup($event: Event, matButton: MatButton) {
    if ($event) {
      $event.stopPropagation();
    }
    const trigger = matButton._elementRef.nativeElement;
    if (this.popoverService.hasPopover(trigger)) {
      this.popoverService.hidePopover(trigger);
    } else {
      const dateFormatSettingsPanelPopover = this.popoverService.displayPopover({
        trigger,
        renderer: this.renderer,
        componentType: DateFormatSettingsPanelComponent,
        hostView: this.viewContainerRef,
        context: {
          dateFormat: deepClone(this.modelValue)
        },
        isModal: true
      });
      dateFormatSettingsPanelPopover.tbComponentRef.instance.popover = dateFormatSettingsPanelPopover;
      dateFormatSettingsPanelPopover.tbComponentRef.instance.dateFormatApplied.subscribe((dateFormat) => {
        dateFormatSettingsPanelPopover.hide();
        this.modelValue = dateFormat;
        this.propagateChange(this.modelValue);
      });
    }
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
          autoDateFormatSettings: mergeDeep({} as AutoDateFormatSettings,
            defaultAutoDateFormatSettings, this.modelValue.autoDateFormatSettings)
        },
        isModal: true
      });
      autoDateFormatSettingsPanelPopover.tbComponentRef.instance.popover = autoDateFormatSettingsPanelPopover;
      autoDateFormatSettingsPanelPopover.tbComponentRef.instance.autoDateFormatSettingsApplied.subscribe((autoDateFormatSettings) => {
        autoDateFormatSettingsPanelPopover.hide();
        this.modelValue.autoDateFormatSettings = autoDateFormatSettings;
        this.propagateChange(this.modelValue);
      });
    }
  }
}
