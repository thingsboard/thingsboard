// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { TenantProfile } from '@shared/models/tenant.model';
import { EntityInfoData } from '@shared/models/entity.models';
import { sortEntitiesByIds } from '@shared/models/base-data';
import { map } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class TenantProfileService {

  constructor(
    private http: HttpClient
  ) { }

  public getTenantProfiles(pageLink: PageLink, config?: RequestConfig): Observable<PageData<TenantProfile>> {
    return this.http.get<PageData<TenantProfile>>(`/api/tenantProfiles${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getTenantProfile(tenantProfileId: string, config?: RequestConfig): Observable<TenantProfile> {
    return this.http.get<TenantProfile>(`/api/tenantProfile/${tenantProfileId}`, defaultHttpOptionsFromConfig(config));
  }

  public saveTenantProfile(tenantProfile: TenantProfile, config?: RequestConfig): Observable<TenantProfile> {
    return this.http.post<TenantProfile>('/api/tenantProfile', tenantProfile, defaultHttpOptionsFromConfig(config));
  }

  public deleteTenantProfile(tenantProfileId: string, config?: RequestConfig) {
    return this.http.delete(`/api/tenantProfile/${tenantProfileId}`, defaultHttpOptionsFromConfig(config));
  }

  public setDefaultTenantProfile(tenantProfileId: string, config?: RequestConfig): Observable<TenantProfile> {
    return this.http.post<TenantProfile>(`/api/tenantProfile/${tenantProfileId}/default`, defaultHttpOptionsFromConfig(config));
  }

  public getDefaultTenantProfileInfo(config?: RequestConfig): Observable<EntityInfoData> {
    return this.http.get<EntityInfoData>('/api/tenantProfileInfo/default', defaultHttpOptionsFromConfig(config));
  }

  public getTenantProfileInfo(tenantProfileId: string, config?: RequestConfig): Observable<EntityInfoData> {
    return this.http.get<EntityInfoData>(`/api/tenantProfileInfo/${tenantProfileId}`, defaultHttpOptionsFromConfig(config));
  }

  public getTenantProfileInfos(pageLink: PageLink, config?: RequestConfig): Observable<PageData<EntityInfoData>> {
    return this.http.get<PageData<EntityInfoData>>(`/api/tenantProfileInfos${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getTenantProfilesByIds(tenantProfileIds: Array<string>, config?: RequestConfig): Observable<Array<EntityInfoData>> {
    return this.http.get<Array<EntityInfoData>>(`/api/tenantProfiles?ids=${tenantProfileIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
      map((tenantProfiles) => sortEntitiesByIds(tenantProfiles, tenantProfileIds))
    );
  }

}
