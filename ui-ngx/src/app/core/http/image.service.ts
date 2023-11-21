///
/// Copyright © 2016-2023 The Thingsboard Authors
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
import { Observable, of } from 'rxjs';
import { PageData } from '@shared/models/page/page-data';
import {
  NO_IMAGE_DATA_URI,
  ImageResourceInfo,
  imageResourceType,
  ImageResourceType,
  IMAGES_URL_PREFIX, isImageResourceUrl
} from '@shared/models/resource.models';
import { catchError, map, switchMap } from 'rxjs/operators';
import { DomSanitizer, SafeUrl } from '@angular/platform-browser';
import { blobToBase64 } from '@core/utils';

@Injectable({
  providedIn: 'root'
})
export class ImageService {
  constructor(
    private http: HttpClient,
    private sanitizer: DomSanitizer
  ) {
  }

  public uploadImage(file: File, title: string, config?: RequestConfig): Observable<ImageResourceInfo> {
    if (!config) {
      config = {};
    }
    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title);
    return this.http.post<ImageResourceInfo>('/api/image', formData,
      defaultHttpUploadOptions(config.ignoreLoading, config.ignoreErrors, config.resendRequest));
  }

  public updateImage(type: ImageResourceType, key: string, file: File, config?: RequestConfig): Observable<ImageResourceInfo> {
    if (!config) {
      config = {};
    }
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ImageResourceInfo>(`${IMAGES_URL_PREFIX}/${type}/${encodeURIComponent(key)}`, formData,
      defaultHttpUploadOptions(config.ignoreLoading, config.ignoreErrors, config.resendRequest));
  }

  public updateImageInfo(imageInfo: ImageResourceInfo, config?: RequestConfig): Observable<ImageResourceInfo> {
    const type = imageResourceType(imageInfo);
    const key = encodeURIComponent(imageInfo.resourceKey);
    return this.http.put<ImageResourceInfo>(`${IMAGES_URL_PREFIX}/${type}/${key}/info`,
      imageInfo, defaultHttpOptionsFromConfig(config));
  }

  public getImages(pageLink: PageLink, config?: RequestConfig): Observable<PageData<ImageResourceInfo>> {
    return this.http.get<PageData<ImageResourceInfo>>(`${IMAGES_URL_PREFIX}${pageLink.toQuery()}`, defaultHttpOptionsFromConfig(config));
  }

  public getImageInfo(type: ImageResourceType, key: string, config?: RequestConfig): Observable<ImageResourceInfo> {
    return this.http.get<ImageResourceInfo>(`${IMAGES_URL_PREFIX}/${type}/${encodeURIComponent(key)}/info`,
      defaultHttpOptionsFromConfig(config));
  }

  public getImageDataUrl(imageUrl: string, preview = false, asString = false): Observable<SafeUrl | string> {
    const parts = imageUrl.split('/');
    const key = parts[parts.length - 1];
    parts[parts.length - 1] = encodeURIComponent(key);
    const encodedUrl = parts.join('/');
    const imageLink = preview ? (encodedUrl + '/preview') : encodedUrl;
    const options = defaultHttpOptionsFromConfig({ignoreLoading: true, ignoreErrors: true});
    return this.http
    .get(imageLink, {...options, ...{ responseType: 'blob' } }).pipe(
      switchMap(val => blobToBase64(val).pipe(
          map((dataUrl) => asString ? dataUrl : this.sanitizer.bypassSecurityTrustUrl(dataUrl))
        )),
      catchError(() => of(asString ? NO_IMAGE_DATA_URI : this.sanitizer.bypassSecurityTrustUrl(NO_IMAGE_DATA_URI)))
    );
  }

  public resolveImageUrl(imageUrl: string, preview = false, asString = false): Observable<SafeUrl | string> {
    if (isImageResourceUrl(imageUrl)) {
      return this.getImageDataUrl(imageUrl, preview, asString);
    } else {
      return of(asString ? imageUrl : this.sanitizer.bypassSecurityTrustUrl(imageUrl));
    }
  }

  public downloadImage(type: ImageResourceType, key: string): Observable<any> {
    return this.http.get(`${IMAGES_URL_PREFIX}/${type}/${encodeURIComponent(key)}`, {
      responseType: 'arraybuffer',
      observe: 'response'
    }).pipe(
      map((response) => {
        const headers = response.headers;
        const filename = headers.get('x-filename');
        const contentType = headers.get('content-type');
        const linkElement = document.createElement('a');
        try {
          const blob = new Blob([response.body], {type: contentType});
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

  public deleteImage(type: ImageResourceType, key: string, config?: RequestConfig) {
    return this.http.delete(`${IMAGES_URL_PREFIX}/${type}/${encodeURIComponent(key)}`, defaultHttpOptionsFromConfig(config));
  }

}
