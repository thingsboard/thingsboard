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
import { User } from '../../shared/models/user.model';
import { Observable } from 'rxjs/index';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { AdminSettings } from '@shared/models/settings.models';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor(
    private http: HttpClient
  ) { }

  public getTenantAdmins(tenantId: string, pageLink: PageLink,
                         ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<PageData<User>> {
    return this.http.get<PageData<User>>(`/api/tenant/${tenantId}/users${pageLink.toQuery()}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getCustomerUsers(customerId: string, pageLink: PageLink,
                          ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<PageData<User>> {
    return this.http.get<PageData<User>>(`/api/customer/${customerId}/users${pageLink.toQuery()}`,
      defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getUser(userId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<User> {
    return this.http.get<User>(`/api/user/${userId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public saveUser(user: User, sendActivationMail: boolean = false,
                  ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<User> {
    let url = '/api/user';
    url += '?sendActivationMail=' + sendActivationMail;
    return this.http.post<User>(url, user, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public deleteUser(userId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false) {
    return this.http.delete(`/api/user/${userId}`, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

  public getActivationLink(userId: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false): Observable<string> {
    return this.http.get(`/api/user/${userId}/activationLink`,
      {...{responseType: 'text'}, ...defaultHttpOptions(ignoreLoading, ignoreErrors)});
  }

  public sendActivationEmail(email: string, ignoreErrors: boolean = false, ignoreLoading: boolean = false) {
    return this.http.post(`/api/user/sendActivationMail?email=${email}`, null, defaultHttpOptions(ignoreLoading, ignoreErrors));
  }

}
