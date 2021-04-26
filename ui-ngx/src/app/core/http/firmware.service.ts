///
/// Copyright © 2016-2021 The Thingsboard Authors
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
import { HttpClient } from '@angular/common/http';
import { PageLink } from '@shared/models/page/page-link';
import { defaultHttpOptionsFromConfig, defaultHttpUploadOptions, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import { Firmware, FirmwareInfo } from '@shared/models/firmware.models';
import { catchError, map, mergeMap } from 'rxjs/operators';
import { deepClone, isDefinedAndNotNull } from '@core/utils';

@Injectable({
  providedIn: 'root'
})
export class FirmwareService {
  constructor(
    private http: HttpClient
  ) {

  }

  public getFirmwares(pageLink: PageLink, hasData?: boolean, config?: RequestConfig): Observable<PageData<FirmwareInfo>> {
    let url = `/api/firmwares`;
    if (isDefinedAndNotNull(hasData)) {
      url += `/${hasData}`;
    }
    url += `${pageLink.toQuery()}`;
    return this.http.get<PageData<FirmwareInfo>>(url, defaultHttpOptionsFromConfig(config));
  }

  public getFirmware(firmwareId: string, config?: RequestConfig): Observable<Firmware> {
    return this.http.get<Firmware>(`/api/firmware/${firmwareId}`, defaultHttpOptionsFromConfig(config));
  }

  public getFirmwareInfo(firmwareId: string, config?: RequestConfig): Observable<FirmwareInfo> {
    return this.http.get<FirmwareInfo>(`/api/firmware/info/${firmwareId}`, defaultHttpOptionsFromConfig(config));
  }

  public downloadFirmware(firmwareId: string): Observable<any> {
    return this.http.get(`/api/firmware/${firmwareId}/download`, { responseType: 'arraybuffer', observe: 'response' }).pipe(
      map((response) => {
        const headers = response.headers;
        const filename = headers.get('x-filename');
        const contentType = headers.get('content-type');
        const linkElement = document.createElement('a');
        try {
          const blob = new Blob([response.body], { type: contentType });
          const url = URL.createObjectURL(blob);
          linkElement.setAttribute('href', url);
          linkElement.setAttribute('download', filename);
          const clickEvent = new MouseEvent('click',
            {
              view: window,
              bubbles: true,
              cancelable: false
            }
          );
          linkElement.dispatchEvent(clickEvent);
          return null;
        } catch (e) {
          throw e;
        }
      })
    );
  }

  public saveFirmware(firmware: Firmware, config?: RequestConfig): Observable<Firmware> {
    if (!firmware.file) {
      return this.saveFirmwareInfo(firmware, config);
    }
    const firmwareInfo = deepClone(firmware);
    delete firmwareInfo.file;
    delete firmwareInfo.checksum;
    delete firmwareInfo.checksumAlgorithm;
    return this.saveFirmwareInfo(firmwareInfo, config).pipe(
      mergeMap(res => {
        return this.uploadFirmwareFile(res.id.id, firmware.file, firmware.checksumAlgorithm, firmware.checksum).pipe(
          catchError(() => this.deleteFirmware(res.id.id))
        );
      })
    );
  }

  public saveFirmwareInfo(firmware: FirmwareInfo, config?: RequestConfig): Observable<Firmware> {
    return this.http.post<Firmware>('/api/firmware', firmware, defaultHttpOptionsFromConfig(config));
  }

  public uploadFirmwareFile(firmwareId: string, file: File, checksumAlgorithm?: string,
                            checksum?: string, config?: RequestConfig): Observable<any> {
    if (!config) {
      config = {};
    }
    const formData = new FormData();
    formData.append('file', file);
    let url = `/api/firmware/${firmwareId}`;
    if (checksumAlgorithm && checksum) {
      url += `?checksumAlgorithm=${checksumAlgorithm}&checksum=${checksum}`;
    }
    return this.http.post(url, formData,
      defaultHttpUploadOptions(config.ignoreLoading, config.ignoreErrors, config.resendRequest));
  }

  public deleteFirmware(firmwareId: string, config?: RequestConfig) {
    return this.http.delete(`/api/firmware/${firmwareId}`, defaultHttpOptionsFromConfig(config));
  }

}
