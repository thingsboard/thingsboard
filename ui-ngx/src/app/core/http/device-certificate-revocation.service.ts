// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { PageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import {
  DeviceCertificateRevocation,
  DeviceCertificateRevocationRequest
} from '@shared/models/device-certificate-revocation.models';

@Injectable({
  providedIn: 'root'
})
export class DeviceCertificateRevocationService {

  constructor(
    private http: HttpClient
  ) { }

  public getRevocations(pageLink: PageLink, config?: RequestConfig): Observable<PageData<DeviceCertificateRevocation>> {
    return this.http.get<PageData<DeviceCertificateRevocation>>(`/api/deviceCertificateRevocations${pageLink.toQuery()}`,
      defaultHttpOptionsFromConfig(config));
  }

  public revokeCertificate(request: DeviceCertificateRevocationRequest, config?: RequestConfig): Observable<DeviceCertificateRevocation> {
    return this.http.post<DeviceCertificateRevocation>('/api/deviceCertificateRevocation', request, defaultHttpOptionsFromConfig(config));
  }

  public deleteRevocation(revocationId: string, config?: RequestConfig): Observable<void> {
    return this.http.delete<void>(`/api/deviceCertificateRevocation/${revocationId}`, defaultHttpOptionsFromConfig(config));
  }

}
