// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Injectable } from '@angular/core';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { ActivationLinkInfo, User, UserEmailInfo, UserInfo } from '@shared/models/user.model';
import { Observable } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { isDefined } from '@core/utils';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';

@Injectable({
  providedIn: 'root'
})
export class UserService {

  constructor(
    private http: HttpClient
  ) { }

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

  public getAllCustomerUsers(pageLink: PageLink,
                             config?: RequestConfig): Observable<PageData<User>> {
    return this.http.get<PageData<User>>(`/api/customer/users${pageLink.toQuery()}`,
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

  public getUserInfo(userId: string, config?: RequestConfig): Observable<UserInfo> {
    return this.http.get<UserInfo>(`/api/user/info/${userId}`, defaultHttpOptionsFromConfig(config));
  }

  public getUsers(userIds: Array<string>, config?: RequestConfig): Observable<Array<User>> {
    return this.http.get<Array<User>>(`/api/users?userIds=${userIds.join(',')}`, defaultHttpOptionsFromConfig(config)).pipe(
      map((users) => sortEntitiesByIds(users, userIds))
    );
  }

  public getUserUsers(pageLink: PageLink,
                      config?: RequestConfig): Observable<PageData<User>> {
    return this.http.get<PageData<User>>(`/api/user/users${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public getAllUserInfos(includeCustomers: boolean,
                         pageLink: PageLink, config?: RequestConfig): Observable<PageData<UserInfo>> {
    let url = `/api/userInfos/all${pageLink.toQuery()}`;
    if (includeCustomers) {
      url += `&includeCustomers=true`;
    }
    return this.http.get<PageData<UserInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getCustomerUserInfos(includeCustomers: boolean, customerId: string,
                              pageLink: PageLink, config?: RequestConfig): Observable<PageData<UserInfo>> {
    let url = `/api/customer/${customerId}/userInfos${pageLink.toQuery()}`;
    if (includeCustomers) {
      url += `&includeCustomers=true`;
    }
    return this.http.get<PageData<UserInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public saveUser(user: User, sendActivationMail: boolean = false,
                  entityGroupIds?: string | string[],
                  config?: RequestConfig): Observable<User> {
    let url = `/api/user?sendActivationMail=${sendActivationMail}`;
    if (entityGroupIds) {
      if (Array.isArray(entityGroupIds)) {
        url += `&entityGroupIds=${entityGroupIds.join(',')}`;
      } else {
        url += `&entityGroupId=${entityGroupIds}`;
      }
    }
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
