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
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { ActivationLinkInfo, User, UserEmailInfo } from '@shared/models/user.model';
import { Observable } from 'rxjs';
import { HttpClient, HttpParams } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { isDefined } from '@core/utils';
import { InterceptorHttpParams } from '@core/interceptors/interceptor-http-params';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor(
    private http: HttpClient
  ) { }

  public getUsers(pageLink: PageLink,
                  config?: RequestConfig): Observable<PageData<User>> {
    return this.http.get<PageData<User>>(`/api/users${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getTenantAdmins(tenantId: string, pageLink: PageLink,
                         config?: RequestConfig): Observable<PageData<User>> {
    return this.http.get<PageData<User>>(`/api/tenant/${tenantId}/users${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerUsers(customerId: string, pageLink: PageLink,
                          config?: RequestConfig): Observable<PageData<User>> {
    return this.http.get<PageData<User>>(`/api/customer/${customerId}/users${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getUsersForAssign(alarmId: string, pageLink: PageLink,
                          config?: RequestConfig): Observable<PageData<UserEmailInfo>> {
    return this.http.get<PageData<UserEmailInfo>>(`/api/users/assign/${alarmId}${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getUser(userId: string, config?: RequestConfig): Observable<User> {
    return this.http.get<User>(`/api/user/${userId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveUser(user: User, sendActivationMail: boolean = false,
                  config?: RequestConfig): Observable<User> {
    let url = '/api/user';
    url += '?sendActivationMail=' + sendActivationMail;
    return this.http.post<User>(url, user, defaultHttpOptionsFromConfig(config));
  }

  public deleteUser(userId: string, config?: RequestConfig) {
    return this.http.delete(`/api/user/${userId}`, defaultHttpOptionsFromConfig(config));
  }

  public getActivationLink(userId: string, config?: RequestConfig): Observable<string> {
    return this.http.get(`/api/user/${userId}/activationLink`,
      {...{responseType: 'text'}, ...defaultHttpOptionsFromConfig(config)});
  }

  public getActivationLinkInfo(userId: string, config?: RequestConfig): Observable<ActivationLinkInfo> {
    return this.http.get<ActivationLinkInfo>(`/api/user/${userId}/activationLinkInfo`, defaultHttpOptionsFromConfig(config));
  }

  public sendActivationEmail(email: string, config?: RequestConfig) {
    const encodeEmail = encodeURIComponent(email);
    return this.http.post(`/api/user/sendActivationMail?email=${encodeEmail}`, null, defaultHttpOptionsFromConfig(config));
  }

  public setUserCredentialsEnabled(userId: string, userCredentialsEnabled?: boolean, config?: RequestConfig): Observable<any> {
    let url = `/api/user/${userId}/userCredentialsEnabled`;
    if (isDefined(userCredentialsEnabled)) {
      url += `?userCredentialsEnabled=${userCredentialsEnabled}`;
    }
    return this.http.post<User>(url, null, defaultHttpOptionsFromConfig(config));
  }

  public findUsersByQuery(pageLink: PageLink, config?: RequestConfig) : Observable<PageData<UserEmailInfo>> {
    return this.http.get<PageData<UserEmailInfo>>(`/api/users/info${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

}
