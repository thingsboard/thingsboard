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

import { Component, OnDestroy } from '@angular/core';
import { Store } from '@ngrx/store';
import { AppState } from '@core/core.state';
import { PageComponent } from '@shared/components/page.component';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { AdminService } from '@core/http/admin.service';
import { HasConfirmForm } from '@core/guards/confirm-on-exit.guard';
import { Subject } from 'rxjs';
import { AdminSettings } from '@shared/models/settings.models';

export interface WhiteLabelSettings {
  appTitle: string;
  logoUrl: string;
  logoLoginUrl: string;
  primaryColor: string;
  supportEmail: string;
  supportUrl: string;
  footerText: string;
}

@Component({
  selector: 'tb-white-label',
  templateUrl: './white-label.component.html',
  styleUrls: ['./white-label.component.scss', './settings-card.scss'],
  standalone: false
})
export class WhiteLabelComponent extends PageComponent implements HasConfirmForm, OnDestroy {

  whiteLabelForm: FormGroup;
  logoPreview: string = '';
  logoLoginPreview: string = '';

  private adminSettings: AdminSettings<WhiteLabelSettings>;
  private readonly destroy$ = new Subject<void>();

  constructor(protected store: Store<AppState>,
              private adminService: AdminService,
              public fb: FormBuilder) {
    super(store);
    this.buildForm();
    this.adminService.getAdminSettings<WhiteLabelSettings>('whiteLabel')
      .subscribe({
        next: settings => this.processSettings(settings),
        error: () => {
          // No existing settings, use defaults
          this.initDefaults();
        }
      });
  }

  ngOnDestroy() {
    super.ngOnDestroy();
    this.destroy$.next();
    this.destroy$.complete();
  }

  private buildForm() {
    this.whiteLabelForm = this.fb.group({
      appTitle: ['', [Validators.required]],
      logoUrl: [''],
      logoLoginUrl: [''],
      primaryColor: ['#FFDF63', [Validators.pattern(/^#[0-9A-Fa-f]{6}$/)]],
      supportEmail: ['', [Validators.email]],
      supportUrl: [''],
      footerText: ['']
    });

    this.whiteLabelForm.get('logoUrl').valueChanges.subscribe(v => this.logoPreview = v);
    this.whiteLabelForm.get('logoLoginUrl').valueChanges.subscribe(v => this.logoLoginPreview = v);
  }

  private initDefaults() {
    this.whiteLabelForm.patchValue({
      appTitle: 'APEX Electro Mechanical Service',
      logoUrl: '/assets/logo_title_white.svg',
      logoLoginUrl: '/assets/apex-logo-white-bg.png',
      primaryColor: '#FFDF63',
      supportEmail: '',
      supportUrl: '',
      footerText: 'APEX Electro Mechanical Service'
    });
  }

  private processSettings(settings: AdminSettings<WhiteLabelSettings>) {
    this.adminSettings = settings;
    if (settings?.jsonValue) {
      this.whiteLabelForm.patchValue(settings.jsonValue);
      this.logoPreview = settings.jsonValue.logoUrl || '';
      this.logoLoginPreview = settings.jsonValue.logoLoginUrl || '';
    } else {
      this.initDefaults();
    }
  }

  save(): void {
    if (!this.adminSettings) {
      this.adminSettings = {
        key: 'whiteLabel',
        jsonValue: null
      } as AdminSettings<WhiteLabelSettings>;
    }
    this.adminSettings.jsonValue = { ...this.adminSettings.jsonValue, ...this.whiteLabelForm.value };
    this.adminService.saveAdminSettings(this.adminSettings)
      .subscribe(settings => this.processSettings(settings));
  }

  discard(): void {
    if (this.adminSettings?.jsonValue) {
      this.whiteLabelForm.reset(this.adminSettings.jsonValue);
    } else {
      this.initDefaults();
    }
  }

  confirmForm(): FormGroup {
    return this.whiteLabelForm;
  }
}
