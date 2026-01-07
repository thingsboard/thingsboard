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
import { DateFormatSettings } from '@shared/models/widget-settings.models';
import { TbPopoverComponent } from '@shared/components/popover.component';
import { UntypedFormControl, Validators } from '@angular/forms';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'tb-date-format-settings-panel',
  templateUrl: './date-format-settings-panel.component.html',
  providers: [],
  styleUrls: ['./date-format-settings-panel.component.scss'],
  encapsulation: ViewEncapsulation.None
})
export class DateFormatSettingsPanelComponent extends PageComponent implements OnInit {

  @Input()
  dateFormat: DateFormatSettings;

  @Input()
  popover: TbPopoverComponent<DateFormatSettingsPanelComponent>;

  @Output()
  dateFormatApplied = new EventEmitter<DateFormatSettings>();

  dateFormatFormControl: UntypedFormControl;

  previewText = '';

  constructor(private date: DatePipe,
              protected store: Store<AppState>,
              private destroyRef: DestroyRef) {
    super(store);
  }

  ngOnInit(): void {
    this.dateFormatFormControl = new UntypedFormControl(this.dateFormat.format, [Validators.required]);
    this.dateFormatFormControl.valueChanges.pipe(
      takeUntilDestroyed(this.destroyRef)
    ).subscribe((value: string) => {
      this.previewText = this.date.transform(Date.now(), value);
    });
    this.previewText = this.date.transform(Date.now(), this.dateFormat.format);
  }

  cancel() {
    this.popover?.hide();
  }

  applyDateFormat() {
    this.dateFormat.format = this.dateFormatFormControl.value;
    this.dateFormatApplied.emit(this.dateFormat);
  }

}
