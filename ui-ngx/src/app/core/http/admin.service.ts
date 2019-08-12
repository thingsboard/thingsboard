///
/// Copyright © 2016-2019 The Thingsboard Authors
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
import { defaultHttpOptions } from './http-utils';
import { Observable } from 'rxjs/index';
import { HttpClient } from '@angular/common/http';
import {AdminSettings, MailServerSettings, SecuritySettings} from '@shared/models/settings.models';

@Injectable({
  providedIn: 'root'
})
export class AdminService {

  constructor(
    private http: HttpClient
  ) { }

  public getAdminSettings<T>(key: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<AdminSettings<T>> {
    return this.http.get<AdminSettings<T>>(`/api/admin/settings/${key}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public saveAdminSettings<T>(adminSettings: AdminSettings<T>,
                              ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<AdminSettings<T>> {
    return this.http.post<AdminSettings<T>>('/api/admin/settings', adminSettings, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public sendTestMail(adminSettings: AdminSettings<MailServerSettings>,
                      ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<void> {
    return this.http.post<void>('/api/admin/settings/testMail', adminSettings, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getSecuritySettings(ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<SecuritySettings> {
    return this.http.get<SecuritySettings>(`/api/admin/securitySettings`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public saveSecuritySettings(securitySettings: SecuritySettings,
                              ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<SecuritySettings> {
    return this.http.post<SecuritySettings>('/api/admin/securitySettings', securitySettings,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }
}
