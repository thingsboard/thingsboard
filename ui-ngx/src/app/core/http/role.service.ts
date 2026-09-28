// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { forkJoin, Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { map } from 'rxjs/operators';
import { sortEntitiesByIds } from '@shared/models/base-data';
import { Role } from '@shared/models/role.models';
import { RoleType } from '@shared/models/security.models';
import { GroupPermission, GroupPermissionInfo } from '@shared/models/group-permission.models';

@Injectable({
  providedIn: 'root'
})
export class RoleService {

  constructor(
    private http: HttpClient
  ) { }

  public getRoles(pageLink: PageLink, type?: RoleType, config?: RequestConfig): Observable<PageData<Role>> {
    let url = `/api/roles${pageLink.toQuery()}`;
    if (type) {
      url += `&type=${type}`;
    }
    return this.http.get<PageData<Role>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getRolesByIds(roleIds: Array<string>, config?: RequestConfig): Observable<Array<Role>> {
    return this.http.get<Array<Role>>(`/api/roles?roleIds=${roleIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((roles) => sortEntitiesByIds(roles, roleIds))
    );
  }

  public getRole(roleId: string, config?: RequestConfig): Observable<Role> {
    return this.http.get<Role>(`/api/role/${roleId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveRole(role: Role, config?: RequestConfig): Observable<Role> {
    return this.http.post<Role>('/api/role', role, defaultHttpOptionsFromConfig(config));
  }

  public deleteRole(roleId: string, config?: RequestConfig) {
    return this.http.delete(`/api/role/${roleId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveGroupPermission(groupPermission: GroupPermission, config?: RequestConfig): Observable<GroupPermission> {
    return this.http.post<GroupPermission>('/api/groupPermission', groupPermission, defaultHttpOptionsFromConfig(config));
  }

  public deleteGroupPermissions(groupPermissionIds: string[], config?: RequestConfig): Observable<any> {
    const observables = groupPermissionIds.map((groupPermissionId) => this.deleteGroupPermission(groupPermissionId, config));
    return forkJoin(observables);
  }

  public deleteGroupPermission(groupPermissionId: string, config?: RequestConfig): Observable<any> {
    return this.http.delete(`/api/groupPermission/${groupPermissionId}`, defaultHttpOptionsFromConfig(config));
  }

  public getUserGroupPermissions(userGroupId: string, config?: RequestConfig): Observable<Array<GroupPermissionInfo>> {
    return this.http.get<Array<GroupPermissionInfo>>(`/api/userGroup/${userGroupId}/groupPermissions`,
      defaultHttpOptionsFromConfig(config));
  }

  public getEntityGroupPermissions(entityGroupId: string, config?: RequestConfig): Observable<Array<GroupPermissionInfo>> {
    return this.http.get<Array<GroupPermissionInfo>>(`/api/entityGroup/${entityGroupId}/groupPermissions`,
      defaultHttpOptionsFromConfig(config));
  }

  public getGroupPermissionInfo(groupPermissionId: string, isUserGroup: boolean, config?: RequestConfig): Observable<GroupPermissionInfo> {
    return this.http.get<GroupPermissionInfo>(`/api/groupPermission/info/${groupPermissionId}?isUserGroup=${isUserGroup}`,
      defaultHttpOptionsFromConfig(config));
  }

  public loadUserGroupPermissionInfos(groupPermissions: Array<GroupPermission>,
                                      config?: RequestConfig): Observable<Array<GroupPermissionInfo>> {
    return this.http.post<Array<GroupPermissionInfo>>('/api/userGroup/groupPermissions/info', groupPermissions,
      defaultHttpOptionsFromConfig(config));
  }
}
