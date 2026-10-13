// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { Injectable } from '@angular/core';
import { defaultHttpOptionsFromConfig, RequestConfig } from './http-utils';
import { finalize, Observable, shareReplay } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import {
  AdminSettings,
  AutoCommitSettings,
  FeaturesInfo,
  JwtSettings,
  LicenseUsageInfo,
  MailConfigTemplate,
  MailServerSettings,
  RepositorySettings,
  RepositorySettingsInfo,
  SecuritySettings,
  TestSmsRequest,
  UpdateMessage
} from '@shared/models/settings.models';
import { EntitiesVersionControlService } from '@core/http/entities-version-control.service';
import { tap } from 'rxjs/operators';
import { LoginResponse } from '@shared/models/login.models';
import { SubscriptionInfo } from '@shared/models/subscription.models';

@Injectable({
  providedIn: 'root'
})
export class AdminService {

  constructor(
    private http: HttpClient,
    private entitiesVersionControlService: EntitiesVersionControlService
  ) { }

  public getAdminSettings<T>(key: string, systemByDefault?: boolean, config?: RequestConfig): Observable<AdminSettings<T>> {
    return this.http.get<AdminSettings<T>>(`/api/admin/settings/${key}?systemByDefault=${systemByDefault ? 'true': 'false'}`,
      defaultHttpOptionsFromConfig(config));
  }

  public saveAdminSettings<T>(adminSettings: AdminSettings<T>,
                              config?: RequestConfig): Observable<AdminSettings<T>> {
    return this.http.post<AdminSettings<T>>('/api/admin/settings', adminSettings, defaultHttpOptionsFromConfig(config));
  }

  public sendTestMail(adminSettings: AdminSettings<MailServerSettings>,
                      config?: RequestConfig): Observable<void> {
    return this.http.post<void>('/api/admin/settings/testMail', adminSettings, defaultHttpOptionsFromConfig(config));
  }

  public sendTestSms(testSmsRequest: TestSmsRequest,
                     config?: RequestConfig): Observable<void> {
    return this.http.post<void>('/api/admin/settings/testSms', testSmsRequest, defaultHttpOptionsFromConfig(config));
  }

  public getSecuritySettings(config?: RequestConfig): Observable<SecuritySettings> {
    return this.http.get<SecuritySettings>(`/api/admin/securitySettings`, defaultHttpOptionsFromConfig(config));
  }

  public saveSecuritySettings(securitySettings: SecuritySettings,
                              config?: RequestConfig): Observable<SecuritySettings> {
    return this.http.post<SecuritySettings>('/api/admin/securitySettings', securitySettings,
      defaultHttpOptionsFromConfig(config));
  }

  public getJwtSettings(config?: RequestConfig): Observable<JwtSettings> {
    return this.http.get<JwtSettings>(`/api/admin/jwtSettings`, defaultHttpOptionsFromConfig(config));
  }

  public saveJwtSettings(jwtSettings: JwtSettings, config?: RequestConfig): Observable<LoginResponse> {
    return this.http.post<LoginResponse>('/api/admin/jwtSettings', jwtSettings, defaultHttpOptionsFromConfig(config));
  }

  public getRepositorySettings(config?: RequestConfig): Observable<RepositorySettings> {
    return this.http.get<RepositorySettings>(`/api/admin/repositorySettings`, defaultHttpOptionsFromConfig(config));
  }

  public saveRepositorySettings(repositorySettings: RepositorySettings,
                                config?: RequestConfig): Observable<RepositorySettings> {
    return this.http.post<RepositorySettings>('/api/admin/repositorySettings', repositorySettings,
      defaultHttpOptionsFromConfig(config)).pipe(
      tap(() => {
        this.entitiesVersionControlService.clearBranchList();
      })
    );
  }

  public deleteRepositorySettings(config?: RequestConfig) {
    return this.http.delete('/api/admin/repositorySettings', defaultHttpOptionsFromConfig(config)).pipe(
      tap(() => {
        this.entitiesVersionControlService.clearBranchList();
      })
    );
  }

  public checkRepositoryAccess(repositorySettings: RepositorySettings,
                               config?: RequestConfig): Observable<void> {
    return this.http.post<void>('/api/admin/repositorySettings/checkAccess', repositorySettings, defaultHttpOptionsFromConfig(config));
  }

  public getRepositorySettingsInfo(config?: RequestConfig): Observable<RepositorySettingsInfo> {
    return this.http.get<RepositorySettingsInfo>('/api/admin/repositorySettings/info', defaultHttpOptionsFromConfig(config));
  }

  public getAutoCommitSettings(config?: RequestConfig): Observable<AutoCommitSettings> {
    return this.http.get<AutoCommitSettings>(`/api/admin/autoCommitSettings`, defaultHttpOptionsFromConfig(config));
  }

  public autoCommitSettingsExists(config?: RequestConfig): Observable<boolean> {
    return this.http.get<boolean>('/api/admin/autoCommitSettings/exists', defaultHttpOptionsFromConfig(config));
  }

  public saveAutoCommitSettings(autoCommitSettings: AutoCommitSettings,
                                config?: RequestConfig): Observable<AutoCommitSettings> {
    return this.http.post<AutoCommitSettings>('/api/admin/autoCommitSettings', autoCommitSettings, defaultHttpOptionsFromConfig(config));
  }

  public deleteAutoCommitSettings(config?: RequestConfig) {
    return this.http.delete('/api/admin/autoCommitSettings', defaultHttpOptionsFromConfig(config));
  }

  public checkUpdates(config?: RequestConfig): Observable<UpdateMessage> {
    return this.http.get<UpdateMessage>(`/api/admin/updates`, defaultHttpOptionsFromConfig(config));
  }

  public getFeaturesInfo(config?: RequestConfig): Observable<FeaturesInfo> {
    return this.http.get<FeaturesInfo>('/api/admin/featuresInfo', defaultHttpOptionsFromConfig(config));
  }

  public getLicenseUsageInfo(config?: RequestConfig): Observable<LicenseUsageInfo> {
    return this.http.get<LicenseUsageInfo>('/api/admin/licenseUsageInfo', defaultHttpOptionsFromConfig(config));
  }

  public getSubscriptionInfo(config?: RequestConfig): Observable<SubscriptionInfo> {
    return this.http.get<SubscriptionInfo>('/api/admin/subscriptionInfo', defaultHttpOptionsFromConfig(config));
  }

  /**
   * Coalesces concurrent callers onto a single in-flight request. Two overlapping refreshes (router
   * resolver + a manual "refresh" click ~60 ms apart is enough) would race the license client's monotonic
   * request-sequence number and one of them would come back 403 "License required: the instance is not
   * activated" over a licence that is fine, locking the management plane on the backend. Sharing the same
   * observable and clearing the reference on completion keeps subsequent callers free to trigger a fresh
   * refresh.
   */
  public refreshLicense(config?: RequestConfig): Observable<SubscriptionInfo> {
    if (!this.refreshLicenseInFlight$) {
      this.refreshLicenseInFlight$ = this.http.post<SubscriptionInfo>('/api/admin/refreshLicense', null,
        defaultHttpOptionsFromConfig(config)).pipe(
        finalize(() => this.refreshLicenseInFlight$ = null),
        shareReplay({ bufferSize: 1, refCount: false })
      );
    }
    return this.refreshLicenseInFlight$;
  }

  private refreshLicenseInFlight$: Observable<SubscriptionInfo> | null = null;

  public getLoginProcessingUrl(config?: RequestConfig): Observable<string> {
    return this.http.get<string>(`/api/admin/mail/oauth2/loginProcessingUrl`, defaultHttpOptionsFromConfig(config));
  }

  public generateAccessToken(config?: RequestConfig): Observable<string> {
    return this.http.get<string>(`/api/admin/mail/oauth2/authorize`, defaultHttpOptionsFromConfig(config));
  }

  public getMailConfigTemplate(config?: RequestConfig): Observable<Array<MailConfigTemplate>> {
    return this.http.get<Array<MailConfigTemplate>>('/api/mail/config/template', defaultHttpOptionsFromConfig(config));
  }
}
