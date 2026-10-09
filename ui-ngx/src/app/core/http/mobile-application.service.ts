// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { QrCodeSettings } from '@shared/models/mobile-app.models';

@Injectable({
  providedIn: 'root'
})
export class MobileApplicationService {

  constructor(
    private http: HttpClient
  ) {}

  public getMobileAppSettings(config?: RequestConfig): Observable<QrCodeSettings> {
    return this.http.get<QrCodeSettings>(`/api/mobile/qr/settings`, defaultHttpOptionsFromConfig(config));
  }

  public getMergedMobileAppSettings(config?: RequestConfig): Observable<QrCodeSettings> {
    return this.http.get<QrCodeSettings>(`/api/mobile/qr/merged`, defaultHttpOptionsFromConfig(config));
  }

  public saveMobileAppSettings(mobileAppSettings: QrCodeSettings, config?: RequestConfig): Observable<QrCodeSettings> {
    return this.http.post<QrCodeSettings>(`/api/mobile/qr/settings`, mobileAppSettings, defaultHttpOptionsFromConfig(config));
  }

  public getMobileAppDeepLink(config?: RequestConfig): Observable<string> {
    return this.http.get<string>(`/api/mobile/qr/deepLink`, defaultHttpOptionsFromConfig(config));
  }

}
