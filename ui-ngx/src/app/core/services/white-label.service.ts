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

import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { AdminService } from '@core/http/admin.service';
import { Title } from '@angular/platform-browser';
import { WhiteLabelSettings } from '@home/pages/admin/white-label.component';

export const WL_DEFAULTS: WhiteLabelSettings = {
  appTitle: 'APEX Electro Mechanical Service',
  logoUrl: 'assets/logo_title_white.svg',
  logoLoginUrl: 'assets/apex-logo-white-bg.png',
  primaryColor: '#FFDF63',
  supportEmail: '',
  supportUrl: '',
  footerText: 'APEX Electro Mechanical Service'
};

@Injectable({ providedIn: 'root' })
export class WhiteLabelService {

  private settingsSubject = new BehaviorSubject<WhiteLabelSettings>(WL_DEFAULTS);
  readonly settings$: Observable<WhiteLabelSettings> = this.settingsSubject.asObservable();

  private loaded = false;

  constructor(private adminService: AdminService,
              private titleService: Title) {}

  /** Call once after the user is authenticated. */
  load(): void {
    if (this.loaded) { return; }
    this.loaded = true;

    this.adminService.getWhiteLabelSettings().subscribe({
      next: (result) => {
        if (result?.jsonValue) {
          const merged = { ...WL_DEFAULTS, ...result.jsonValue };
          this.settingsSubject.next(merged);
          this.applyGlobal(merged);
        }
      },
      error: () => {
        // No saved white-label settings yet — use defaults, nothing to apply.
      }
    });
  }

  /** Re-loads settings and reapplies them (call after saving in White Label page). */
  reload(): void {
    this.loaded = false;
    this.load();
  }

  get snapshot(): WhiteLabelSettings {
    return this.settingsSubject.value;
  }

  private applyGlobal(s: WhiteLabelSettings): void {
    // Page title
    if (s.appTitle) {
      this.titleService.setTitle(s.appTitle);
    }
  }
}
