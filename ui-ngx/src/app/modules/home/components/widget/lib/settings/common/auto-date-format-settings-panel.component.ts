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

import { Component, DestroyRef, EventEmitter, Input, OnInit, Output, ViewEncapsulation } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import {
  AutoDateFormatSettings, defaultAutoDateFormatSettings,
  FormatTimeUnit,
  formatTimeUnits,
  formatTimeUnitTranslations
} from '@shared/models/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormBuilder, UntypedFormGroup, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-auto-date-format-settings-panel',
  templateUrl: './auto-date-format-settings-panel.component.html',
  providers: [],
  styleUrls: ['./auto-date-format-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class AutoDateFormatSettingsPanelComponent extends PageComponent implements OnInit {

  formatTimeUnits = formatTimeUnits;

  formatTimeUnitTranslations = formatTimeUnitTranslations;

  @Input()
  autoDateFormatSettings: AutoDateFormatSettings;

  @Input()
  defaultValues = defaultAutoDateFormatSettings;

  @Input()
  popover: TbPopoverComponent<AutoDateFormatSettingsPanelComponent>;

  @Output()
  autoDateFormatSettingsApplied = new EventEmitter<AutoDateFormatSettings>();

  autoDateFormatFormGroup: UntypedFormGroup;

  previewText: {[unit in FormatTimeUnit]: string} = {} as any;

  constructor(private date: DatePipe,
              private fb: UntypedFormBuilder,
              protected store: Store<AppState>,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.autoDateFormatFormGroup = this.fb.group({});
    for (const unit of formatTimeUnits) {
      this.autoDateFormatFormGroup.addControl(unit,
        this.fb.control(this.autoDateFormatSettings[unit] || this.defaultValues[unit], [Validators.required]));
      this.autoDateFormatFormGroup.get(unit).valueChanges.pipe(
        takeUntilDestroyed(this.destroyRef)
      ).subscribe((value: string) => {
        this.previewText[unit] = this.date.transform(Date.now(), value);
      });
      this.previewText[unit] = this.date.transform(Date.now(), this.autoDateFormatSettings[unit] || this.defaultValues[unit]);
    }
  }

  cancel() {
    this.popover?.hide();
  }

  applyAutoDateFormatSettings() {
    const autoDateFormatSettings: AutoDateFormatSettings = this.autoDateFormatFormGroup.value;
    for (const unit of formatTimeUnits) {
      if (autoDateFormatSettings[unit] === this.defaultValues[unit]) {
        delete autoDateFormatSettings[unit];
      }
    }
    this.autoDateFormatSettingsApplied.emit(autoDateFormatSettings);
  }

}
