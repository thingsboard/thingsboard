// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Inject, Injectable, DOCUMENT } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { defaultHttpOptionsFromConfig, RequestConfig } from '@core/http/http-utils';
import { Observable } from 'rxjs';
import { BlobEntityInfo, BlobEntityWithCustomerInfo } from '@shared/models/blob-entity.models';
import { TimePageLink } from '@shared/models/page/page-link';
import { PageData } from '@shared/models/page/page-data';
import { map } from 'rxjs/operators';

import { WINDOW } from '@core/services/window.service';
import { sortEntitiesByIds } from '@shared/models/base-data';
import { getFilenameFromHttpHeader, isDefinedAndNotNull } from '@core/utils';

// @dynamic
@Injectable({
  providedIn: 'root'
})
export class BlobEntityService {

  constructor(
    @Inject(WINDOW) private window: Window,
    @Inject(DOCUMENT) private document: Document,
    private http: HttpClient,
  ) {
  }

  public getBlobEntityInfo(blobEntityId: string, config?: RequestConfig): Observable<BlobEntityWithCustomerInfo> {
    return this.http.get<BlobEntityWithCustomerInfo>(`/api/blobEntity/info/${blobEntityId}`, defaultHttpOptionsFromConfig(config));
  }

  public getBlobEntities(pageLink: TimePageLink, type: string = '',
                         config?: RequestConfig): Observable<PageData<BlobEntityWithCustomerInfo>> {
    let url = `/api/blobEntities${pageLink.toQuery()}`;
    if (isDefinedAndNotNull(type)) {
      url += `&type=${type}`;
    }
    return this.http.get<PageData<BlobEntityWithCustomerInfo>>(url,
      defaultHttpOptionsFromConfig(config));
  }

  public getBlobEntitiesByIds(blobEntityIds: Array<string>, config?: RequestConfig): Observable<Array<BlobEntityInfo>> {
    return this.http.get<Array<BlobEntityInfo>>(`/api/blobEntities?blobEntityIds=${blobEntityIds.join(',')}`,
      defaultHttpOptionsFromConfig(config)).pipe(
        map((blobEntities) => sortEntitiesByIds(blobEntities, blobEntityIds))
    );
  }

  public deleteBlobEntity(blobEntityId: string, config?: RequestConfig) {
    return this.http.delete(`/api/blobEntity/${blobEntityId}`, defaultHttpOptionsFromConfig(config));
  }

  public downloadBlobEntity(blobEntityId: string): Observable<any> {
    return this.http.get(`/api/blobEntity/${blobEntityId}/download`, { responseType: 'arraybuffer', observe: 'response' }).pipe(
      map((response) => {
        const headers = response.headers;
        const filename = getFilenameFromHttpHeader(headers);
        const contentType = headers.get('content-type');
        const linkElement = this.document.createElement('a');
        try {
          const blob = new Blob([response.body], { type: contentType });
          const url = URL.createObjectURL(blob);
          linkElement.setAttribute('href', url);
          linkElement.setAttribute('download', filename);
          linkElement.click();
          setTimeout(() => URL.revokeObjectURL(url), 0);
          return null;
        } catch (e) {
          throw e;
        }
      })
    );
  }

}
