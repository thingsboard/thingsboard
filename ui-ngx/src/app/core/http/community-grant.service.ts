// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { defaultHttpOptionsFromConfig, defaultHttpUploadOptions, RequestConfig } from './http-utils';
import {
  CommunityGrantRegistration,
  communityGrantRegistrationFrom,
  CommunityGrantStateInfo
} from '@shared/models/ce-grant/community-grant.models';

@Injectable({
  providedIn: 'root'
})
export class CommunityGrantService {

  constructor(private http: HttpClient) {
  }

  /**
   * Mints a fresh claim token and sign-up URL. An unreachable portal is not an error: it answers with
   * `mode = OFFLINE`.
   */
  public register(config?: RequestConfig): Observable<CommunityGrantRegistration> {
    return this.http.post<CommunityGrantStateInfo>('/api/communityGrant/start', null, defaultHttpOptionsFromConfig(config))
      .pipe(map(info => communityGrantRegistrationFrom(info)));
  }

  public pollRegistration(config?: RequestConfig): Observable<CommunityGrantRegistration> {
    return this.http.get<CommunityGrantStateInfo>('/api/communityGrant/state', defaultHttpOptionsFromConfig(config))
      .pipe(map(info => communityGrantRegistrationFrom(info)));
  }

  /**
   * Answers once the run has started; its report or failure arrives on a later `GET /state`. Errors are not
   * toasted: the dialog shows them itself.
   */
  public runOfflineScaleCheck(bundle: File): Observable<CommunityGrantRegistration> {
    const formData = new FormData();
    formData.append('bundle', bundle);
    return this.http.post<CommunityGrantStateInfo>('/api/communityGrant/offline/run', formData,
      defaultHttpUploadOptions(false, true))
      .pipe(map(info => communityGrantRegistrationFrom(info)));
  }

  public downloadOfflineReport(config?: RequestConfig): Observable<string> {
    return this.http.get('/api/communityGrant/offline/report',
      {...{responseType: 'text' as const}, ...defaultHttpOptionsFromConfig(config)});
  }

  public confirmOfflineHandoff(config?: RequestConfig): Observable<CommunityGrantRegistration> {
    return this.http.post<CommunityGrantStateInfo>('/api/communityGrant/offline/handoff', null,
      defaultHttpOptionsFromConfig(config))
      .pipe(map(info => communityGrantRegistrationFrom(info)));
  }

  /** Success means only that the request reached the portal, never that the registered owner was notified. */
  public requestAccess(config?: RequestConfig): Observable<void> {
    return this.http.post<void>('/api/communityGrant/requestAccess', null, defaultHttpOptionsFromConfig(config));
  }
}
