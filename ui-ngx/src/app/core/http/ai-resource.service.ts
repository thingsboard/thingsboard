// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpEventType } from '@angular/common/http';
import { defaultHttpOptions, defaultHttpUploadOptions } from '@core/http/http-utils';
import { Observable, of } from 'rxjs';
import { catchError, filter, map, shareReplay, tap } from 'rxjs/operators';
import { AiResourceConfig, AiResourceInfo } from '@shared/models/ai-chat.models';

export type AiResourceUploadEvent =
  { type: 'progress'; progress: number } |
  { type: 'done'; resource: AiResourceInfo };

@Injectable({
  providedIn: 'root'
})
export class AiResourceService {

  private config$: Observable<AiResourceConfig | null>;

  private readonly urls = new Map<string, Observable<string>>();

  constructor(private http: HttpClient) {
  }

  getConfig(): Observable<AiResourceConfig | null> {
    if (!this.config$) {
      this.config$ = this.http.get<AiResourceConfig>('/api/ai/resources/config', defaultHttpOptions(true, true)).pipe(
        catchError(() => {
          this.config$ = null;
          return of(null);
        }),
        shareReplay(1)
      );
    }
    return this.config$;
  }

  upload(file: File): Observable<AiResourceUploadEvent> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    return this.http.post<AiResourceInfo>('/api/ai/resources', formData, {
      ...defaultHttpUploadOptions(true, true),
      observe: 'events',
      reportProgress: true
    }).pipe(
      filter((event: HttpEvent<AiResourceInfo>) => event.type === HttpEventType.UploadProgress || event.type === HttpEventType.Response),
      map((event: HttpEvent<AiResourceInfo>): AiResourceUploadEvent => {
        if (event.type === HttpEventType.UploadProgress) {
          return {type: 'progress', progress: event.total ? Math.round(100 * event.loaded / event.total) : 0};
        }
        return {type: 'done', resource: (event as any).body as AiResourceInfo};
      })
    );
  }

  delete(resourceId: string): Observable<void> {
    this.urls.delete(resourceId);
    return this.http.delete<void>(`/api/ai/resources/${resourceId}`, defaultHttpOptions(true, true));
  }

  rememberUrl(resourceId: string, url: string): void {
    this.urls.set(resourceId, of(url));
  }

  getUrl(resourceId: string): Observable<string> {
    let url$ = this.urls.get(resourceId);
    if (!url$) {
      url$ = this.http.get(`/api/ai/resources/${resourceId}`, {...defaultHttpOptions(true, true), responseType: 'blob'}).pipe(
        map(blob => URL.createObjectURL(blob)),
        catchError(error => {
          this.urls.delete(resourceId);
          throw error;
        }),
        shareReplay(1)
      );
      this.urls.set(resourceId, url$);
    }
    return url$;
  }

  download(resource: AiResourceInfo): Observable<void> {
    return this.getUrl(resource.id).pipe(
      tap(url => {
        const link = document.createElement('a');
        link.href = url;
        link.download = resource.name;
        document.body.appendChild(link);
        link.click();
        link.remove();
      }),
      map(() => undefined)
    );
  }

}
