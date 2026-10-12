// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { EdgeAssignments, EdgeProfile } from '@shared/models/edge-profile.models';
import { EntityType } from '@shared/models/entity-type.models';

@Injectable({
  providedIn: 'root'
})
export class EdgeProfileService {

  constructor(private http: HttpClient) {
  }

  public getEdgeProfiles(pageLink: PageLink, config?: RequestConfig): Observable<PageData<EdgeProfile>> {
    return this.http.get<PageData<EdgeProfile>>(`/api/edgeProfiles${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getEdgeProfile(edgeProfileId: string, config?: RequestConfig): Observable<EdgeProfile> {
    return this.http.get<EdgeProfile>(`/api/edgeProfile/${edgeProfileId}`, defaultHttpOptionsFromConfig(config));
  }

  public getDefaultEdgeProfile(config?: RequestConfig): Observable<EdgeProfile> {
    return this.http.get<EdgeProfile>('/api/edgeProfile/default', defaultHttpOptionsFromConfig(config));
  }

  public saveEdgeProfile(edgeProfile: EdgeProfile, config?: RequestConfig): Observable<EdgeProfile> {
    return this.http.post<EdgeProfile>('/api/edgeProfile', edgeProfile, defaultHttpOptionsFromConfig(config));
  }

  public deleteEdgeProfile(edgeProfileId: string, config?: RequestConfig) {
    return this.http.delete(`/api/edgeProfile/${edgeProfileId}`, defaultHttpOptionsFromConfig(config));
  }

  public setDefaultEdgeProfile(edgeProfileId: string, config?: RequestConfig): Observable<EdgeProfile> {
    return this.http.post<EdgeProfile>(`/api/edgeProfile/${edgeProfileId}/default`, null, defaultHttpOptionsFromConfig(config));
  }

  public getEdgeProfileAssignments(edgeProfileId: string, config?: RequestConfig): Observable<EdgeAssignments> {
    return this.http.get<EdgeAssignments>(`/api/edgeProfile/${edgeProfileId}/assignments`, defaultHttpOptionsFromConfig(config));
  }

  public addRuleChain(edgeProfileId: string, ruleChainId: string, config?: RequestConfig): Observable<EdgeProfile> {
    return this.http.post<EdgeProfile>(`/api/edgeProfile/${edgeProfileId}/ruleChain/${ruleChainId}`, null, defaultHttpOptionsFromConfig(config));
  }

  public removeRuleChain(edgeProfileId: string, ruleChainId: string, config?: RequestConfig): Observable<EdgeProfile> {
    return this.http.delete<EdgeProfile>(`/api/edgeProfile/${edgeProfileId}/ruleChain/${ruleChainId}`, defaultHttpOptionsFromConfig(config));
  }

  public setRootRuleChain(edgeProfileId: string, ruleChainId: string, config?: RequestConfig): Observable<EdgeProfile> {
    return this.http.post<EdgeProfile>(`/api/edgeProfile/${edgeProfileId}/${ruleChainId}/root`, null, defaultHttpOptionsFromConfig(config));
  }

  public addEntityGroup(edgeProfileId: string, entityGroupId: string, groupType: EntityType, config?: RequestConfig): Observable<EdgeProfile> {
    return this.http.post<EdgeProfile>(`/api/edgeProfile/${edgeProfileId}/entityGroup/${entityGroupId}/${groupType}`, null,
      defaultHttpOptionsFromConfig(config));
  }

  public removeEntityGroup(edgeProfileId: string, entityGroupId: string, groupType: EntityType, config?: RequestConfig): Observable<EdgeProfile> {
    return this.http.delete<EdgeProfile>(`/api/edgeProfile/${edgeProfileId}/entityGroup/${entityGroupId}/${groupType}`,
      defaultHttpOptionsFromConfig(config));
  }

  public addIntegration(edgeProfileId: string, integrationId: string, config?: RequestConfig): Observable<EdgeProfile> {
    return this.http.post<EdgeProfile>(`/api/edgeProfile/${edgeProfileId}/integration/${integrationId}`, null, defaultHttpOptionsFromConfig(config));
  }

  public removeIntegration(edgeProfileId: string, integrationId: string, config?: RequestConfig): Observable<EdgeProfile> {
    return this.http.delete<EdgeProfile>(`/api/edgeProfile/${edgeProfileId}/integration/${integrationId}`, defaultHttpOptionsFromConfig(config));
  }

  public addSchedulerEvent(edgeProfileId: string, schedulerEventId: string, config?: RequestConfig): Observable<EdgeProfile> {
    return this.http.post<EdgeProfile>(`/api/edgeProfile/${edgeProfileId}/schedulerEvent/${schedulerEventId}`, null, defaultHttpOptionsFromConfig(config));
  }

  public removeSchedulerEvent(edgeProfileId: string, schedulerEventId: string, config?: RequestConfig): Observable<EdgeProfile> {
    return this.http.delete<EdgeProfile>(`/api/edgeProfile/${edgeProfileId}/schedulerEvent/${schedulerEventId}`, defaultHttpOptionsFromConfig(config));
  }

  public applyEdgeProfile(edgeProfileId: string, config?: RequestConfig) {
    return this.http.post(`/api/edgeProfile/${edgeProfileId}/apply`, null, defaultHttpOptionsFromConfig(config));
  }
}
