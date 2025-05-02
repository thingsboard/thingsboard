///
/// Copyright © 2016-2025 The Thingsboard Authors
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

import { Component, OnInit } from '@angular/core';
import { PageComponent } from '@shared/components/page.component';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { select, Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { TrendzSettingsService } from '@core/http/trendz-settings.service';
import { TrendzSettings } from '@shared/models/trendz-settings.models';
import { isDefinedAndNotNull } from '@core/utils';

@Component({
  selector: 'tb-trendz-settings',
  templateUrl: './trendz-settings.component.html',
  styleUrls: ['./trendz-settings.component.scss', './settings-card.scss']
})
export class TrendzSettingsComponent extends PageComponent implements OnInit, HasConfirmForm {

  trendzSettingsForm: FormGroup;

  constructor(protected store: Store<AppState>,
              private fb: FormBuilder,
              private trendzSettingsService: TrendzSettingsService) {
    super(store);
  }

  ngOnInit() {
    this.trendzSettingsForm = this.fb.group({
      trendzUrl: [null, [Validators.pattern(/^(https?:\/\/)[^\s/$.?#].[^\s]*$/i)]],
      isTrendzEnabled: [false]
    });

    this.trendzSettingsService.getTrendzSettings().subscribe((trendzSettings) => {
      this.setTrendzSettings(trendzSettings);
    });

    this.trendzSettingsForm.get('isTrendzEnabled').valueChanges
          .subscribe((enabled: boolean) => this.toggleUrlRequired(enabled));
  }

  toggleUrlRequired(enabled: boolean) {
    const trendzUrlControl = this.trendzSettingsForm.get('trendzUrl')!;
    const validators = [Validators.pattern(/^(https?:\/\/)[^\s/$.?#].[^\s]*$/i)];

    if (enabled) {
      validators.push(Validators.required);
    }

    trendzUrlControl.setValidators(validators);
    trendzUrlControl.updateValueAndValidity();
  }

  setTrendzSettings(trendzSettings: TrendzSettings) {
    this.trendzSettingsForm.reset({
      trendzUrl: trendzSettings?.baseUrl,
      isTrendzEnabled: isDefinedAndNotNull(trendzSettings?.enabled) ?
        trendzSettings?.enabled : false
    });

    this.toggleUrlRequired(this.trendzSettingsForm.get('isTrendzEnabled').value);
  }

  confirmForm(): FormGroup {
    return this.trendzSettingsForm;
  }

  save(): void {
    const trendzUrl = this.trendzSettingsForm.get('trendzUrl').value;
    const isTrendzEnabled =   this.trendzSettingsForm.get('isTrendzEnabled').value;

    const trendzSettings: TrendzSettings = {
      baseUrl: trendzUrl,
      enabled: isTrendzEnabled
    };

    this.trendzSettingsService.saveTrendzSettings(trendzSettings).subscribe(() => {
      this.setTrendzSettings(trendzSettings);
    })
  }
}
