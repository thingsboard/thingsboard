// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
import { Injectable } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpEvent, HttpEventType } from '@angular/common/http';
import { defaultHttpOptions, defaultHttpUploadOptions } from '@core/http/http-utils';
import { from, Observable, of, Subscription, throwError } from 'rxjs';
import { catchError, filter, finalize, map, shareReplay, switchMap } from 'rxjs/operators';
import { AiResourceConfig, AiResourceInfo } from '@shared/models/ai-chat.models';

export type AiResourceUploadEvent =
  { type: 'progress'; progress: number } |
  { type: 'done'; resource: AiResourceInfo };

interface CachedUrl {
  url$: Observable<string>;
  url?: string;
  size: number;
  dropped?: boolean;
}

const MAX_CONCURRENT_FETCHES = 3;
const MAX_CACHED_URLS = 40;
const MAX_CACHED_BYTES = 64 * 1024 * 1024;

@Injectable({
  providedIn: 'root'
})
export class AiResourceService {

  private config$: Observable<AiResourceConfig | null>;

  private readonly urls = new Map<string, CachedUrl>();

  private readonly waitingFetches: Array<() => void> = [];

  private activeFetches = 0;

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
    this.forget(resourceId);
    return this.http.delete<void>(`/api/ai/resources/${resourceId}`, defaultHttpOptions(true, true));
  }

  rememberUrl(resourceId: string, url: string, size: number): void {
    this.forget(resourceId);
    this.urls.set(resourceId, {url$: of(url), url, size});
    this.evict(resourceId);
  }

  getUrl(resourceId: string): Observable<string> {
    const cached = this.urls.get(resourceId);
    if (cached) {
      this.urls.delete(resourceId);
      this.urls.set(resourceId, cached);
      return cached.url$;
    }
    const entry: CachedUrl = {url$: null, size: 0};
    entry.url$ = this.fetch(resourceId).pipe(
      map(blob => {
        const url = URL.createObjectURL(blob);
        if (entry.dropped) {
          setTimeout(() => URL.revokeObjectURL(url));
        } else {
          entry.url = url;
          entry.size = blob.size;
          this.evict(resourceId);
        }
        return url;
      }),
      catchError(error => {
        if (this.urls.get(resourceId) === entry) {
          this.urls.delete(resourceId);
        }
        throw error;
      }),
      shareReplay(1)
    );
    this.urls.set(resourceId, entry);
    return entry.url$;
  }

  download(resource: AiResourceInfo): Observable<void> {
    const cachedUrl = this.urls.get(resource.id)?.url;
    if (cachedUrl) {
      this.save(cachedUrl, resource.name);
      return of(undefined);
    }
    return this.fetch(resource.id).pipe(
      map(blob => {
        const url = URL.createObjectURL(blob);
        this.save(url, resource.name);
        setTimeout(() => URL.revokeObjectURL(url));
      })
    );
  }

  private save(url: string, name: string): void {
    const link = document.createElement('a');
    link.href = url;
    link.download = name;
    document.body.appendChild(link);
    link.click();
    link.remove();
  }

  private fetch(resourceId: string): Observable<Blob> {
    const request$ = this.http.get(`/api/ai/resources/${resourceId}`, {...defaultHttpOptions(true, true), responseType: 'blob'}).pipe(
      catchError(error => this.readErrorBody(error))
    );
    return new Observable<Blob>(subscriber => {
      let subscription: Subscription;
      let finished = false;
      const finish = () => {
        if (!finished) {
          finished = true;
          this.activeFetches--;
          this.waitingFetches.shift()?.();
        }
      };
      const start = () => {
        this.activeFetches++;
        subscription = request$.pipe(finalize(finish)).subscribe(subscriber);
      };
      if (this.activeFetches < MAX_CONCURRENT_FETCHES) {
        start();
      } else {
        this.waitingFetches.push(start);
      }
      return () => {
        const index = this.waitingFetches.indexOf(start);
        if (index >= 0) {
          this.waitingFetches.splice(index, 1);
        }
        subscription?.unsubscribe();
      };
    });
  }

  private readErrorBody(error: any): Observable<never> {
    if (!(error instanceof HttpErrorResponse) || !(error.error instanceof Blob)) {
      return throwError(() => error);
    }
    return from(error.error.text()).pipe(
      catchError(() => of('')),
      switchMap(text => throwError(() => new HttpErrorResponse({
        error: text || null,
        headers: error.headers,
        status: error.status,
        statusText: error.statusText,
        url: error.url ?? undefined
      })))
    );
  }

  private forget(resourceId: string): void {
    const entry = this.urls.get(resourceId);
    if (entry) {
      this.urls.delete(resourceId);
      this.release(entry);
    }
  }

  private release(entry: CachedUrl): void {
    entry.dropped = true;
    if (entry.url) {
      URL.revokeObjectURL(entry.url);
    }
  }

  private evict(keep: string): void {
    let bytes = 0;
    this.urls.forEach(entry => bytes += entry.size);
    for (const [resourceId, entry] of this.urls) {
      if (this.urls.size <= MAX_CACHED_URLS && bytes <= MAX_CACHED_BYTES) {
        break;
      }
      if (resourceId !== keep && entry.url) {
        this.urls.delete(resourceId);
        bytes -= entry.size;
        this.release(entry);
      }
    }
  }

}
